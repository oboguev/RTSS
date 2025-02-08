package rtss.forward_1926_193x;

import rtss.data.population.calc.RebalanceUrbanRural;
import rtss.data.population.calc.RescalePopulation;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

/*
 * При обработке переписи 1939 года были внесены приписки.
 * 
 * Действительная численность населения СССР составляла 167.6-167.7 млн. чел
 * (а преднамеренная фальсификация около 2,8-2,9 млн., или 1,7%),
 * при этом городское население составляло 47.5 млн. (28.3%), а не 55.9.
 
 * Население РСФСР составляло 106.9 млн. чел., 
 * а городское население 34.1 (31.9%), а не 36.8. 
 * 
 *   В.Б. Жиромская, "Численность населения России в 1939 г. : Поиск истины" // "Население России в 1920-1950-е годы: 
 *   Численность, потери, миграции", ИРИ РАН, М. 1994, стр. 32, 35, 38-40.
 * 
 *   Андреев,Дарский,Харькова, "Население Советского Союза 1922-1991", М. Наука, 1993, стр. 30-33 
 * 
 * Мы не располагаем сведениями о возрастной структуре приписок,
 * (Андреев,Дарский,Харькова, "Население Советского Союза 1922-1991", М. Наука, 1993, стр. 34)
 * поэтому корректируем равномерно все возрастные группы.
 */
public class Adjust_1939
{
    public PopulationByLocality adjust(Area area, final PopulationByLocality p) throws Exception
    {
        if (area == Area.USSR)
        {
            final double CorrectTotalPopulation = 167_650_000;
            final double CorrectUrbanlPopulation = 47_500_000;
            return correct(p, CorrectTotalPopulation, CorrectUrbanlPopulation);
        }
        else if (area == Area.RSFSR)
        {
            final double CorrectTotalPopulation = 106_900_000;
            final double CorrectUrbanlPopulation = 34_100_000;
            return correct(p, CorrectTotalPopulation, CorrectUrbanlPopulation);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }
    
    static private PopulationByLocality correct(PopulationByLocality p, double CorrectTotalPopulation, double CorrectUrbanlPopulation) throws Exception
    {
        PopulationByLocality p0 = p;
        
        double t = p.sum(Locality.TOTAL, Gender.BOTH, 0, Population.MAX_AGE);
        if (t > CorrectTotalPopulation)
        {
            p = RescalePopulation.scaleAllTo(p, CorrectTotalPopulation);
            t = CorrectTotalPopulation;
        }
        
        double u = p.sum(Locality.URBAN, Gender.BOTH, 0, Population.MAX_AGE);
        if (u/t > CorrectUrbanlPopulation / CorrectTotalPopulation)
        {
            p = RebalanceUrbanRural.rebalanceUrbanRural(p, CorrectUrbanlPopulation / CorrectTotalPopulation);
        }
        
        if (p == p0)
            p = p.clone();
        
        return p;
    }
}
