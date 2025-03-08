package rtss.ww2losses.population1941;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.calc.RescalePopulation;
import rtss.data.population.projection.ForwardPopulationT;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.ww2losses.helpers.PeacetimeMortalityTables;
import rtss.ww2losses.helpers.WarHelpers;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population194x.UtilBase_194x;
import rtss.ww2losses.struct.HalfYearEntries.HalfYearSelector;

public class PopulationMiddle1941 extends UtilBase_194x
{
    public static class PopulationForwardingResult1941
    {
        public PopulationContext p_mid1941;
        public PopulationContext observed_deaths_byGenderAge;
        public double observed_births;
        public double[] births_byday;
    }
    
    private final AreaParameters ap;
    
    public PopulationMiddle1941(AreaParameters ap)
    {
        this.ap = ap;
    }

    public PopulationForwardingResult1941 forward_1941_1st_halfyear(
            final PopulationContext p_start1941,
            final PeacetimeMortalityTables peacetimeMortalityTables,
            final double asfr_calibration,
            final AgeSpecificFertilityRates asfrs) throws Exception
    {
        PopulationForwardingResult1941 fr = new PopulationForwardingResult1941();

        final CombinedMortalityTable mt1941_1 = peacetimeMortalityTables.getTable(1941, HalfYearSelector.FirstHalfYear);
        PopulationContext p = p_start1941.clone();

        /*
         * Первая передвижка с начала 1941 до середины 1941 года с использованием CBR_1940.
         * Её назначение -- дать предварительную оценку населения в середине 1941 года,
         * которая затем будет использована для исчисления средней за полугодие численности женских фертильных групп.
         */
        ForwardPopulationT fw = new ForwardPopulationT();
        fw.setBirthRateTotal(ap.CBR_1940);
        fw.forward(p, mt1941_1, 0.5);
        // p = rescaleToADH(p, ap);
        
        /*
         * Вторая передвижка от начала до середины 1941 года, на этот раз
         * с использованием ASFR для расчёта числа рождений
         */
        if (asfrs != null)
        {
            PopulationContext pavg = p_start1941.avg(p);
            double nbirths = asfr_calibration * 0.5 * asfrs.births(pavg.toPopulation());
            final int ndays = fw.birthDays(0.5);
            
            /*
             * Число рождений исходя из постоянства рождаемости и смертности должно возрастать,
             * но на деле оно снижалось с 1940 года.
             * 
             * Не пытаясь реконструировать кривую хода числа рождений в первом полугодии 1941 года, 
             * мы приближаем её плоской линией.
             */
            
            // double[] births = WarHelpers.births(ndays, nbirths, nbirths, nbirths);
            double[] births = Util.normalize(Util.repeat(ndays, 1), nbirths);
            double[] m_births = WarHelpers.male_births(births);
            double[] f_births = WarHelpers.female_births(births);

            fw = new ForwardPopulationT();
            fw.setBirthCount(m_births, f_births);
            p = p_start1941.clone();
            fw.forward(p, mt1941_1, 0.5);
            
            fr.births_byday = births;
        }

        fr.p_mid1941 = p;
        fr.observed_deaths_byGenderAge = fw.deathsByGenderAge();
        fr.observed_births = fw.getObservedBirths();
        
        fr.p_mid1941.clipLastDayAccumulation(fr.observed_deaths_byGenderAge);
        
        return fr;
    }
    
    /*
     * Перемасштабировать для точного совпадения общей численности полов с расчётом АДХ
    */
    @SuppressWarnings("unused")
    private PopulationContext rescaleToADH(PopulationContext p_mid1941, AreaParameters ap) throws Exception
    {
        if (ap.area == Area.USSR && Util.False)
        {
            final double USSR_1941_START = 195_392_000; // АДХ, "Население Советского Союза", стр. 77, 118, 126
            final double USSR_1941_MID = forward_6mo(USSR_1941_START, ap.growth_1940());

            /* АДХ, "Население Советского Союза", стр. 56, 74 */
            final double males_jun21 = 94_338_000;
            final double females_jun21 = 102_378_000;
            final double total_jun21 = males_jun21 + females_jun21;

            final double females_mid1941 = females_jun21 * USSR_1941_MID / total_jun21;
            final double males_mid1941 = males_jun21 * USSR_1941_MID / total_jun21;

            p_mid1941 = RescalePopulation.scaleTotal(p_mid1941, males_mid1941, females_mid1941);
        }
        
        return p_mid1941;
    }
}
