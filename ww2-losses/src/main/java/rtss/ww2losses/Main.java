package rtss.ww2losses;

import rtss.data.ValueConstraint;
import rtss.data.asfr.AgeSpecificFertilityRatesByTimepoint;
import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.asfr.InterpolateASFR;
import rtss.data.curves.InterpolateYearlyToDailyAsValuePreservingMonotoneCurve;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.data.population.calc.RescalePopulation;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;
import rtss.ww2losses.HalfYearEntries.HalfYearSelector;
import rtss.ww2losses.ageline.AgeLineFactorIntensities;
import rtss.ww2losses.ageline.EvalAgeLineLossIntensities;
import rtss.ww2losses.helpers.ExportResults;
import rtss.ww2losses.helpers.PeacetimeMortalityTables;
import rtss.ww2losses.helpers.PrintHalfYears;
import rtss.ww2losses.helpers.PrintYears;
import rtss.ww2losses.helpers.ShowPopulationAgeSliceHistory;
import rtss.ww2losses.helpers.VerifyHalfYears;
import rtss.ww2losses.helpers.WarHelpers;
import rtss.ww2losses.model.Model;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population_194x.AdjustPopulation;
import rtss.ww2losses.population_194x.AdjustPopulation1941;
import rtss.ww2losses.population_194x.MortalityTable_1940;
import rtss.ww2losses.population_194x.Population_In_Middle_1941;
import rtss.ww2losses.util.CalibrateASFR;
import rtss.ww2losses.util.RebalanceASFR;

import static rtss.data.population.forward.ForwardPopulation.years2days;

import java.util.ArrayList;
import java.util.List;

// ### для девочек РСФСР провал в числе избыточных смертей в возрастах 10-15 на 1941.2(см .txt)

// ### при подсчётах actual_deaths, excess_deaths, CBR -- убирать протяжённые блоки отрицательных значений (РСФСР)
// ### причём отдельно по MALE и FEMALE
// ### смертность при числе смертей < 0 ???

public class Main
{
    public static void main(String[] args)
    {
        try
        {
            new Main(Area.USSR).main();
            new Main(Area.RSFSR).main();
            Util.out("");
            Util.out("=== Конец расчёта ===");
        }
        catch (Exception ex)
        {
            Util.err("*** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    private Main(Area area) throws Exception
    {
        this.area = area;
        this.ap = AreaParameters.forArea(area);
        this.p1946_actual = PopulationADH.getPopulation(ap.area, 1946).toPopulationContext();
        adjustForTuva();
        split_p1946();
    }

    Main(Model model) throws Exception
    {
        this(model.params.area);
        this.model = model;
    }

    /*
     * Корректировать младенческую и раннедетскую смертность в таблицах смертности
     * 1943-1945 гг. с учётом эффекта антибиотиков 
     */
    private boolean ApplyAntibiotics = Util.True;

    /*
     * Распечатывать диагностический вывод
     */
    private boolean PrintDiagnostics = Util.True;

    /*
     * Использовать если население на начало 1946 года уже не содержит эмигрантов
     */
    private boolean DeductEmigration = Util.False;

    /*
     * Заменить некоторые части отрицательного дефицита РФСР на нули 
     */
    private boolean CancelNegativeDeficit = Util.False;

    /*
     * Размер контекста отслеживания: только дети или все возраста
     */
    private static int PopulationContextSize = PopulationContext.ALL_AGES;

    private Area area;
    private AreaParameters ap;
    private static int MAX_AGE = Population.MAX_AGE;

    /* фактическое население на начало 1946 года */
    private PopulationContext p1946_actual;

    /* фактическое население на начало 1946 года рождённое до середины 1941 */
    private PopulationContext p1946_actual_born_prewar;

    /* фактическое население на начало 1946 года рождённое после середины 1941 */
    private PopulationContext p1946_actual_born_postwar;

    /* возрастные коэффициенты рождаемости */
    private AgeSpecificFertilityRatesByYear yearly_asfrs;
    private AgeSpecificFertilityRatesByTimepoint halfyearly_asfrs;
    private double asfr_calibration;

    /* таблицы смертности */
    private CombinedMortalityTable mt1940;
    private PeacetimeMortalityTables peacetimeMortalityTables;

    /* весовые коэффцииенты для факторов распределяющих потери по времени */
    private double aw_conscripts_rkka_loss = 0.9;
    private double aw_general_occupation = 0.4;

    /* 
    * интенсивность потерь РККА по полугодиям
    * (Г.Ф. Кривошеев и др, "Россия и СССР в войнах XX века : Книга потерь", М. : Вече, 2010, стр. 236, 242, 245)
    */
    private static final double[] rkka_loss_intensity = { 0, 3_137_673, 1_518_213, 1_740_003, 918_618, 1_393_811, 915_019, 848_872, 800_817, 0 };

    /* 
     * интенсивность оккупации по полугодиям
     * (Федеральная служба государственной статистики, "Великая отечественная война : юбилейный статистический сборник", М. 2020, стр. 36)
     */
    private static final double[] occupation_intensity = { 0, 37_265, 71_754, 77_177, 63_740, 47_258, 31_033, 5_041, 0, 0 };

    /* 
     * равномерная интенсивность по военным полугодиям
     */
    private static final double[] even_intensity = { 0, 1, 1, 1, 1, 1, 1, 1, 1, 0 };

    /* 
     * интенсивность иммиграции в РСФСР из западных ССР
     */
    private static final double[] rsfsr_immigration_intensity = { 0, 10.5, 1.8, 1.6, 0, 0, 0, 0, 0, 0 };

    /*
     * данные для полугодий начиная с середины 1941 и по начало 1946 года
     */
    private HalfYearEntries<HalfYearEntry> halves;

    /* параметры настроек при многократном автоматическом исполнении внешним драйвером */
    private Model model;

    /* директория для сохранения файлов с результатами (если null -- не сохранять) */
    public String exportDirectory = "c:\\@ww2losses\\export";

    /* дефицит населения на начало 1946 года, до поправки на иммиграцию */
    private PopulationContext deficit1946_raw_preimmigration;
    private PopulationContext deficit1946_adjusted_preimmigration;

    /* дефицит населения  на начало 1946 года, после поправки на иммиграцию */
    private PopulationContext deficit1946_raw_postimmigration;
    private PopulationContext deficit1946_adjusted_postimmigration;

    public static enum Phase
    {
        PRELIMINARY, ACTUAL
    };

    private Phase phase;

    void main() throws Exception
    {
        Util.out("");
        Util.out("**********************************************************************************");
        Util.out("Вычисление для " + area.toString());
        Util.out("");

        if (model != null)
        {
            this.aw_conscripts_rkka_loss = model.params.aw_conscripts_rkka_loss;
            this.aw_general_occupation = model.params.aw_general_occupation;
            this.PrintDiagnostics = model.params.PrintDiagnostics;
        }
        else
        {
            model = new Model();
            model.params = null;
        }

        if (ApplyAntibiotics)
        {
            Util.out("С учётом влияния антибиотиков");
        }
        else
        {
            Util.out("Без учёта влияния антибиотиков");
        }

        switch (area)
        {
        case USSR:
            yearly_asfrs = AgeSpecificFertilityRatesByYear.load("age_specific_fertility_rates/USSR/USSR-ASFR.xlsx");
            halfyearly_asfrs = InterpolateASFR.interpolate(yearly_asfrs, 1920, 1959, 2);
            break;

        case RSFSR:
            yearly_asfrs = AgeSpecificFertilityRatesByYear.load("age_specific_fertility_rates/survey-1960.xlsx");
            halfyearly_asfrs = InterpolateASFR.interpolate(yearly_asfrs, 1920, 1959, 2);
            break;
        }

        asfr_calibration = CalibrateASFR.calibrate1940(ap, yearly_asfrs);
        Util.out(String.format("Калибровочная поправка ASFR (возрастных коэффициентов женской плодовитости): %.3f", asfr_calibration));

        /* 
         * Перечитать ASFR для полугодий 1941 года,
         * оставив на первую половину 1941 года коэффициенты плодовитости 1940-го года,
         * а коэффициенты на вторую половину 1941-го года исправить соответственно для сохранения среднего за 1941 год значения. 
         */
        RebalanceASFR.rebalance_1941_halfyears(yearly_asfrs, halfyearly_asfrs);

        /* таблицы смертности для 1940 года и полугодий 1941-1945 при мирных условиях */
        mt1940 = new MortalityTable_1940(ap).evaluate();
        peacetimeMortalityTables = new PeacetimeMortalityTables(mt1940, ApplyAntibiotics);

        /* население на середину 1941 года */
        AdjustPopulation adjuster1941 = null;
        adjuster1941 = new AdjustPopulation1941(area);
        Population_In_Middle_1941 pm1941 = new Population_In_Middle_1941(ap, adjuster1941);
        PopulationContext p_mid1941 = new PopulationContext(PopulationContextSize);
        Population p_leftover = pm1941.evaluateAsPopulation(p_mid1941, mt1940);
        Util.assertion(p_leftover.sum() == 0);

        /* по передвижке на основе CBR_1940 */
        PopulationContext p_start1941 = pm1941.p_start_1941.forLocality(Locality.TOTAL).toPopulationContext();
        PopulationContext deaths_1941_1st_halfyear = pm1941.observed_deaths_1941_1st_halfyear_byGenderAge.clone();
        double births_1941_1st_halfyear = pm1941.observed_births_1941_1st_halfyear;

        /* по передвижке на основе ASFR */
        if (Util.True)
        {
            ForwardingResult fr = forward_1941_1st_halfyear(p_start1941, p_mid1941);
            p_mid1941 = fr.p_result;
            deaths_1941_1st_halfyear = fr.observed_deaths_byGenderAge;
            births_1941_1st_halfyear = fr.observed_births;
        }

        if (Util.False)
        {
            new PopulationChart("Население " + area + " на середину 1941 года")
                    .show("перепись", p_mid1941.toPopulation())
                    .display();
        }

        if (ap.area == Area.USSR)
        {
            stage_1(Phase.ACTUAL, p_start1941, deaths_1941_1st_halfyear, births_1941_1st_halfyear, p_mid1941, null);

        }
        else if (ap.area == Area.RSFSR)
        {
            stage_1(Phase.PRELIMINARY, p_start1941, deaths_1941_1st_halfyear, births_1941_1st_halfyear, p_mid1941, null);

            HalfYearEntries<HalfYearEntry> immigration_halves = halves;
            halves = null;

            stage_1(Phase.ACTUAL, p_start1941, deaths_1941_1st_halfyear, births_1941_1st_halfyear, p_mid1941, immigration_halves);

            double v1 = totalImmigration(immigration_halves);
            double v2 = totalImmigration(halves);

            // в случае расхождения делать stage_1 итеративно до схождения
            if (!Util.same(v1, v2))
                throw new Exception("Расхождение в исчисленном объёме иммиграции на предварительном и окончательном шагах");
        }

        evalNewBirths();
        evalDeathsForNewBirths_UnderPeacetimeChildMortality();
        fitDeathsForNewBirths_UnderActualWartimeChildMortality();

        new VerifyHalfYears(ap, halves).verify(false);

        PrintHalfYears.print(ap, halves, model.results);
        PrintYears.print(ap, halves, model.results);

        PopulationContext allExcessDeathsByDeathAge = allExcessDeathsByDeathAge();
        PopulationContext allExcessDeathsByAgeAt1946 = allExcessDeathsByAgeAt1946();

        if (Util.False)
        {
            new PopulationChart("Избыточные смерти " + area + " в 1941-1945 гг. по возрасту в момент смерти")
                    .show("смерти", allExcessDeathsByDeathAge.toPopulation())
                    .display();
        }

        if (Util.False)
        {
            new PopulationChart("Избыточные смерти " + area + " в 1941-1945 гг. по возрасту на начало 1946")
                    .show("смерти", allExcessDeathsByAgeAt1946.toPopulation())
                    .display();
        }

        // ### to do: also initial-stage deficit (pre-immigration)
        ExportResults.exportResults(exportDirectory, ap, halves,
                                    allExcessDeathsByDeathAge,
                                    allExcessDeathsByAgeAt1946,
                                    deficit1946_raw_preimmigration, deficit1946_adjusted_preimmigration,
                                    deficit1946_raw_postimmigration, deficit1946_adjusted_postimmigration);
    }

    private void stage_1(
            Phase phase,
            PopulationContext p_start1941,
            PopulationContext deaths_1941_1st_halfyear,
            double births_1941_1st_halfyear,
            PopulationContext p_mid1941,
            HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        this.phase = phase;

        /* передвижка по полугодиям для мирных условий */
        halves = evalHalves_step_6mo(p_start1941, deaths_1941_1st_halfyear, births_1941_1st_halfyear, p_mid1941, immigration_halves);

        if (Util.False)
        {
            String point = "1946.1";
            new PopulationChart("Ожидаемое население " + area.toString() + " на " + point)
                    .show("1", halves.get(point).p_nonwar_without_births)
                    .display();
        }

        if (Util.False)
        {
            new PopulationChart("Сверхсмертность населения " + area.toString() + " на 1946.1")
                    .show("1", eval_deficit_1946(halves, DeductEmigration))
                    .display();
        }

        evalDeficit1946(immigration_halves);
        evalAgeLines();
    }

    /* ================================================================================== */

    /*
     * Передвижка от начала до середины 1941 года с использованием ASFR для расчёта рождений
     */
    private ForwardingResult forward_1941_1st_halfyear(PopulationContext p_start1941, PopulationContext p_mid1941) throws Exception
    {
        PopulationContext pavg = p_start1941.avg(p_mid1941);

        PopulationContext p = p_start1941.clone();
        CombinedMortalityTable mt = peacetimeMortalityTables.get(1941, HalfYearSelector.FirstHalfYear);

        ForwardPopulationT fw = new ForwardPopulationT();
        int ndays = fw.birthDays(0.5);

        double nbirths = asfr_calibration * 0.5 * halfyearly_asfrs.getForTimepoint("1941.0").births(pavg.toPopulation());

        double[] births = WarHelpers.births(ndays, nbirths, nbirths, nbirths);
        double[] m_births = WarHelpers.male_births(births);
        double[] f_births = WarHelpers.female_births(births);
        fw.setBirthCount(m_births, f_births);
        fw.forward(p, mt, 0.5);

        ForwardingResult res = new ForwardingResult();
        res.p_result = p.clone();
        res.observed_deaths_byGenderAge = fw.deathsByGenderAge().clone();
        res.observed_births = nbirths;
        return res;
    }

    public static class ForwardingResult
    {
        public PopulationContext p_result;
        public PopulationContext observed_deaths_byGenderAge;
        public double observed_births;
    }

    /* ================================================================================== */

    /*
     * Подготовить полугодовые сегменты.
     * Передвижка для мирного времени с шагом полгода от середины 1941 до начала 1946 года.
     */
    @SuppressWarnings("unused")
    private HalfYearEntries<HalfYearEntry> evalHalves_step_6mo(
            PopulationContext p_start1941,
            PopulationContext deaths_1941_1st_halfyear,
            double births_1941_1st_halfyear,
            PopulationContext p_mid1941,
            HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        HalfYearEntries<HalfYearEntry> halves = new HalfYearEntries<HalfYearEntry>();
        PopulationContext pctx = p_mid1941.clone();

        HalfYearEntry curr, prev;
        int year = 1941;

        /* первое полугодие 1941 */
        HalfYearSelector half = HalfYearSelector.FirstHalfYear;
        prev = curr = new HalfYearEntry(year, half,
                                        p_start1941.clone(),
                                        p_start1941.clone());
        curr.actual_deaths = deaths_1941_1st_halfyear.clone();
        curr.actual_peace_deaths = deaths_1941_1st_halfyear.clone();
        curr.actual_excess_wartime_deaths = newPopulationContext();
        curr.expected_nonwar_deaths = curr.actual_deaths.sum();
        curr.expected_nonwar_births = births_1941_1st_halfyear;

        halves.add(curr);

        /* второе полугодие 1941 */
        half = HalfYearSelector.SecondHalfYear;
        curr = new HalfYearEntry(year, half, pctx.clone(), pctx.clone());
        prev.next = curr;
        curr.prev = prev;
        prev = curr;
        halves.add(curr);

        /* подготовиться к передвижке населения с учётом рождений после середины 1941 года */
        PopulationContext pwb = pctx.clone();

        /* подготовиться к передвижке населения без учёта рождений после середины 1941 года (только наличного на середину 1941 года) */
        PopulationContext pxb = pctx.clone();

        /* продвигать с шагом по полгода до января 1946 */
        for (;;)
        {
            int current_year = year;
            HalfYearSelector current_half = half;

            if (half == HalfYearSelector.FirstHalfYear)
            {
                if (year == 1946)
                    break;
                half = HalfYearSelector.SecondHalfYear;
            }
            else
            {
                half = HalfYearSelector.FirstHalfYear;
                year++;
            }

            /* определить таблицу смертности, с учётом падения детской смертности из-за введения антибиотиков */
            CombinedMortalityTable mt = peacetimeMortalityTables.get(current_year, current_half);
            curr.peace_mt = mt;

            /* передвижка на следующие полгода населения с учётом рождений */
            ForwardPopulationT fw1 = new ForwardPopulationT();
            fw1.setBirthRateTotal(ap.CBR_1940);
            fw1.forward(pwb, mt, 0.5);
            if (immigration_halves != null && immigration_halves.get(year, half).prev.immigration != null)
            {
                pwb = pwb.add(immigration_halves.get(year, half).prev.immigration, ValueConstraint.NON_NEGATIVE);
            }

            /* передвижка на следующие полгода населения без учёта рождений */
            ForwardPopulationT fw2 = new ForwardPopulationT();
            fw2.setBirthRateTotal(0);
            fw2.forward(pxb, mt, 0.5);
            if (immigration_halves != null && immigration_halves.get(year, half).prev.immigration != null)
            {
                pxb = pxb.add(immigration_halves.get(year, half).prev.immigration, ValueConstraint.NON_NEGATIVE);
            }

            /* сохранить результаты в полугодовой записи */
            curr = new HalfYearEntry(year, half, pwb.clone(), pxb.clone());
            prev.expected_nonwar_births = fw1.getObservedBirths();
            prev.expected_nonwar_deaths = fw2.getObservedDeaths();

            curr.prev = prev;
            prev.next = curr;
            prev = curr;
            halves.add(curr);
        }

        for (HalfYearEntry he : halves)
        {
            if (he.year == 1946)
                break;

            if (he.peace_mt == null)
                he.peace_mt = peacetimeMortalityTables.get(he.year, he.halfyear);

            he.peace_lx_male = mt2lx(he.peace_mt, Locality.TOTAL, Gender.MALE);
            he.peace_lx_female = mt2lx(he.peace_mt, Locality.TOTAL, Gender.FEMALE);
        }

        return halves;
    }

    /* ================================================================================== */

    /* вычесть население Тувы из населения начала 1946 года */
    private void adjustForTuva() throws Exception
    {
        final double tuva_pop = 100_000;
        double pop = p1946_actual.sum();
        double scale = (pop - tuva_pop) / pop;
        p1946_actual = RescalePopulation.scaleBy(p1946_actual, scale);
    }

    /*
     * Вычислить половозрастную структуру дефицита населения на 1946.1 для графического изображения.
     * Возвращаемый дефицит охватывает только наличное на начало войны население, без учёта рождений
     * во время войны.  
     */
    private PopulationContext eval_deficit_1946(HalfYearEntries<HalfYearEntry> halves, boolean deductEmigration) throws Exception
    {
        PopulationContext p1946_expected_without_births = halves.last().p_nonwar_without_births;
        PopulationContext deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar, ValueConstraint.NONE);
        if (deductEmigration)
            deficit = deficit.sub(emigration(), ValueConstraint.NONE);
        return deficit;
    }

    /*
     * Разбить наличное на начало 1946 года население на родившееся до и после середины 1941 года 
     */
    private void split_p1946() throws Exception
    {
        int nd_4_5 = 9 * years2days(0.5);

        p1946_actual_born_postwar = p1946_actual.selectByAgeDays(0, nd_4_5 - 1);
        p1946_actual_born_prewar = p1946_actual.selectByAgeDays(nd_4_5, years2days(MAX_AGE + 1));

        double v_total = p1946_actual.sum();
        double v_prewar = p1946_actual_born_prewar.sum();
        double v_postwar = p1946_actual_born_postwar.sum();

        if (Util.differ(v_total, v_prewar + v_postwar))
            Util.err("Ошибка расщепления");
    }

    /*
     * Половозрастная структура эмиграции.
     * 80% мужчин, 20% женщин, возрасты 20-50,
     * СССР = 850 тыс., РСФСР = 70 тыс.
     */
    private PopulationContext emigration() throws Exception
    {
        double emig = 0;

        switch (area)
        {
        case USSR:
            emig = 850_000;
            break;

        case RSFSR:
            emig = 70_000;
            break;
        }

        PopulationContext p = new PopulationContext(PopulationContext.ALL_AGES);
        p.beginTotal();

        for (int age = 0; age <= MAX_AGE; age++)
        {
            p.setYearValue(Gender.MALE, age, 0);
            p.setYearValue(Gender.FEMALE, age, 0);
        }

        for (int age = 20; age <= 50; age++)
        {
            p.setYearValue(Gender.MALE, age, 0.8);
            p.setYearValue(Gender.FEMALE, age, 0.2);
        }

        p = RescalePopulation.scaleAllTo(p, emig);

        return p;
    }

    /* ======================================================================================================= */

    private void evalDeficit1946(HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        PopulationContext p1946_expected_with_births = halves.last().p_nonwar_with_births;
        PopulationContext p1946_expected_without_births = halves.last().p_nonwar_without_births;
        PopulationContext p1946_expected_newonly = p1946_expected_with_births.sub(p1946_expected_without_births);
        PopulationContext p1941_mid = halves.get(1941, HalfYearSelector.SecondHalfYear).p_nonwar_without_births;

        /*
         * проверить, что сумма expected_nonwar_deaths примерно равна разнице численностей
         * p_nonwar_without_births
         */
        double v_sum_deaths = 0;
        double v_sum_immigration = 0;
        for (HalfYearEntry curr : halves)
        {
            if (curr.year == 1941 && curr.halfyear == HalfYearSelector.FirstHalfYear)
                continue;
            if (curr.year == 1946)
                continue;

            v_sum_deaths += curr.expected_nonwar_deaths;

            if (immigration_halves != null)
            {
                v_sum_immigration += immigration_halves.get(curr.year, curr.halfyear).immigration.sum();
            }
        }

        double v = p1941_mid.sum();
        v -= p1946_expected_without_births.sum();
        if (Util.differ(v_sum_deaths - v_sum_immigration, v, 0.0001))
            Util.err("Несовпадение числа смертей");

        /* =================================================== */

        PopulationContext deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar, ValueConstraint.NONE);
        PopulationContext deficit_wb_raw = p1946_expected_with_births.sub(p1946_actual, ValueConstraint.NONE);
        PopulationContext deficit_wb_adjusted = null;

        if (Util.False)
        {
            new PopulationChart("Дефицит " + ap.area)
                    .show("дефицит", deficit)
                    .display();
        }

        if (Util.False)
        {
            deficit.setValueConstraint(ValueConstraint.NONE);
            new PopulationChart("Дефицит " + ap.area + " сдвинутый по возрасту вниз на 5 лет")
                    .show("дефицит", deficit.moveDown(5))
                    .display();
        }

        if (area == Area.RSFSR && CancelNegativeDeficit)
        {
            if (PrintDiagnostics && phase == Phase.ACTUAL)
                WarHelpers.validateDeficit(deficit, "До эмиграции и отмены отрицательных женских значений:");

            /*
             * Для РСФСР отменить отрицательные значения дефицита женского населения
             * в возрастах 15-60 лет как вызванные вероятно миграцией.
             */
            cancelNegativeDeficit(cancelDeficitRSFSR);
            deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar, ValueConstraint.NONE);
            deficit_wb_adjusted = p1946_expected_with_births.sub(p1946_actual, ValueConstraint.NONE);
        }

        v = p1946_expected_with_births.sum();
        v -= p1946_actual.sum();
        double v_total = v;
        if (phase == Phase.ACTUAL)
            outk("Общий дефицит населения к январю 1946, тыс. чел.", v);

        v = p1946_expected_without_births.sum();
        v -= p1946_actual_born_prewar.sum();
        double v_prewar = v;
        if (phase == Phase.ACTUAL)
            outk("Дефицит наличного в начале войны населения к январю 1946, тыс. чел.", v);

        v = p1946_expected_newonly.sum();
        v -= p1946_actual_born_postwar.sum();
        double v_postwar = v;
        if (phase == Phase.ACTUAL)
            outk("Дефицит рождённного во время войны населения к январю 1946, тыс. чел.", v);

        if (Util.differ(v_total, v_prewar + v_postwar))
            Util.err("Расхождение категорий дефицита");

        if (PrintDiagnostics && Util.False && phase == Phase.ACTUAL)
        {
            ShowPopulationAgeSliceHistory.showWithoutBirhts(ap, p1946_actual, halves, 3);
            ShowPopulationAgeSliceHistory.showWithoutBirhts(ap, p1946_actual, halves, 4);
        }

        /* оставить только сверхсмертность */
        if (DeductEmigration)
        {
            PopulationContext emigration = emigration();
            if (phase == Phase.ACTUAL)
            {
                out("");
                outk("Эмиграция, тыс. чел.", emigration.sum());
            }
            deficit = deficit.sub(emigration, ValueConstraint.NONE);
        }
        else
        {
            if (phase == Phase.ACTUAL)
            {
                out("");
                out("Эмиграция ещё включена в половозрастную структуру начала 1946 года, и не вычитается из смертности");
            }
        }

        if (area == Area.RSFSR && CancelNegativeDeficit)
        {
            deficit = cancelNegativeDeficit(deficit, cancelDeficitRSFSR);
            deficit_wb_raw = cancelNegativeDeficit(deficit_wb_raw, cancelDeficitRSFSR);
            deficit_wb_adjusted = cancelNegativeDeficit(deficit_wb_adjusted, cancelDeficitRSFSR);
        }

        if (PrintDiagnostics && phase == Phase.ACTUAL)
        {
            if (area == Area.RSFSR && CancelNegativeDeficit)
                WarHelpers.validateDeficit(deficit, "После эмиграции и отмены отрицательных женских значений:");
            else
                WarHelpers.validateDeficit(deficit);
        }

        if (Util.False)
        {
            /* график сверхсмертности */
            PopulationChart.display("Cверхсмертность населения " + area + " накопленная с середины 1941 по конец 1945 года",
                                    deficit,
                                    "1");
        }

        /*
         * разбить сверхсмертность на категории с примерной (предварительной) численностью 
         */
        double deficit_total = deficit.sum();
        double deficit_m_conscripts = subcount(deficit, Gender.MALE, Constants.CONSCRIPT_AGE_FROM, Constants.CONSCRIPT_AGE_TO + 4.5);
        double deficit_f_fertile = subcount(deficit, Gender.FEMALE, 15, 58);
        double deficit_other = deficit_total - deficit_m_conscripts - deficit_f_fertile;

        if (phase == Phase.ACTUAL)
        {
            out("");
            outk("Предварительная сверхсмертность всего наличного на середину 1941 года населения к концу 1945 года, по дефициту", deficit_total);
            out("Предварительная разбивка на категории по временному окну:");
            outk("    Сверхсмертность [по дефициту] мужчин призывного возраста", deficit_m_conscripts);
            outk("    Сверхсмертность [по дефициту] женщин фертильного возраста", deficit_f_fertile);
            outk("    Сверхсмертность [по дефициту] остального наличного на середину 1941 года населения", deficit_other);

            this.deficit1946_raw_postimmigration = deficit_wb_raw;
            this.deficit1946_adjusted_postimmigration = deficit_wb_adjusted;
        }
        else if (phase == Phase.PRELIMINARY)
        {
            if (this.deficit1946_raw_preimmigration == null)
                this.deficit1946_raw_preimmigration = deficit_wb_raw;
            
            if (this.deficit1946_adjusted_preimmigration == null)
                this.deficit1946_adjusted_preimmigration = deficit_wb_adjusted;
        }
    }

    /* ======================================================================================================= */

    public static class CancelDeficit
    {
        public Gender gender;
        public int age1;
        public int age2;

        public CancelDeficit(Gender gender, int age1, int age2)
        {
            this.gender = gender;
            this.age1 = age1;
            this.age2 = age2;
        }
    }

    private static CancelDeficit[] cancelDeficitRSFSR = {
                                                          new CancelDeficit(Gender.MALE, 7, 10),
                                                          new CancelDeficit(Gender.FEMALE, 7, 11),
                                                          new CancelDeficit(Gender.FEMALE, 15, 60)
    };

    private void cancelNegativeDeficit(CancelDeficit[] cancels) throws Exception
    {
        PopulationContext p1946_expected_without_births = halves.last().p_nonwar_without_births;
        PopulationContext deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar, ValueConstraint.NONE);

        for (CancelDeficit cancel : cancels)
        {
            for (int age = cancel.age1; age <= cancel.age2; age++)
            {
                double v = deficit.getYearValue(cancel.gender, age);
                if (v < 0)
                {
                    // p1946_actual.addYearValue(gender, age, v);
                    unneg(p1946_actual, cancel.gender, age, deficit);
                }
            }
        }

        p1946_actual.makeBoth();
        p1946_actual.recalcTotal();
        split_p1946();
    }

    private PopulationContext cancelNegativeDeficit(PopulationContext deficit, CancelDeficit[] cancels) throws Exception
    {
        deficit.setValueConstraint(ValueConstraint.NONE);

        for (CancelDeficit cancel : cancels)
        {
            for (int age = cancel.age1; age <= cancel.age2; age++)
            {
                double v = deficit.getYearValue(cancel.gender, age);
                if (v < 0)
                {
                    // p1946_actual.addYearValue(gender, age, v);
                    unneg(p1946_actual, cancel.gender, age, deficit);
                    deficit.setYearValue(cancel.gender, age, 0);
                }
            }
        }

        deficit.makeBoth();
        deficit.recalcTotal();

        p1946_actual.makeBoth();
        p1946_actual.recalcTotal();

        split_p1946();

        return deficit;
    }

    private void unneg(PopulationContext p, Gender gender, int age, PopulationContext deficit) throws Exception
    {
        for (int nd = p.firstDayForAge(age); nd <= p.lastDayForAge(age); nd++)
        {
            double v = deficit.getDay(Locality.TOTAL, gender, nd);
            p.addDay(Locality.TOTAL, gender, nd, -v);
        }
    }

    /* ======================================================================================================= */

    /*
     * Обработать возрастные линии для наличного на середину 1941 года населения.
     * 
     * Для каждой линии определить интенсивность потерь, которая при данных весовых коэффициентах разбивки attrition 
     * связывает начальную численность линии (на середину 1941 года) с конечной численностью (на начало 1946 года).
     * 
     * Затем с использованием найденной интенсивности потерь расчитать ход линии по полугодиям,
     * включая число смертей за каждое полугодие и численность населения в этой линии на каждое полугодие. 
     */
    private void evalAgeLines() throws Exception
    {
        /* нормализованный полугодовой коэффициент распределения потерь для не-призывного населения */
        double[] ac_general = wsum(aw_general_occupation, occupation_intensity,
                                   1 - aw_general_occupation, even_intensity);

        /* нормализованный полугодовой коэффициент распределения потерь для призывного населения */
        double[] ac_conscripts = wsum(aw_conscripts_rkka_loss, rkka_loss_intensity,
                                      1.0 - aw_conscripts_rkka_loss, ac_general);

        /* нормализованный полугодовой коэффициент интенсивности иммиграции в РСФСР */
        double[] ac_rsfsr_immigration = Util.normalize(rsfsr_immigration_intensity);

        /* 
         * вычислить коэфициенты интенсивности военных потерь для каждого возраста и пола,
         * подогнав их так, чтобы начальное население линии (середины 1941) приходило к конечному (начала 1946) 
         */
        EvalAgeLineLossIntensities eval = new EvalAgeLineLossIntensities(halves, ac_general, ac_conscripts);
        AgeLineFactorIntensities alis = eval.evalPreliminaryLossIntensity(p1946_actual);

        if (Util.False)
        {
            alis.display("Интенсивность военных потерь " + area);
            // PopulationContext p = alis.toPopulationContext();
            // Util.noop();
        }

        AgeLineFactorIntensities amig = null;

        if (ap.area == Area.RSFSR)
        {
            /*
             * Для РСФСР для определенных групп (gender, age1-age2)
             * устранить отрицательные коэффициенты военных потерь (или провалы в смертности), вызываемые иммиграцией.
             * 
             * Интерполировать значения коэффициента военных потерь (хранимые в @alis) между положительными 
             * точками age1-age2 для устранения промежуточных отрицательных значений.
             * 
             * Затем найти положительный коэф. иммиграционной интенсивности в этих возрастах/полах (для этих половозрастных линий).
             */
            alis.unnegInterpolateYears(Gender.MALE, 2.5, 7.5);
            alis.unnegInterpolateYears(Gender.FEMALE, 2.1, 7.37);
            alis.unnegInterpolateYears(Gender.FEMALE, 42.5, 57.5);

            // alis.display("Исправленная интенсивность военных потерь " + area);
            // PopulationContext p_alis = alis.toPopulationContext();

            eval.setImmigration(ac_rsfsr_immigration);
            amig = new AgeLineFactorIntensities();

            // вычислить мнтенсивность иммиграции
            eval.evalMigration(p1946_actual, amig, alis, Gender.MALE, 2.5, 7.5);
            eval.evalMigration(p1946_actual, amig, alis, Gender.FEMALE, 2.1, 7.37);
            eval.evalMigration(p1946_actual, amig, alis, Gender.FEMALE, 42.5, 57.5);

            // amig.display("Интенсивность иммиграции" + area);
            // PopulationContext p_amig = amig.toPopulationContext();
            // Util.noop();
        }

        /* 
         * расчёт (и действительное построение) возрастных линий с учётов найденных коэфициентов интенсивности потерь
         * и иммиграционной интенсивности 
         */
        eval.processAgeLines(alis, amig, p1946_actual);

        /* compare halves.last.actual_population vs. p1946_actual_born_prewar */
        PopulationContext diff = p1946_actual_born_prewar.sub(halves.last().actual_population, ValueConstraint.NONE);
        Util.assertion(Math.abs(diff.sum(0, MAX_AGE - 1)) < 100);
        Util.assertion(Math.abs(diff.getYearValue(Gender.BOTH, MAX_AGE)) < 1200);

        HalfYearEntry he = halves.get("1941.1");
        he.actual_population = he.p_nonwar_with_births.clone();

        he = halves.get("1941.2");
        he.actual_population = he.p_nonwar_with_births.clone();

        for (HalfYearEntry hx : halves)
            hx.actual_population_without_births = hx.actual_population.clone();

        printExcessDeaths();
    }

    private void printExcessDeaths() throws Exception
    {
        double sum_all = 0;
        double sum_conscripts = 0;

        /* к середине полугодия исполнится FROM/TO */
        int conscript_age_from = years2days(Constants.CONSCRIPT_AGE_FROM - 0.25);
        int conscript_age_to = years2days(Constants.CONSCRIPT_AGE_TO - 0.25);

        for (HalfYearEntry he : halves)
        {
            if (he.actual_excess_wartime_deaths == null || he.actual_excess_wartime_deaths.sum() == 0)
                continue;

            sum_all += he.actual_excess_wartime_deaths.sum();
            sum_conscripts += he.actual_excess_wartime_deaths.sumDays(Gender.MALE, conscript_age_from, conscript_age_to);
        }

        // ###
        outk("Избыточное число всех смертей", sum_all);
        // ###
        outk(String.format("Избыточное число смертей мужчин призывного возраста (%.1f-%.1f лет)",
                           Constants.CONSCRIPT_AGE_FROM,
                           Constants.CONSCRIPT_AGE_TO),
             sum_conscripts);
    }

    public double totalImmigration(HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        double sum = 0;

        for (HalfYearEntry he : immigration_halves)
        {
            if (he.immigration != null)
                sum += he.immigration.sum();
        }

        return sum;
    }

    /* ======================================================================================================= */

    /*
     * Вычислить фактическое число рождений в военное время
     * по данным анамнеситического опроса 1960 года 
     * и по числу женщин фертильного возраста согласно расчитанной ранее структуре остатка населения наличного в начале войны
     */
    private void evalNewBirths() throws Exception
    {
        for (HalfYearEntry he : halves)
        {
            if (he.next == null)
                break;

            /* взрослое население в начале периода */
            PopulationContext p1 = he.actual_population;

            /* взрослое население в конце периода */
            PopulationContext p2 = he.next.actual_population;

            /* среднее взрослое население за период */
            PopulationContext pavg = p1.avg(p2, ValueConstraint.NONE);

            if (Util.False)
            {
                he.actual_births = asfr_calibration * 0.5 * yearly_asfrs.getForYear(he.year).births(pavg.toPopulation());
            }
            else
            {
                String timepoint = null;
                switch (he.halfyear)
                {
                case FirstHalfYear:
                    timepoint = he.year + ".0";
                    break;
                case SecondHalfYear:
                    timepoint = he.year + ".1";
                    break;
                }

                he.actual_births = asfr_calibration * 0.5 * halfyearly_asfrs.getForTimepoint(timepoint).births(pavg.toPopulation());
            }
        }

        /*
         * Для 1941 года (оба полугодия)
         */
        if (Util.False)
        {
            HalfYearEntry he1 = halves.get(0);
            HalfYearEntry he2 = halves.get(1);
            double delta = he1.actual_births - he1.expected_nonwar_births;
            he1.actual_births = he1.expected_nonwar_births;
            he2.actual_births += delta;
        }
    }

    /* ======================================================================================================= */

    /*
     * Вычислить ожидаемое число смертей от новых рождений в военное время (от фактического числа военных рождений),
     * ожидаемый остаток фактически рождённых в 1941.вт.пол. - 1945.вт.пол. к началу 1946 при детской смертности 
     * мирных условий, и дефицит остатка на начало 1946 года из-за возросшей в военное время детской смертности. 
     */
    private void evalDeathsForNewBirths_UnderPeacetimeChildMortality() throws Exception
    {
        /*
         * передвижка новрождаемого населения по полугодиям
         * от середины 1941 с p = empty
         * и добавлением числа рождений за полугодие согласно he.actual_births
         */
        PopulationContext p = newPopulationContext();

        for (HalfYearEntry he = halves.get("1941.2"); he.year != 1946; he = he.next)
        {
            ForwardPopulationT fw = new ForwardPopulationT();
            int ndays = fw.birthDays(0.5);

            // добавить фактические рождения, распределив их по дням
            double nb1 = he.prev.actual_births;
            double nb2 = he.actual_births;
            double nb3 = (he.next != null) ? he.next.actual_births : nb2;
            double[] births = WarHelpers.births(ndays, nb1, nb2, nb3);
            double[] m_births = WarHelpers.male_births(births);
            double[] f_births = WarHelpers.female_births(births);
            fw.setBirthCount(m_births, f_births);

            fw.forward(p, he.peace_mt, 0.5);

            // остаток родившихся за время войны
            he.next.wartime_born_remainder_UnderPeacetimeChildMortality = p.clone();

            // число смертей от рождений при мирной смертности
            Util.assertion(Util.same(fw.getObservedDeaths(), fw.deathsByGenderAge().sum()));
            he.actual_warborn_deaths_baseline = fw.getObservedDeaths();
            he.actual_peace_deaths_from_newborn = fw.deathsByGenderAge().clone();
        }

        double v1 = p.sum();
        double v2 = p1946_actual_born_postwar.sum();

        outk("Сверхсмертность рождённых во время войны, по дефициту к началу 1946 года, тыс. чел.", v1 - v2);
    }

    /* ======================================================================================================= */

    /*
     * Итеративно повторять передвижку рождений военного времени до начала 1946 года с со-пропорциональным повышением 
     * коэффициентов смертности детских лет на общий множитель (распадающийся в возрастах 0-4 года и к возрасту 5 лет
     * становящийся единицей), однако сохраняя разницу между таблицами разных лет связанную с введением антибиотиков. 
     * 
     * Мы итеративно повторяем передвижку до тех пор, пока не найдётся множитель дающий остаток рождённых в годы войны 
     * на начало 1946 года равный их численности по реконструкции АДХ (обратным отсчётом от переписи 1959 года, 
     * для РСФСР с учётом межреспубликанской миграции). 
     * 
     * Такая передвижка даст приближение к фактическому распределению смертей рождённых в годы войны.
     */
    private void fitDeathsForNewBirths_UnderActualWartimeChildMortality() throws Exception
    {
        double m1 = 0.5;
        double m2 = 2.5;

        for (;;)
        {
            double m = (m1 + m2) / 2;
            double diff = fitDeathsForNewBirths(m, false);

            if (Math.abs(diff) < 200)
            {
                Util.out(String.format("Множитель детской смертности военного времени: %.2f", m));
                fitDeathsForNewBirths(m, true);
                break;
            }

            if (diff > 0)
                m1 = m;
            else
                m2 = m;
        }

        double excess = 0;
        for (HalfYearEntry he = halves.get("1941.2"); he.year != 1946; he = he.next)
            excess += he.actual_warborn_deaths - he.actual_warborn_deaths_baseline;
        outk("Сверхсмертность рождённых во время войны, фактическая к началу 1946 года, тыс. чел.", excess);
    }

    private double fitDeathsForNewBirths(double multiplier, boolean record) throws Exception
    {
        /*
         * передвижка новрождаемого населения по полугодиям
         * от середины 1941 с добавлением числа рождений за полугодие согласно he.actual_births
         */
        PopulationContext p = newPopulationContext();

        for (HalfYearEntry he = halves.get("1941.2"); he.year != 1946; he = he.next)
        {
            ForwardPopulationT fw = new ForwardPopulationT();
            int ndays = fw.birthDays(0.5);

            // добавить фактические рождения, распределив их по дням
            double nb1 = he.prev.actual_births;
            double nb2 = he.actual_births;
            double nb3 = (he.next != null) ? he.next.actual_births : nb2;
            double[] births = WarHelpers.births(ndays, nb1, nb2, nb3);
            double[] m_births = WarHelpers.male_births(births);
            double[] f_births = WarHelpers.female_births(births);
            fw.setBirthCount(m_births, f_births);

            List<PatchInstruction> instructions = new ArrayList<>();
            PatchInstruction instruction = new PatchInstruction(PatchOpcode.MultiplyWithDecay, 0, 5, multiplier * imr_fy_multiplier(he), 1.0);
            // PatchInstruction instruction = new PatchInstruction(PatchOpcode.Multiply, 0, 7, multiplier * imr_fy_multiplier(he));
            instructions.add(instruction);
            CombinedMortalityTable mt = PatchMortalityTable.patch(mt1940, instructions, "множитель смертности " + multiplier);

            fw.forward(p, mt, 0.5);

            if (record)
            {
                // остаток родившихся за время войны
                he.next.wartime_born_remainder_UnderActualWartimeChildMortality = p.clone();

                // ввести остаток рождённых до конца полугодия в население начала следующего полугодия
                merge(p, he.next.actual_population);

                // число смертей от рождений
                Util.assertion(Util.same(fw.getObservedDeaths(), fw.deathsByGenderAge().sum()));

                he.actual_warborn_deaths = fw.getObservedDeaths();
                add(fw.deathsByGenderAge(), he.actual_deaths);
                add(he.actual_peace_deaths_from_newborn, he.actual_peace_deaths);

                PopulationContext delta = fw.deathsByGenderAge().sub(he.actual_peace_deaths_from_newborn, ValueConstraint.NONE);
                add(delta, he.actual_excess_wartime_deaths);
            }
        }

        // проверка
        if (record)
        {
            for (HalfYearEntry he : halves)
            {
                if (he.year != 1946)
                    check_actual_deaths(he);
            }
        }

        double v1 = p.sum();
        double v2 = p1946_actual_born_postwar.sum();

        return v1 - v2;
    }

    private double imr_fy_multiplier(HalfYearEntry he) throws Exception
    {
        String yh = he.year + "." + he.halfyear.seq(1);

        switch (yh)
        {
        case "1941.1":
            return 1.00;

        case "1941.2":
            return 1.27;

        case "1942.1":
            return 2.25;

        case "1942.2":
            return 2.30;

        case "1943.1":
            return 1.20;

        case "1943.2":
            return 1.08;

        case "1944.1":
            return 0.72;

        case "1944.2":
            return 0.54;

        case "1945.1":
            return 0.40;

        case "1945.2":
            return 0.40;

        default:
            throw new IllegalArgumentException();
        }
    }

    private void check_actual_deaths(HalfYearEntry he) throws Exception
    {
        check_actual_deaths(he, Gender.MALE);
        check_actual_deaths(he, Gender.FEMALE);
    }

    private void check_actual_deaths(HalfYearEntry he, Gender gender) throws Exception
    {
        for (int nd = 0; nd <= he.actual_deaths.MAX_DAY; nd++)
        {
            double total = he.actual_deaths.getDay(Locality.TOTAL, gender, nd);
            double excess_wartime = he.actual_excess_wartime_deaths.getDay(Locality.TOTAL, gender, nd);
            double peace = he.actual_peace_deaths.getDay(Locality.TOTAL, gender, nd);
            Util.assertion(Util.same(peace + excess_wartime, total));
        }
    }

    /* ======================================================================================================= */

    /*
     * Взвешенная сумма w1*ww1 + w2*ww2
     * 
     * Массивы ww1 и ww2 предварительно нормализуются по сумме всех членов на 1.0
     * (без изменения начальных копий).
     * 
     * Возвращаемый результат также нормализуется. 
     */
    private double[] wsum(double w1, double[] ww1, double w2, double[] ww2) throws Exception
    {
        ww1 = Util.normalize(ww1);
        ww2 = Util.normalize(ww2);

        ww1 = Util.multiply(ww1, w1);
        ww2 = Util.multiply(ww2, w2);

        double[] ww = Util.add(ww1, ww2);
        ww = Util.normalize(ww);

        return ww;
    }

    private double subcount(PopulationContext p, Gender gender, double age1, double age2) throws Exception
    {
        double sum = 0;

        // весовые коэффициенты для возрастного окна
        double[] weights = { 0.5, 1.5, 2.5, 3.5 };

        for (int k = 0; k < weights.length; k++)
        {
            double weight = weights[k] / 4.0;
            double v = p.getYearValue(gender, age1 + k);
            sum += v * weight;
        }

        for (int k = 0; k < weights.length; k++)
        {
            double weight = weights[k] / 4.0;
            double v = p.getYearValue(gender, age2 - k - 1);
            sum += v * weight;
        }

        int nd1 = years2days(age1 + weights.length) + 1;
        int nd2 = years2days(age2 - weights.length) - 1;

        sum += p.sumDays(gender, nd1, nd2);

        return sum;
    }

    /* ======================================================================================================= */

    private void outk(String what, double v)
    {
        out(what + ": " + f2s(v / 1000.0));
    }

    private void out(String what)
    {
        Util.out(what);
    }

    private String f2s(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }

    private PopulationContext newPopulationContext()
    {
        return PopulationContext.newTotalPopulationContext(ValueConstraint.NONE);
    }

    private void merge(PopulationContext from, PopulationContext to) throws Exception
    {
        merge(from, to, Gender.MALE);
        merge(from, to, Gender.FEMALE);
    }

    private void merge(PopulationContext from, PopulationContext to, Gender gender) throws Exception
    {
        for (int nd = 0; nd <= from.MAX_DAY; nd++)
        {
            double v1 = from.getDay(Locality.TOTAL, gender, nd);

            if (v1 != 0)
            {
                double v2 = to.getDay(Locality.TOTAL, gender, nd);
                if (v2 != 0)
                    throw new Exception("unable to merge: already has data for this age");
                to.setDay(Locality.TOTAL, gender, nd, v1);
            }
        }
    }

    private void add(PopulationContext from, PopulationContext to) throws Exception
    {
        add(from, to, Gender.MALE);
        add(from, to, Gender.FEMALE);
    }

    private void add(PopulationContext from, PopulationContext to, Gender gender) throws Exception
    {
        for (int nd = 0; nd <= from.MAX_DAY; nd++)
        {
            double v = from.getDay(Locality.TOTAL, gender, nd);
            to.addDay(Locality.TOTAL, gender, nd, v);
        }
    }

    /* ======================================================================================================= */

    /*
     * Построить кривую l(x) для таблицы смертности mt, указаного типа местности и пола
     */
    private double[] mt2lx(final CombinedMortalityTable mt, final Locality locality, final Gender gender) throws Exception
    {
        double[] yearly_lx = mt.getSingleTable(locality, gender).lx();

        /*
         * Провести дневную кривую так что
         *       daily_lx[0]         = yearly_lx[0]
         *       daily_lx[365]       = yearly_lx[1]
         *       daily_lx[365 * 2]   = yearly_lx[2]
         *       etc.
         */
        double[] daily_lx = InterpolateYearlyToDailyAsValuePreservingMonotoneCurve.yearly2daily(yearly_lx);

        /*
         * Базовая проверка правильности
         */
        if (Util.differ(daily_lx[0], yearly_lx[0]) ||
            Util.differ(daily_lx[365 * 1], yearly_lx[1]) ||
            Util.differ(daily_lx[365 * 2], yearly_lx[2]) ||
            Util.differ(daily_lx[365 * 3], yearly_lx[3]))
        {
            throw new Exception("Ошибка в построении daily_lx");
        }

        Util.assertion(Util.isMonotonicallyDecreasing(daily_lx, true));

        return daily_lx;
    }

    /* ======================================================================================================= */

    /*
     * Составить половозрастную стркутуру всех смертей по возрасту в момент смерти
     */
    private PopulationContext allExcessDeathsByDeathAge() throws Exception
    {
        PopulationContext p = newPopulationContext();

        for (HalfYearEntry he : halves)
        {
            if (he.index().equals("1941.1") || he.index().equals("1946.1"))
                continue;

            p = p.add(he.actual_excess_wartime_deaths, ValueConstraint.NONE);
        }

        return p;
    }

    /*
     * Составить половозрастную стркутуру всех смертей по возрасту на начало 1946 года
     */
    private PopulationContext allExcessDeathsByAgeAt1946() throws Exception
    {
        PopulationContext p = newPopulationContext();

        for (HalfYearEntry he : halves)
        {
            if (he.index().equals("1941.1") || he.index().equals("1946.1"))
                continue;

            double offset = (1946 + 0) - (he.year + 0.5 * he.halfyear.seq(0));

            PopulationContext up = he.actual_excess_wartime_deaths.moveUpPreserving(offset);
            p = p.add(up, ValueConstraint.NONE);
        }

        return p;
    }
}