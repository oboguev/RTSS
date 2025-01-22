package rtss.ww2losses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.RescalePopulation;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.FieldValue;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;
import rtss.ww2losses.helpers.ShowForecast;
import rtss.ww2losses.helpers.WarHelpers;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population_194x.MortalityTable_1940;
import rtss.ww2losses.population_194x.Population_In_Middle_1941;
import rtss.ww2losses.util.CalibrateASFR;
import rtss.ww2losses.util.HalfYearEntries;
import rtss.ww2losses.util.HalfYearEntries.HalfYearSelector;

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
        this.p1946_actual = PopulationADH.getPopulationByLocality(ap.area, 1946);
        adjustForTuva();
        split_p1946();
    }

    /*
     * Корректировать младенческую и раннедетскую смертность в таблицах смертности
     * 1943-1945 гг. с учётом эффекта антибиотиков 
     */
    private static boolean AppyAntibiotics = Util.True;

    /*
     * Распечатывать диагностический вывод
     */
    private static boolean PrintDiagnostics = Util.False;

    private Area area;
    private AreaParameters ap;
    private static int MAX_AGE = Population.MAX_AGE;

    /* фактическое население на начало 1946 года */
    private PopulationByLocality p1946_actual;

    /* фактическое население на начало 1946 года рождённое до середины 1941*/
    private PopulationByLocality p1946_actual_born_prewar;

    /* фактическое население на начало 1946 года рождённое после середины 1941*/
    private PopulationByLocality p1946_actual_born_postwar;

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

    private AgeSpecificFertilityRatesByYear asfrs;
    private double asfr_calibration;

    /*
     * данные для полугодий начиная с середины 1941 и по начало 1946 года
     */
    HalfYearEntries<HalfYearEntry> halves;

    private void main() throws Exception
    {
        Util.out("");
        Util.out("**********************************************************************************");
        Util.out("Вычисление для " + area.toString());
        Util.out("");

        switch (area)
        {
        case USSR:
            asfrs = AgeSpecificFertilityRatesByYear.load("age_specific_fertility_rates/USSR/USSR-ASFR.xlsx");
            break;

        case RSFSR:
            asfrs = AgeSpecificFertilityRatesByYear.load("age_specific_fertility_rates/survey-1960.xlsx");
            break;
        }

        asfr_calibration = CalibrateASFR.calibrate1940(ap, asfrs);

        /* таблица смертности для 1940 года */
        CombinedMortalityTable mt1940 = new MortalityTable_1940(ap).evaluate();

        HalfYearEntries<HalfYearEntry> halves1 = evalHalves_step_1yr(mt1940);
        HalfYearEntries<HalfYearEntry> halves2 = evalHalves_step_6mo(mt1940);

        if (Util.False)
        {
            /*
             * Сравнение для ожидаемого населения
             */
            String point = "1946.1";
            new PopulationChart("Сравнение вариантов ожидаемого населения на " + point)
                    .show("1", halves1.get(point).p_nonwar_without_births.forLocality(Locality.TOTAL))
                    .show("2", halves2.get(point).p_nonwar_without_births.forLocality(Locality.TOTAL))
                    .display();

            if (Util.False)
            {
                new PopulationChart("Вариант 1 ожидаемого населения на " + point)
                        .show("1", halves1.get(point).p_nonwar_without_births.forLocality(Locality.TOTAL))
                        .display();

                new PopulationChart("Вариант 2 ожидаемого населения на " + point)
                        .show("2", halves2.get(point).p_nonwar_without_births.forLocality(Locality.TOTAL))
                        .display();

            }
            Util.noop();
        }

        if (Util.False)
        {
            /*
             * Сравнение для дефицитов
             */
            new PopulationChart("Сравнение вариантов дефицита населения на 1946.1")
                    .show("1", eval_deficit_1946(halves1).forLocality(Locality.TOTAL))
                    .show("2", eval_deficit_1946(halves2).forLocality(Locality.TOTAL))
                    .display();

            if (Util.False)
            {
                new PopulationChart("Вариант 1 дефицита населения на 1946.1")
                        .show("1", eval_deficit_1946(halves1).forLocality(Locality.TOTAL))
                        .display();

                new PopulationChart("Вариант 2 дефицита населения на 1946.1")
                        .show("2", eval_deficit_1946(halves2).forLocality(Locality.TOTAL))
                        .display();
            }

            Util.noop();
        }

        halves = halves2;
        evalDeficit1946();
        evalBirths();

        Util.noop();
    }

    /* ================================================================================== */

    /*
     * Подготовить полугодовые сегменты.
     * Вариант 1:
     * Основная передвижка с шагом год.
     * Вторичная передвижка (для промежуточных точек и до 1946) с шагом полгода. 
     */
    private HalfYearEntries<HalfYearEntry> evalHalves_step_1yr(CombinedMortalityTable mt1940) throws Exception
    {
        HalfYearEntries<HalfYearEntry> halves = createHalves();

        /* население на середину 1941 года */
        Population_In_Middle_1941 pm1941 = new Population_In_Middle_1941(ap);
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        PopulationByLocality p = pm1941.evaluate(fctx, mt1940);

        /* первое полугодие 1941 */
        HalfYearEntry curr = halves.get(1941, HalfYearSelector.FirstHalfYear);
        curr.p_nonwar_with_births = pm1941.p_start_1941;
        curr.p_nonwar_without_births = pm1941.p_start_1941;
        curr.expected_nonwar_deaths = pm1941.observed_deaths_1941_1st_halfyear;
        curr.expected_nonwar_births = pm1941.observed_births_1941_1st_halfyear;

        /* второе полугодие 1941 */
        curr = halves.get(1941, HalfYearSelector.SecondHalfYear);
        curr.p_nonwar_with_births = fctx.end(p);
        curr.p_nonwar_without_births = fctx.end(p);

        /* подготовиться к передвижке населения с учётом рождений после середины 1941 года */
        PopulationByLocality pwb = p.clone();

        /* подготовиться к передвижке населения без учёта рождений после середины 1941 года (только наличного на середину 1941 года) */
        PopulationByLocality pxb = p.clone();
        PopulationForwardingContext fctx_xb = fctx.clone();

        curr.save_fw(pwb, fctx, pxb, fctx_xb);

        /* 
         * годовой шаг от вторых половин года (1942-1945)
         *     от 1941.2 -> 1942.2 -> 1943.2 -> 1944.2 -> 1945.2
         */
        for (int year = 1942; year <= 1945; year++)
        {
            curr = halves.get(year, HalfYearSelector.SecondHalfYear);
            HalfYearEntry prev2 = curr.prev.prev;

            /* определить таблицу смертности, с учётом падения детской смертности из-за введения антибиотиков */
            CombinedMortalityTable mt = cross_year_mt(mt1940, year - 1);

            /* передвижка на следующий год населения с учётом рождений */
            ForwardPopulationT fw1 = new ForwardPopulationT();
            fw1.setBirthRateTotal(ap.CBR_1940);
            pwb = fw1.forward(pwb, fctx, mt, 1.0);

            /* передвижка на следующий год населения без учёта рождений */
            ForwardPopulationT fw2 = new ForwardPopulationT();
            fw2.setBirthRateTotal(0);
            pxb = fw2.forward(pxb, fctx_xb, mt, 1.0);

            curr.save_fw(pwb, fctx, pxb, fctx_xb);

            curr.p_nonwar_with_births = fctx.end(pwb);
            curr.p_nonwar_without_births = fctx_xb.end(pxb);

            /* за год от prev2 */
            prev2.expected_nonwar_births = fw1.getObservedBirths();
            prev2.expected_nonwar_deaths = fw2.getObservedDeaths();
        }

        /* 
         * полугодовой шаг для вторых половин года (1942-1946),
         * от середины года к началу следующего
         *     1941.2 -> 1942.1 
         *     1942.2 -> 1943.1 
         *     1943.2 -> 1944.1 
         *     1944.2 -> 1945.1 
         *     1945.2 -> 1946.1 
         */
        for (int year = 1942; year <= 1946; year++)
        {
            curr = halves.get(year, HalfYearSelector.FirstHalfYear);
            HalfYearEntry prev = curr.prev;

            /* определить таблицу смертности, с учётом падения детской смертности из-за введения антибиотиков */
            CombinedMortalityTable mt = year_mt(mt1940, year - 1);

            /* передвижка на следующие полгода населения с учётом рождений */
            pwb = prev.fw_p_wb;
            fctx = prev.fw_fctx_wb;
            ForwardPopulationT fw1 = new ForwardPopulationT();
            fw1.setBirthRateTotal(ap.CBR_1940);
            pwb = fw1.forward(pwb, fctx, mt, 0.5);

            /* передвижка на следующие полгода населения без учёта рождений */
            pxb = prev.fw_p_xb;
            fctx_xb = prev.fw_fctx_xb;
            ForwardPopulationT fw2 = new ForwardPopulationT();
            fw2.setBirthRateTotal(0);
            pxb = fw2.forward(pxb, fctx_xb, mt, 0.5);

            curr.save_fw(pwb, fctx, pxb, fctx_xb);

            curr.p_nonwar_with_births = fctx.end(pwb);
            curr.p_nonwar_without_births = fctx_xb.end(pxb);

            /* в prev ведичины за год, включая текущее полугодие и предшествующее полугодие */
            curr.expected_nonwar_births = prev.expected_nonwar_births - fw1.getObservedBirths();
            curr.expected_nonwar_deaths = prev.expected_nonwar_deaths - fw2.getObservedDeaths();

            prev.expected_nonwar_births = fw1.getObservedBirths();
            prev.expected_nonwar_deaths = fw2.getObservedDeaths();
        }

        return halves;
    }

    private HalfYearEntries<HalfYearEntry> createHalves()
    {
        HalfYearEntries<HalfYearEntry> halves = new HalfYearEntries<HalfYearEntry>();

        HalfYearEntry curr, prev = null;
        int year = 1941;
        HalfYearSelector half = HalfYearSelector.FirstHalfYear;

        for (;;)
        {
            curr = new HalfYearEntry(year, half, null, null);
            halves.add(curr);
            curr.prev = prev;
            if (prev != null)
                prev.next = curr;
            prev = curr;

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
        }

        return halves;
    }

    /* ================================================================================== */

    /*
     * Подготовить полугодовые сегменты.
     * Вариант 2:
     * Передвижка с шагом полгода.
     */
    @SuppressWarnings("unused")
    private HalfYearEntries<HalfYearEntry> evalHalves_step_6mo(CombinedMortalityTable mt1940) throws Exception
    {
        HalfYearEntries<HalfYearEntry> halves = new HalfYearEntries<HalfYearEntry>();

        /* население на середину 1941 года */
        Population_In_Middle_1941 pm1941 = new Population_In_Middle_1941(ap);
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        PopulationByLocality p = pm1941.evaluate(fctx, mt1940);
        PopulationByLocality px = fctx.end(p);
        if (Util.False)
        {
            new PopulationChart("Население на середину 1941 года")
                    .show("перепись", px.forLocality(Locality.TOTAL))
                    .display();
        }

        HalfYearEntry curr, prev;
        int year = 1941;

        /* первое полугодие 1941 */
        HalfYearSelector half = HalfYearSelector.FirstHalfYear;
        prev = curr = new HalfYearEntry(year, half, pm1941.p_start_1941, pm1941.p_start_1941);
        curr.expected_nonwar_deaths = pm1941.observed_deaths_1941_1st_halfyear;
        curr.expected_nonwar_births = pm1941.observed_births_1941_1st_halfyear;
        halves.add(curr);

        /* второе полугодие 1941 */
        half = HalfYearSelector.SecondHalfYear;
        curr = new HalfYearEntry(year, half, px, px);
        prev.next = curr;
        curr.prev = prev;
        prev = curr;
        halves.add(curr);

        /* подготовиться к передвижке населения с учётом рождений после середины 1941 года */
        PopulationByLocality pwb = p.clone();

        /* подготовиться к передвижке населения без учёта рождений после середины 1941 года (только наличного на середину 1941 года) */
        PopulationByLocality pxb = p.clone();
        PopulationForwardingContext fctx_xb = fctx.clone();

        /* продвигать с шагом по полгода до января 1946 */
        for (;;)
        {
            int current_year = year;

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
            CombinedMortalityTable mt = year_mt(mt1940, current_year);

            /* передвижка на следующие полгода населения с учётом рождений */
            ForwardPopulationT fw1 = new ForwardPopulationT();
            fw1.setBirthRateTotal(ap.CBR_1940);
            pwb = fw1.forward(pwb, fctx, mt, 0.5);

            /* передвижка на следующие полгода населения без учёта рождений */
            ForwardPopulationT fw2 = new ForwardPopulationT();
            fw2.setBirthRateTotal(0);
            pxb = fw2.forward(pxb, fctx_xb, mt, 0.5);

            /* сохранить результаты в полугодовой записи */
            curr = new HalfYearEntry(year, half, fctx.end(pwb), fctx_xb.end(pxb));
            prev.expected_nonwar_births = fw1.getObservedBirths();
            prev.expected_nonwar_deaths = fw2.getObservedDeaths();

            curr.prev = prev;
            prev.next = curr;
            prev = curr;
            halves.add(curr);
        }

        return halves;
    }

    /* 
     * Определить таблицу смертности для года @year 
     * с учётом падения детской смертности из-за введения антибиотиков 
     */
    private CombinedMortalityTable year_mt(CombinedMortalityTable mt1940, int year) throws Exception
    {
        if (!AppyAntibiotics)
            return mt1940;

        double scale0 = imr_scale(year);
        if (!Util.differ(scale0, 1.0))
            return mt1940;

        PatchInstruction instruction = new PatchInstruction(PatchOpcode.MultiplyWithDecay, 0, 5, scale0, 1.0);
        List<PatchInstruction> instructions = new ArrayList<>();
        instructions.add(instruction);

        CombinedMortalityTable xmt = PatchMortalityTable.patch(mt1940, instructions, "поправка антибиотиков для " + year);

        return xmt;
    }

    /* 
     * Определить таблицу смертности для года из двух полугодий (2-е полугодие year, 1-е полугодие year + 1) 
     * с учётом падения детской смертности из-за введения антибиотиков 
     */
    private CombinedMortalityTable cross_year_mt(CombinedMortalityTable mt1940, int year) throws Exception
    {
        if (!AppyAntibiotics)
            return mt1940;

        double scale0 = (imr_scale(year) + imr_scale(year + 1)) / 2;
        if (!Util.differ(scale0, 1.0))
            return mt1940;

        PatchInstruction instruction = new PatchInstruction(PatchOpcode.MultiplyWithDecay, 0, 5, scale0, 1.0);
        List<PatchInstruction> instructions = new ArrayList<>();
        instructions.add(instruction);

        CombinedMortalityTable xmt = PatchMortalityTable.patch(mt1940, instructions, "поправка антибиотиков для " + year);

        return xmt;
    }

    /*
     * Младенческая смертность относительно 1940 года
     */
    private double imr_scale(int year) throws Exception
    {
        switch (year)
        {
        case 1940:
        case 1941:
        case 1942:
            return 1.0;

        case 1943:
            return 0.76;

        case 1944:
            return 0.53;

        case 1945:
            return 0.45;

        default:
            throw new IllegalArgumentException();
        }
    }

    /* ======================================================================================================= */

    private void evalDeficit1946() throws Exception
    {
        PopulationByLocality p1946_expected_with_births = halves.last().p_nonwar_with_births;
        PopulationByLocality p1946_expected_without_births = halves.last().p_nonwar_without_births;
        PopulationByLocality p1946_expected_newonly = p1946_expected_with_births.sub(p1946_expected_without_births);
        PopulationByLocality p1941_mid = halves.get(1941, HalfYearSelector.SecondHalfYear).p_nonwar_without_births;

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

        double v = p1941_mid.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        v -= p1946_expected_without_births.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        if (Util.differ(v_sum, v))
            Util.err("Несовпадение числа смертей");

        /* =================================================== */

        if (area == Area.RSFSR)
        {
            /*
             * Для РСФСР отменить отрицательные значения дефицита женского населения
             * в возрастах 15-60 лет как вызванные вероятно миграцией
             */
            cancelNegativeDeficit(Gender.FEMALE, 15, 60);
        }

        v = p1946_expected_with_births.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        v -= p1946_actual.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        Util.out("Общий дефицит населения к январю 1946, тыс. чел.: " + f2k(v / 1000.0));

        v = p1946_expected_without_births.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        v -= p1946_actual_born_prewar.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        Util.out("Дефицит наличного в начале войны населения к январю 1946, тыс. чел.: " + f2k(v / 1000.0));

        v = p1946_expected_newonly.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        v -= p1946_actual_born_postwar.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        Util.out("Дефицит рождённного во время войны населения к январю 1946, тыс. чел.: " + f2k(v / 1000.0));

        PopulationByLocality deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar);

        if (Util.False)
        {
            new PopulationChart("Дефицит " + ap.area)
                    .show("дефицит", deficit.forLocality(Locality.TOTAL))
                    .display();
        }

        if (PrintDiagnostics)
        {
            ShowForecast.show(ap, p1946_actual, halves, 3);
            ShowForecast.show(ap, p1946_actual, halves, 4);
        }

        // deficit.validate();

        backpropagateExistingDeficit(deficit);

        deficit = deficit.sub(emigration());
        // validate(deficit);

        if (PrintDiagnostics)
            WarHelpers.validateDeficit(deficit);

        /*
         * разбить сверхсмертность на категории 
         */
        double deficit_total = deficit.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        double deficit_m_conscripts = subcount(deficit, Gender.MALE, 19, 59);
        double deficit_f_fertile = subcount(deficit, Gender.FEMALE, 15, 58);
        double deficit_other = deficit_total - deficit_m_conscripts - deficit_f_fertile;

        Util.out("");
        Util.out("Сверхсмертность всего наличного на середину 1941 года населения: " + f2k(deficit_total / 1000.0));
        Util.out("Сверхсмертность мужчин призывного возраста: " + f2k(deficit_m_conscripts / 1000.0));
        Util.out("Сверхсмертность женщин фертильного возраста: " + f2k(deficit_f_fertile / 1000.0));
        Util.out("Сверхсмертность остального наличного на середину 1941 года населения: " + f2k(deficit_other / 1000.0));

        /*
         * распределить объём сверхсмертности по полугодиям 
         */
        scatter("excess_war_deaths_fertile_f", occupation_intensity, deficit_f_fertile * 0.7);
        scatter("excess_war_deaths_fertile_f", deficit_f_fertile * 0.3);

        scatter("excess_war_deaths", occupation_intensity, deficit_f_fertile * 0.7);
        scatter("excess_war_deaths", deficit_f_fertile * 0.3);

        scatter("excess_war_deaths", occupation_intensity, deficit_other * 0.7);
        scatter("excess_war_deaths", deficit_other * 0.3);

        scatter("excess_war_deaths", rkka_loss_intensity, deficit_m_conscripts * 0.7);
        scatter("excess_war_deaths", occupation_intensity, (deficit_m_conscripts * 0.3) * 0.7);
        scatter("excess_war_deaths", (deficit_m_conscripts * 0.3) * 0.3);

        /*
         * проверка 
         */
        double sum = 0;
        for (HalfYearEntry he : halves)
            sum += he.excess_war_deaths;
        if (Util.differ(deficit_total, sum))
            throw new Exception("ошибка распределения deficit_total");

        sum = 0;
        for (HalfYearEntry he : halves)
            sum += he.excess_war_deaths_fertile_f;
        if (Util.differ(deficit_f_fertile, sum))
            throw new Exception("ошибка распределения deficit_f_fertile");

        Util.noop();
    }

    /*
     * Вычислить половозрастную структуру дефицита населения на 1946.1 для графического изображения.
     * Возвращаемый дефицит охватывает только наличное на начало войны население, без учёта рождений
     * во время войны.  
     */
    private PopulationByLocality eval_deficit_1946(HalfYearEntries<HalfYearEntry> halves) throws Exception
    {
        PopulationByLocality p1946_expected_without_births = halves.last().p_nonwar_without_births;
        PopulationByLocality deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar);
        deficit = deficit.sub(emigration());
        return deficit;
    }

    private void split_p1946() throws Exception
    {
        p1946_actual_born_postwar = p1946_actual.selectByAge(0, 4.5);
        p1946_actual_born_prewar = p1946_actual.selectByAge(4.5, MAX_AGE + 1);
    }

    private PopulationByLocality emigration() throws Exception
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

        PopulationByLocality p = PopulationByLocality.newPopulationTotalOnly();
        for (int age = 0; age <= MAX_AGE; age++)
        {
            p.set(Locality.TOTAL, Gender.MALE, age, 0);
            p.set(Locality.TOTAL, Gender.FEMALE, age, 0);
        }

        for (int age = 20; age <= 60; age++)
        {
            p.set(Locality.TOTAL, Gender.MALE, age, 0.8);
            p.set(Locality.TOTAL, Gender.FEMALE, age, 0.2);
        }

        p.makeBoth(Locality.TOTAL);

        p = RescalePopulation.scaleAllTo(p, emig);

        return p;
    }

    /* вычесть население Тувы из населения начала 1946 года */
    private void adjustForTuva() throws Exception
    {
        final double tuva_pop = 100_000;
        double pop = p1946_actual.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE);
        double scale = (pop - tuva_pop) / pop;
        p1946_actual = RescalePopulation.scaleAllBy(p1946_actual, scale);
    }

    private void cancelNegativeDeficit(Gender gender, int age1, int age2) throws Exception
    {
        PopulationByLocality p1946_expected_without_births = halves.last().p_nonwar_without_births;
        Population deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar).forLocality(Locality.TOTAL);
        for (int age = age1; age <= age2; age++)
        {
            double v = deficit.get(gender, age);
            if (v < 0)
            {
                p1946_actual.add(Locality.TOTAL, gender, age, v);
            }
        }

        split_p1946();
    }

    private double subcount(PopulationByLocality p, Gender gender, int age1, int age2) throws Exception
    {
        double sum_wv = 0;
        double sum_weights = 0;

        for (int age = age1; age <= age2; age++)
        {
            /*
             * Окно возрастающее с обеих концов с 0.5 (полгода expsure) до 4.0 (exposure на всё время войны)
             */
            double weight = 4.0;

            switch (Math.abs(age - age1))
            {
            case 0:
                weight = 0.5;
                break;

            case 1:
                weight = 1.5;
                break;

            case 2:
                weight = 2.5;
                break;
            case 3:

                weight = 3.5;
                break;
            }

            switch (Math.abs(age2 - age))
            {
            case 0:
                weight = 0.5;
                break;

            case 1:
                weight = 1.5;
                break;

            case 2:
                weight = 2.5;
                break;
            case 3:

                weight = 3.5;
                break;
            }

            double v = p.get(Locality.TOTAL, gender, age);
            sum_weights += weight;
            sum_wv += v * weight;
        }

        sum_weights /= (age2 - age1 + 1);
        return sum_wv / sum_weights;
    }

    /* ======================================================================================================= */

    /*
     * Распределить дефицит населения от начала 1946 года
     * на начало каждого предшествуюшего полугодия, c последовательным его уменьшением 
     * соответствено полугодовым коэффициентам attrition, и с возрастным сдвигом
     */
    private void backpropagateExistingDeficit(PopulationByLocality deficit1946) throws Exception
    {
        /* полугодовой коэффициент распределения потерь для не-призывного населения */
        double[] ac_generic = wsum(0.3, even_intensity, 0.7, occupation_intensity);
        List<Double> acv_generic = atov_reverse(ac_generic);

        /* полугодовой коэффициент распределения потерь для призывного населения */
        double[] ac_conscripts = wsum(0.7, rkka_loss_intensity, 0.3, ac_generic);
        List<Double> acv_conscripts = atov_reverse(ac_conscripts);

        HalfYearEntry he = halves.last();
        he.accumulated_deficit = deficit1946;

        for (;;)
        {
            he = he.prev;
            if (he.year == 1941)
                break;

            double a_generic = acv_generic.get(0) / sum(acv_generic);
            double a_conscripts = acv_conscripts.get(0) / sum(acv_conscripts);
            acv_generic.remove(0);
            acv_conscripts.remove(0);

            /*
             * Вычислить потери в текущем полугодии
             */
            PopulationByLocality loss = he.next.accumulated_deficit.clone();

            for (int age = 0; age <= MAX_AGE; age++)
            {
                double v = loss.get(Locality.TOTAL, Gender.FEMALE, age);
                loss.set(Locality.TOTAL, Gender.FEMALE, age, v * a_generic);
            }

            for (int age = 0; age <= MAX_AGE; age++)
            {
                double v = loss.get(Locality.TOTAL, Gender.MALE, age);
                if (age >= 18 && age <= 55)
                    loss.set(Locality.TOTAL, Gender.MALE, age, v * a_conscripts);
                else
                    loss.set(Locality.TOTAL, Gender.MALE, age, v * a_generic);
            }

            /*
             * Вычислить потери на начало полугодия
             */
            PopulationByLocality x = he.next.accumulated_deficit.sub(loss);
            he.accumulated_deficit = x.moveDown(0.5);
        }

        Util.noop();
    }

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

    /*
     * Откинуть первые и последние коэффициенты (нули для 1-го полугодия 1941 и 1-го полугодия 1946 гг.),
     * остаток отсортировать в обращённом порядке, для обратного отсчёта по полугодиям от 1946 к 1941 г.
     */
    private List<Double> atov_reverse(double[] a)
    {
        List<Double> list = new ArrayList<>();
        for (double d : a)
            list.add(d);
        list.remove(0);
        list.remove(list.size() - 1);
        Collections.reverse(list);
        return list;
    }

    private double sum(List<Double> v)
    {
        double s = 0;
        for (double d : v)
            s += d;
        return s;
    }

    /* ======================================================================================================= */

    private void evalBirths() throws Exception
    {
        double cumulative_excess_war_deaths_fertile_f = 0;

        Util.out(String.format("Калибровочная поправка ASFR: %.3f", asfr_calibration));

        for (HalfYearEntry he : halves)
        {
            if (he.next == null)
                break;

            /*
             * число женщин фертильного возраста
             */
            PopulationByLocality pf = he.p_nonwar_without_births.avg(he.next.p_nonwar_without_births);
            pf = pf.selectByAge(15, 54);

            double f = pf.sum(Locality.TOTAL, Gender.FEMALE, 15, 54);
            f -= cumulative_excess_war_deaths_fertile_f;
            f -= he.excess_war_deaths_fertile_f / 2;
            cumulative_excess_war_deaths_fertile_f += he.excess_war_deaths_fertile_f;

            pf = RescalePopulation.scaleTotal(pf, 1.0, f);

            /*
             * число фактических рождений
             */
            if (he.year != 1941)
                he.actual_births = asfr_calibration * 0.5 * asfrs.getForYear(he.year).births(pf);
        }

        /*
         * Для 1941 года (оба полугодия)
         */
        HalfYearEntry he1 = halves.get(0);
        HalfYearEntry he2 = halves.get(1);
        he1.actual_births = he1.expected_nonwar_births;

        PopulationByLocality pf = he2.p_nonwar_without_births.selectByAge(15, 54);
        double year_births = asfr_calibration * asfrs.getForYear(1941).births(pf);
        he2.actual_births = year_births - he1.actual_births;

        Util.out("");
        Util.out("Дефицит числа рождений, по полугодиям:");
        double all_bd = 0;
        for (HalfYearEntry he : halves)
        {
            if (he.year == 1946)
                break;
            double bd = he.expected_nonwar_births - he.actual_births;
            Util.out(String.format("%s %s", he.toString(), f2k(bd / 1000.0)));
            all_bd += bd;
        }
        Util.out(String.format("всего %s", f2k(all_bd / 1000.0)));

        Util.out("");
        Util.out("Фактическое число рождений, по полугодиям:");
        double all_actual_births = 0;
        for (HalfYearEntry he : halves)
        {
            if (he.year == 1946)
                break;
            Util.out(String.format("%s %s", he.toString(), f2k(he.actual_births / 1000.0)));
            all_actual_births += he.actual_births;
        }
        Util.out(String.format("всего %s", f2k(all_actual_births / 1000.0)));
    }

    /* ======================================================================================================= */

    /*
     * Распределить величину по полугодиям по интенсивности
     */
    private void scatter(String field, double[] intensity, double amount) throws Exception
    {
        if (halves.size() != intensity.length + 1)
            throw new IllegalArgumentException("длины не совпадают");

        double isum = 0;
        for (double v : intensity)
            isum += v;

        int k = 0;
        for (HalfYearEntry he : halves)
        {
            if (he.year == 1946)
                break;
            scatter_add(he, field, amount * intensity[k++] / isum);
        }
    }

    /*
     * Распределить величину по военным полугодиям равномерно
     */
    private void scatter(String field, double amount) throws Exception
    {
        for (HalfYearEntry he : halves)
        {
            if (he.year == 1941 && he.halfyear == HalfYearSelector.FirstHalfYear)
            {
                continue;
            }
            else if (he.year == 1945 && he.halfyear == HalfYearSelector.SecondHalfYear)
            {
                continue;
            }
            else if (he.year == 1946)
            {
                continue;
            }
            else
            {
                scatter_add(he, field, amount / 8.0);
            }
        }
    }

    private void scatter_add(Object o, String field, double extra) throws Exception
    {
        Double dv = FieldValue.getDouble(o, field);
        if (dv == null)
            dv = 0.0;
        FieldValue.setDouble(o, field, dv + extra);
    }

    private String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
