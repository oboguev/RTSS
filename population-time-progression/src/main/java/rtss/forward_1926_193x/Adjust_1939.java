package rtss.forward_1926_193x;

import rtss.data.population.calc.RebalanceUrbanRural;
import rtss.data.population.calc.RescalePopulation;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/*
 * При обработке переписи 1939 года были внесены приписки.
 * 
 * По Жиромской, действительная численность населения СССР составляла 167.6-167.7 млн. чел
 * (а преднамеренная фальсификация около 2,8-2,9 млн., или 1,7%),
 * при этом городское население составляло 47.5 млн. (28.3%), а не 55.9.
 
 * Население РСФСР составляло 106.9 млн. чел., 
 * а городское население 34.1 (31.9%), а не 36.8. 
 *
 * АДХ полагают возможным принять часть данных с контрольных бланков и произвольной добавки
 * внесённой ЦСУ СССР на неточность счёта, в этом случае население после всех коррекций
 * оказыввается 168.449 или 168.871 млн. чел. (АДХ-СССР, стр. 33).
 * 
 *   В.Б. Жиромская, "Численность населения России в 1939 г. : Поиск истины" // "Население России в 1920-1950-е годы: 
 *   Численность, потери, миграции", ИРИ РАН, М. 1994, стр. 32, 35, 38-40.
 * 
 *   [АДХ-СССР] Андреев,Дарский,Харькова, "Население Советского Союза 1922-1991", М. Наука, 1993, стр. 30-33
 * 
 * Мы не располагаем сведениями о возрастной структуре приписок (АДХ-СССР стр. 33-34),
 * поэтому корректируем равномерно все возрастные группы.
 * 
 * Численность возрастов 0-3 мы исправляем соответственно взаимоувязке переписей 1937 и 1939 гг. (АДХ-СССР, стр. 35),
 * перераспределяя избыток или нехватку в возрасты 4 и 5. 
 * 
 */
public class Adjust_1939
{
    public enum Correction1939
    {
        ZHIROMSKAYA, ADH_168_449, ADH_168_871
    }

    private final Correction1939 correction;

    public Adjust_1939()
    {
        this(Correction1939.ADH_168_449);
    }

    public Adjust_1939(Correction1939 correction)
    {
        this.correction = correction;
    }

    public PopulationByLocality adjust(Area area, final PopulationByLocality p) throws Exception
    {
        final double ussr_urban_total = 47_500_000.0 / 167_650_000;

        if (area == Area.USSR && correction == Correction1939.ZHIROMSKAYA)
        {
            final double correctTotalPopulation = 167_650_000;
            final double correctUrbanlPopulation = 47_500_000;
            return correct(p, correctTotalPopulation, correctUrbanlPopulation);
        }
        else if (area == Area.USSR && correction == Correction1939.ADH_168_449)
        {
            final double correctTotalPopulation = 168_448_972;
            final double correctUrbanlPopulation = correctTotalPopulation * ussr_urban_total;
            return correct(p, correctTotalPopulation, correctUrbanlPopulation);
        }
        else if (area == Area.USSR && correction == Correction1939.ADH_168_871)
        {
            final double correctTotalPopulation = 168_870_874;
            final double correctUrbanlPopulation = correctTotalPopulation * ussr_urban_total;
            return correct(p, correctTotalPopulation, correctUrbanlPopulation);
        }
        else if (area == Area.RSFSR)
        {
            final double correctTotalPopulation = 106_900_000;
            final double correctUrbanlPopulation = 34_100_000;
            return correct(p, correctTotalPopulation, correctUrbanlPopulation);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    private PopulationByLocality correct(PopulationByLocality p, double correctTotalPopulation, double correctUrbanlPopulation)
            throws Exception
    {
        PopulationByLocality p0 = p;

        double t = p.sum(Locality.TOTAL, Gender.BOTH, 0, Population.MAX_AGE);
        if (t > correctTotalPopulation)
        {
            p = RescalePopulation.scaleAllTo(p, correctTotalPopulation);
            t = correctTotalPopulation;
        }

        /* Население в возрастах 0-3 лет (АДХ-СССР, стр. 35) */
        setYoungAges(p, 5792, 4856, 4063, 3543);

        double u = p.sum(Locality.URBAN, Gender.BOTH, 0, Population.MAX_AGE);
        if (u / t > correctUrbanlPopulation / correctTotalPopulation)
        {
            p = RebalanceUrbanRural.rebalanceUrbanRural(p, correctUrbanlPopulation / correctTotalPopulation);
        }

        if (p == p0)
            p = p.clone();

        return p;
    }

    private void setYoungAges(PopulationByLocality p, double... yp) throws Exception
    {
        yp = Util.multiply(yp, 1000.0);
        double psum_initial = p.sum();

        // распределить diff в возрасты 4 и 5
        double diff = p.sum(0, 3) - Util.sum(yp);
        
        p = RescalePopulation.scaleYearAgeAllTo(p, 0, yp[0]);
        p = RescalePopulation.scaleYearAgeAllTo(p, 1, yp[1]);
        p = RescalePopulation.scaleYearAgeAllTo(p, 2, yp[2]);
        p = RescalePopulation.scaleYearAgeAllTo(p, 3, yp[3]);
        
        p = RescalePopulation.scaleYearAgeAllTo(p, 4, p.get(Locality.TOTAL, Gender.BOTH, 4) + diff / 2);
        p = RescalePopulation.scaleYearAgeAllTo(p, 5, p.get(Locality.TOTAL, Gender.BOTH, 5) + diff / 2);

        Util.assertion(Util.same(psum_initial, p.sum()));
    }
}