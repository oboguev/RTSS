package rtss.ww2losses.population1941;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.curves.CurveUtil;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.calc.RescalePopulation;
import rtss.data.population.projection.ForwardPopulationT;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;
import rtss.ww2losses.helpers.PeacetimeMortalityTables;
import rtss.ww2losses.helpers.WarHelpers;
import rtss.ww2losses.helpers.diag.DiagHelper;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population194x.UtilBase_194x;
import rtss.ww2losses.struct.HalfYearEntries.HalfYearSelector;

import static rtss.data.population.projection.ForwardPopulation.years2days;

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
        PopulationContext p;
        ForwardPopulationT fw;

        /*
         * Первая передвижка с начала 1941 до середины 1941 года с использованием CBR_1940.
         * Её назначение -- дать предварительную оценку населения в середине 1941 года,
         * которая затем будет использована для исчисления средней за полугодие численности женских фертильных групп.
         */
        fw = new ForwardPopulationT();
        fw.setBirthRateTotal(ap.CBR_1940);
        p = p_start1941.clone();
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
            double[] births = Util.normalize(Util.repeat(ndays, 1), nbirths);
            double[] m_births = WarHelpers.male_births(births);
            double[] f_births = WarHelpers.female_births(births);
            
            if (Util.False)
                DiagHelper.viewProjection(p_start1941.clone(), peacetimeMortalityTables, Gender.MALE, ndays);

            fw = new ForwardPopulationT();
            fw.setBirthCount(m_births, f_births);
            p = p_start1941.clone();
            fw.forward(p, mt1941_1, 0.5);
            
            fr.births_byday = births;

            if (Util.False)
                DiagHelper.view_mid1941(p, Gender.MALE);
        }

        fr.observed_deaths_byGenderAge = fw.deathsByGenderAge();
        fr.observed_births = fw.getObservedBirths();
        
        
        /*
         * Во втором полугодии 1941 года происходит склейка двух разнородных видов данных:
         * распаковки (дезагрегации) для старших возрастов и начинающегося учёта рождений.
         * 
         * Их разнородность приводит к тому, что в точке перехода (возраст 0.5 лет результата)
         * возникает разрыв, затем сказывающийся зубчатосьтью всех структур.
         * 
         * Устранить разрыв таким образом, чтобы сохранить сумму населения в возрасте 0-0.5 лет,
         * его численность в возрасте 0 дней и непрерывность кривой на возрастном участке 0-0.5 лет. 
         */
        if (Util.True)
        {
            fixDiscontinuity(p, Gender.MALE, fr);
            fixDiscontinuity(p, Gender.FEMALE, fr);
        }
        
        if (Util.False)
            DiagHelper.view_mid1941(p, Gender.MALE);

        if (Util.False)
        {
            /* отобразить график населения на начало 1941 года */
            PopulationChart.display("Население " + ap.area + " на начало 1941 года", p_start1941, "");
            
            /* отобразить график населения на середину 1941 года */
            PopulationChart.display("Население " + ap.area + " на середину 1941 года", p, "");
            
            Util.noop();
        }
        
        fr.p_mid1941 = p;
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
    
    @SuppressWarnings("unused")
    private void fixDiscontinuity(PopulationContext p, Gender gender, PopulationForwardingResult1941 fr) throws Exception
    {
        int ndays_05 = years2days(0.5);
        int ndays_15 = years2days(1.5);

        double[] a = p.asArray(Locality.TOTAL, gender);
        
        double[] v = CurveUtil.fill_linear(0, a[0], ndays_15 - 1, a[ndays_15]);
        double[] vd = CurveUtil.distort_matchsum(v, a[ndays_15], a[0], Util.sum_range(a, 0, ndays_15 - 1));
        
        Util.checkSame(Util.sum(vd), Util.sum_range(a, 0, ndays_15 - 1));
        double[] ad = a.clone();
        Util.insert(ad, vd, 0);
        Util.checkSame(Util.sum(ad), Util.sum(a));
        
        p.fromArray(Locality.TOTAL, gender, ad);
        
        // adjust observedDeaths
        for (int nd = ndays_05; nd < ndays_15; nd++)
        {
            fr.observed_deaths_byGenderAge.addDay(Locality.TOTAL, gender, nd - ndays_05, a[nd] - ad[nd]);
        }

        double va = Util.sum_range(a, 0, ndays_05 - 1);
        double vad = Util.sum_range(ad, 0, ndays_05 - 1);
        fr.observed_deaths_byGenderAge.addDay(Locality.TOTAL, gender, 0, va - vad);
    }
}
