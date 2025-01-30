package rtss.ww2losses;

import rtss.data.ValueConstraint;
import rtss.data.asfr.AgeSpecificFertilityRatesByTimepoint;
import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.asfr.InterpolateASFR;
import rtss.data.curves.InterpolateYearlyToDailyAsValuePreservingMonotoneCurve;
import rtss.data.mortality.CombinedMortalityTable;
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
import rtss.ww2losses.ageline.AgeLineLossIntensities;
import rtss.ww2losses.ageline.EvalAgeLineLossIntensities;
import rtss.ww2losses.helpers.PeacetimeMortalityTables;
import rtss.ww2losses.helpers.ShowForecast;
import rtss.ww2losses.helpers.WarHelpers;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population_194x.MortalityTable_1940;
import rtss.ww2losses.population_194x.Population_In_Middle_1941;
import rtss.ww2losses.util.CalibrateASFR;

import static rtss.data.population.forward.ForwardPopulation.years2days;

public class Main
{
    public static void main(String[] args)
    {
        try
        {
            new Main(Area.USSR).main();
            new Main(Area.RSFSR).main();
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

    /*
     * Корректировать младенческую и раннедетскую смертность в таблицах смертности
     * 1943-1945 гг. с учётом эффекта антибиотиков 
     */
    private static boolean ApplyAntibiotics = Util.True;

    /*
     * Распечатывать диагностический вывод
     */
    private static boolean PrintDiagnostics = Util.True;

    /*
     * Использовать если население на начало 1946 года уже не содержит эмигрантов
     */
    private static boolean DeductEmigration = Util.False;

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
     * данные для полугодий начиная с середины 1941 и по начало 1946 года
     */
    private HalfYearEntries<HalfYearEntry> halves;

    private void main() throws Exception
    {
        Util.out("");
        Util.out("**********************************************************************************");
        Util.out("Вычисление для " + area.toString());
        Util.out("");

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

        /* таблицы смертности для 1940 года и полугодий 1941-1945 при мирных условиях */
        mt1940 = new MortalityTable_1940(ap).evaluate();
        peacetimeMortalityTables = new PeacetimeMortalityTables(mt1940, ApplyAntibiotics);

        /* население на середину 1941 года */
        Population_In_Middle_1941 pm1941 = new Population_In_Middle_1941(ap);
        PopulationContext fctx_mid1941 = new PopulationContext(PopulationContextSize);
        Population p = pm1941.evaluateAsPopulation(fctx_mid1941, mt1940);
        Util.assertion(p.sum() == 0);

        if (Util.False)
        {
            new PopulationChart("Население " + area + " на середину 1941 года")
                    .show("перепись", fctx_mid1941.toPopulation())
                    .display();
        }

        /* передвижка для мирных условий */
        halves = evalHalves_step_6mo(pm1941, fctx_mid1941);

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

        evalDeficit1946();
        evalAgeLines();
        evalNewBirths();
        evalNewBirthsDeaths();
        // ### fitNewBirthsDeaths();
        // ### print half-yearly
        // ### print yearly
        // ### save files: population structure, excess deaths
    }

    /* ================================================================================== */

    /*
     * Подготовить полугодовые сегменты.
     * Передвижка с шагом полгода.
     */
    @SuppressWarnings("unused")
    private HalfYearEntries<HalfYearEntry> evalHalves_step_6mo(Population_In_Middle_1941 pm1941, PopulationContext fctx_mid1941) throws Exception
    {
        HalfYearEntries<HalfYearEntry> halves = new HalfYearEntries<HalfYearEntry>();
        PopulationContext fctx = fctx_mid1941.clone();

        HalfYearEntry curr, prev;
        int year = 1941;

        /* первое полугодие 1941 */
        HalfYearSelector half = HalfYearSelector.FirstHalfYear;
        prev = curr = new HalfYearEntry(year, half,
                                        pm1941.p_start_1941.forLocality(Locality.TOTAL).toPopulationContext(),
                                        pm1941.p_start_1941.forLocality(Locality.TOTAL).toPopulationContext());
        curr.expected_nonwar_deaths = pm1941.observed_deaths_1941_1st_halfyear;
        curr.expected_nonwar_births = pm1941.observed_births_1941_1st_halfyear;
        halves.add(curr);

        /* второе полугодие 1941 */
        half = HalfYearSelector.SecondHalfYear;
        curr = new HalfYearEntry(year, half, fctx.clone(), fctx.clone());
        prev.next = curr;
        curr.prev = prev;
        prev = curr;
        halves.add(curr);

        /* подготовиться к передвижке населения с учётом рождений после середины 1941 года */
        PopulationContext pwb = fctx.clone();

        /* подготовиться к передвижке населения без учёта рождений после середины 1941 года (только наличного на середину 1941 года) */
        PopulationContext pxb = fctx.clone();

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

            /* передвижка на следующие полгода населения без учёта рождений */
            ForwardPopulationT fw2 = new ForwardPopulationT();
            fw2.setBirthRateTotal(0);
            fw2.forward(pxb, mt, 0.5);

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

    private void evalDeficit1946() throws Exception
    {
        PopulationContext p1946_expected_with_births = halves.last().p_nonwar_with_births;
        PopulationContext p1946_expected_without_births = halves.last().p_nonwar_without_births;
        PopulationContext p1946_expected_newonly = p1946_expected_with_births.sub(p1946_expected_without_births);
        PopulationContext p1941_mid = halves.get(1941, HalfYearSelector.SecondHalfYear).p_nonwar_without_births;

        /*
         * проверить, что сумма expected_nonwar_deaths примерно равна разнице численностей
         * p_nonwar_without_births
         */
        double v_sum = 0;
        for (HalfYearEntry curr : halves)
        {
            if (curr.year == 1941 && curr.halfyear == HalfYearSelector.FirstHalfYear)
                continue;
            if (curr.year == 1946)
                continue;

            v_sum += curr.expected_nonwar_deaths;
        }

        double v = p1941_mid.sum();
        v -= p1946_expected_without_births.sum();
        if (Util.differ(v_sum, v, 0.0001))
            Util.err("Несовпадение числа смертей");

        /* =================================================== */

        PopulationContext deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar, ValueConstraint.NONE);

        if (Util.False)
        {
            new PopulationChart("Дефицит " + ap.area)
                    .show("дефицит", deficit)
                    .display();
        }

        if (area == Area.RSFSR)
        {
            if (PrintDiagnostics)
                WarHelpers.validateDeficit(deficit, "До эмиграции и отмены отрицательных женских значений:");

            /*
             * Для РСФСР отменить отрицательные значения дефицита женского населения
             * в возрастах 15-60 лет как вызванные вероятно миграцией
             */
            cancelNegativeDeficit(Gender.FEMALE, 15, 60);
            deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar, ValueConstraint.NONE);
        }

        v = p1946_expected_with_births.sum();
        v -= p1946_actual.sum();
        double v_total = v;
        outk("Общий дефицит населения к январю 1946, тыс. чел.", v);

        v = p1946_expected_without_births.sum();
        v -= p1946_actual_born_prewar.sum();
        double v_prewar = v;
        outk("Дефицит наличного в начале войны населения к январю 1946, тыс. чел.", v);

        v = p1946_expected_newonly.sum();
        v -= p1946_actual_born_postwar.sum();
        double v_postwar = v;
        outk("Дефицит рождённного во время войны населения к январю 1946, тыс. чел.", v);

        if (Util.differ(v_total, v_prewar + v_postwar))
            Util.err("Расхождение категорий дефицита");

        if (PrintDiagnostics)
        {
            ShowForecast.show(ap, p1946_actual, halves, 3);
            ShowForecast.show(ap, p1946_actual, halves, 4);
        }

        /* оставить только сверхсмертность */
        if (DeductEmigration)
        {
            PopulationContext emigration = emigration();
            out("");
            outk("Эмиграция, тыс. чел.", emigration.sum());
            deficit = deficit.sub(emigration, ValueConstraint.NONE);
        }
        else
        {
            out("");
            out("Эмиграция ещё включена в половозрастную структуру начала 1946 года, и не вычитается из смертности");
        }

        if (area == Area.RSFSR)
            deficit = cancelNegativeDeficit(deficit, Gender.FEMALE, 15, 60);

        if (PrintDiagnostics)
        {
            if (area == Area.RSFSR)
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

        out("");
        outk("Предварительная сверхсмертность всего наличного на середину 1941 года населения к концу 1945 года, по дефициту", deficit_total);
        out("Предварительная разбивка на категории по временному окну:");
        outk("    Сверхсмертность [по дефициту] мужчин призывного возраста", deficit_m_conscripts);
        outk("    Сверхсмертность [по дефициту] женщин фертильного возраста", deficit_f_fertile);
        outk("    Сверхсмертность [по дефициту] остального наличного на середину 1941 года населения", deficit_other);
    }

    /* ======================================================================================================= */

    private void cancelNegativeDeficit(Gender gender, int age1, int age2) throws Exception
    {
        PopulationContext p1946_expected_without_births = halves.last().p_nonwar_without_births;
        PopulationContext deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar, ValueConstraint.NONE);

        for (int age = age1; age <= age2; age++)
        {
            double v = deficit.getYearValue(gender, age);
            if (v < 0)
            {
                // p1946_actual.addYearValue(gender, age, v);
                unneg(p1946_actual, gender, age, deficit);
            }
        }

        p1946_actual.makeBoth();
        p1946_actual.recalcTotal();
        split_p1946();
    }

    private PopulationContext cancelNegativeDeficit(PopulationContext deficit, Gender gender, int age1, int age2) throws Exception
    {
        deficit.setValueConstraint(ValueConstraint.NONE);

        for (int age = age1; age <= age2; age++)
        {
            double v = deficit.getYearValue(gender, age);
            if (v < 0)
            {
                // p1946_actual.addYearValue(gender, age, v);
                unneg(p1946_actual, gender, age, deficit);
                deficit.setYearValue(gender, age, 0);
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

    private void evalAgeLines() throws Exception
    {
        /* полугодовой коэффициент распределения потерь для не-призывного населения */
        double[] ac_general = wsum(0.6, even_intensity, 0.4, occupation_intensity);

        /* полугодовой коэффициент распределения потерь для призывного населения */
        double[] ac_conscripts = wsum(0.9, rkka_loss_intensity, 0.1, ac_general);

        /* вычислить коэфициенты интенсивности военных потерь для каждого возраста и пола */
        EvalAgeLineLossIntensities eval = new EvalAgeLineLossIntensities(halves, ac_general, ac_conscripts);
        AgeLineLossIntensities alis = eval.eval(p1946_actual);

        /* расчёт возрастных линий с учётов найденных коэфициентов интенсивности */
        eval.processAgeLines(alis, p1946_actual);

        /* compare halves.last.actual_population vs. p1946_actual_born_prewar */
        PopulationContext diff = p1946_actual_born_prewar.sub(halves.last().actual_population, ValueConstraint.NONE);
        Util.assertion(Math.abs(diff.sum(0, MAX_AGE - 1)) < 100);
        Util.assertion(Math.abs(diff.getYearValue(Gender.BOTH, MAX_AGE)) < 1200);

        HalfYearEntry he = halves.get("1941.1");
        he.actual_population = he.p_nonwar_with_births;
        he.actual_deaths = null;
        he.actual_peace_deaths = null;
        he.actual_excess_wartime_deaths = null;

        he = halves.get("1941.2");
        he.actual_population = he.p_nonwar_with_births;

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

        outk("Избыточное число всех смертей", sum_all);
        outk(String.format("Избыточное число смертей мужчин призывного возраста (%.1f-%.1f лет)",
                           Constants.CONSCRIPT_AGE_FROM,
                           Constants.CONSCRIPT_AGE_TO),
             sum_conscripts);
    }

    /* ======================================================================================================= */

    /*
     * Вычислить фактическое число рождений в военное время
     */
    private void evalNewBirths() throws Exception
    {
        Util.out(String.format("Калибровочная поправка ASFR: %.3f", asfr_calibration));

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
    
    /*
     * Вычислить ожидаемое число смертей от новых рождений в военное время (от фактического числа военных рождений),
     * ожидаемый остаток фактически рождённых в 1941.вт.пол. - 1945.вт.пол. к началу 1946 при детской смертности 
     * мирных условий, и дефицит остатка на начало 1946 года из-за возросшей в военное время детской смертности. 
     */
    private void evalNewBirthsDeaths() throws Exception
    {
        /*
         * передвижка новрождаемого населения по полугодиям
         * от середины 1941 с p = empty
         * и добавлением числа рождений за полугодие согласно he.actual_births
         */
        PopulationContext p = newPopulationContext();

        HalfYearEntry he = halves.get("1941.2");
        for (;;)
        {
            if (he.year == 1946)
                break;

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

            // число смертей от рождений
            he.actual_warborn_deaths_baseline = fw.getObservedDeaths();

            he = he.next;
        }

        double v1 = p.sum();
        double v2 = p1946_actual_born_postwar.sum();

        outk("Сверхсмертность рождённых во время войны, по дефициту к началу 1946 года, тыс. чел.", v1 - v2);
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
        out(what + ": " + f2k(v / 1000.0));
    }

    private void out(String what)
    {
        Util.out(what);
    }

    private String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }

    private PopulationContext newPopulationContext()
    {
        PopulationContext p = new PopulationContext(PopulationContext.ALL_AGES);
        p.setValueConstraint(ValueConstraint.NONE);
        p.beginTotal();
        return p;
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

        return daily_lx;
    }
}
