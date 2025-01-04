package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.InterpolateMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.params.AreaParameters;

/**
 * Вычислить таблицу смертности населения СССР или РСФСР для 1940 года.
 * 
 * К сожалению, для случая СССР, АДХ не опубликовали вычисленные и использованные ими возрастные показатели смертности, 
 * в т.ч. для 1940 года. Насколько можно судить, для взрослых возрастов они полагали их близкими по величине к госкомстатовскому 
 * расчёту для 1938-1939 гг.:
 * 
 * "Расчет возрастных чисел умерших проводился в несколько этапов путем передвижки населения 1939 г. с использованием
 * модифицированных таблиц смертности 1938-1939 гг., так, чтобы при данных числах живущих в каждом возрасте они давали
 * заданное общее число умерших... При модификации таблиц смертности 1938-1939 гг. мы воспользовались независимыми оценками
 * уровня младенческой смертности, рассчитанными нами по данным о младенческой смертности на территориях с хорошей
 * регистрацией." (АДХ-СССР, стр. 54-55). Для 1940 г. этот уровень полагается равным 184.0 промилле (АДХ-СССР стр. 135).
 * 
 * Для случая СССР:
 * 
 * Использование структуры населения CCCР для начала 1940 года по расчётам АДХ, а также уровней рождаемости и смертности СССР
 * вычисленных АДХ для 1940 года даёт итоговую таблицу смертности в возрастах 5 и более лет равную значениям таблицы ГКС-СССР-1938,
 * а в возрастной группе 0-4 года линейно составленную на 61.3% из таблицы (АДХ-РСФСР-1940) и на 38.7% из таблицы (ГКС-СССР-1938,
 * но с замещением смертности в возрасте 0 на значение вычисленное АДХ). Эта комбинация даёт смертность населения равную
 * расчитанной АДХ для 1940 года, при расчитанных же АДХ структуре населения и рождаемости в 1940 году. 
 * 
 * Для случая РСФСР:

 * Использование таблицы ГКС-СССР-1938 с поправкой младенческой смертности даёт для населения РСФСР-1940 по АДХ (при 
 * рождаемости по АДХ же в 34.6) смертность 18.3 промилле, а применение таблицы АДХ-РСФСР-1940 -- смертность 25.6 промилле.
 * Между тем, исчисленная АДХ величина смертности составляет 23.2. Таким образом, таблица ГКС-СССР-1938 оказывается
 * излищне оптимистичной, а таблица АДХ-РСФСР-1940 наоборот черезчур пессимистичной.
 * 
 * Чтобы добиться соответствия уровню смертности 23.2, мы используем таблицу линейно составленную для всех возрастов 
 * на 67.3% из таблицы (АДХ-РСФСР-1940) и на 32.7% из таблицы (ГКС-СССР-1938, но с замещением смертности в возрасте 0 на 
 * значение вычисленное АДХ). Эта комбинация (при структуре населения РСФСР 1940 по АДХ) даёт значение смертности всего 
 * населения равное 23.2.
 */
public class MortalityTable_1940 extends UtilBase_194x
{
    private AreaParameters ap;
    private CombinedMortalityTable mt1;
    private CombinedMortalityTable mt2;
    
    /* уровень младенческой смертности в СССР в 1940 году по АДХ (АДХ-СССР, стр. 135) */
    private static final double ADH_USSR_infant_CDR_1940 = 184.0;
    private static final boolean use_ADH_USSR_InfantMortalityRate = true;
    
    public MortalityTable_1940(AreaParameters ap) throws Exception
    {
        this.ap = ap;
        
        mt1 = CombinedMortalityTable.load("mortality_tables/USSR/1938-1939");
        mt1.comment("ГКС-СССР-1938");
        if (use_ADH_USSR_InfantMortalityRate)
        {
            mt1 = PatchMortalityTable.patchInfantMortalityRate(mt1, ADH_USSR_infant_CDR_1940, "infant mortality patched to ADH");
        }

        mt2 = CombinedMortalityTable.loadTotal("mortality_tables/RSFSR/1940");
        mt2.comment("АДХ-РСФСР-1940");
    }

    public CombinedMortalityTable evaluate() throws Exception
    {

        switch (ap.area)
        {
        case USSR:
            // return InterpolateMortalityTable.forTargetRates(mt1, mt2, new Population_In_Early_1940(ap).evaluate(), ap.CBR_1940, ap.CDR_1940, 4, ADH_USSR_infant_CDR_1940);
            return InterpolateMortalityTable.forTargetRates(mt1, mt2, new Population_In_Early_1940(ap).evaluate(), ap.CBR_1940, ap.CDR_1940, 4, null);

        case RSFSR:
            return InterpolateMortalityTable.forTargetRates(mt1, mt2, new Population_In_Early_1940(ap).evaluate(), ap.CBR_1940, ap.CDR_1940);
            
        default:
            throw new IllegalArgumentException();
        }
    }

    public void show_survival_rates_1941_1946() throws Exception
    {
        show_survival_rates_1941_1946(mt1);
        show_survival_rates_1941_1946(mt2);
        show_survival_rates_1941_1946(evaluate());
    }
    
    /*
     * Вероятности дожития при равномерном распределении по возрастам в указанной группе
     */
    private void show_survival_rates_1941_1946(CombinedMortalityTable mt) throws Exception
    {
        Util.out("");
        Util.out(String.format("Survival rate 1941 -> 1946 using life table %s:", mt.comment()));
        Util.out("");
        Util.out("  age      M      F  ");
        Util.out("=======  =====  =====");
        
        show_survival_rates_1941_1946_newborn(mt);
        show_survival_rates_1941_1946(mt, 0, 4);
        show_survival_rates_1941_1946(mt, 5, 14);
        show_survival_rates_1941_1946(mt, 15, 24);
        show_survival_rates_1941_1946(mt, 25, 34);
        show_survival_rates_1941_1946(mt, 35, 44);
        show_survival_rates_1941_1946(mt, 45, 54);
        show_survival_rates_1941_1946(mt, 55, 64);
        show_survival_rates_1941_1946(mt, 65, 74);
        show_survival_rates_1941_1946(mt, 75, Population.MAX_AGE);
    }

    private void show_survival_rates_1941_1946_newborn(CombinedMortalityTable mt) throws Exception
    {
        double m_rate = survival_rates_1941_1946_newborn(mt, Gender.MALE);
        double f_rate = survival_rates_1941_1946_newborn(mt, Gender.FEMALE);
        Util.out(String.format("%7s  %.3f  %.3f", "newborn", m_rate, f_rate));
    }

    private void show_survival_rates_1941_1946(CombinedMortalityTable mt, int age1, int age2) throws Exception
    {
        double m_rate = survival_rates_1941_1946(mt, Gender.MALE, age1, age2);
        double f_rate = survival_rates_1941_1946(mt, Gender.FEMALE, age1, age2);
        String ages = String.format("%s-%s", age1, age2);
        Util.out(String.format("%7s  %.3f  %.3f", ages, m_rate, f_rate));
    }

    private double survival_rates_1941_1946(CombinedMortalityTable mt, Gender gender, int age1, int age2) throws Exception
    {
        // build population for the start of 1941
        PopulationByLocality p = PopulationByLocality.newPopulationTotalOnly();

        for (int age = 0; age <= PopulationByLocality.MAX_AGE; age++)
        {
            p.set(Locality.TOTAL, Gender.MALE, age, 0);
            p.set(Locality.TOTAL, Gender.FEMALE, age, 0);
        }
        
        for (int age = age1; age <= age2; age++)
            p.set(Locality.TOTAL, gender, age, 1);
        
        double sum0 = p.sum(Locality.TOTAL, gender, 0, PopulationByLocality.MAX_AGE);
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        p = fctx.begin(p);

        // to early 1942
        ForwardPopulationT fw = new ForwardPopulationT();
        fw.setBirthRateTotal(0);
        p = fw.forward(p, fctx, mt, 1);

        // to early 1943
        p = fw.forward(p, fctx, mt, 1);

        // to early 1944
        p = fw.forward(p, fctx, mt, 1);

        // to early 1945
        p = fw.forward(p, fctx, mt, 1);

        // to early 1946
        p = fw.forward(p, fctx, mt, 1);

        p = fctx.end(p);

        double sum = p.sum(Locality.TOTAL, gender, 0, PopulationByLocality.MAX_AGE);

        return sum / sum0;
    }

    private double survival_rates_1941_1946_newborn(CombinedMortalityTable mt, Gender gender) throws Exception
    {
        // build population for the start of 1941
        PopulationByLocality p = PopulationByLocality.newPopulationTotalOnly();

        for (int age = 0; age <= PopulationByLocality.MAX_AGE; age++)
        {
            p.set(Locality.TOTAL, Gender.MALE, age, 0);
            p.set(Locality.TOTAL, Gender.FEMALE, age, 0);
            p.set(Locality.TOTAL, Gender.BOTH, age, 0);
        }
        
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        double sum0 = 1.0;
        fctx.set(Locality.TOTAL, gender, 0, sum0);

        // forward to early 1942
        ForwardPopulationT fw = new ForwardPopulationT();
        fw.setBirthRateTotal(0);
        p = fw.forward(p, fctx, mt, 1);

        // to early 1943
        p = fw.forward(p, fctx, mt, 1);

        // to early 1944
        p = fw.forward(p, fctx, mt, 1);

        // to early 1945
        p = fw.forward(p, fctx, mt, 1);

        // to early 1946
        p = fw.forward(p, fctx, mt, 1);

        p = fctx.end(p);

        double sum = p.sum(Locality.TOTAL, gender, 0, PopulationByLocality.MAX_AGE);

        return sum / sum0;
   }
}