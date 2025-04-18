package rtss.ww2losses;

import rtss.data.ValueConstraint;
import rtss.data.asfr.AgeSpecificFertilityRatesByTimepoint;
import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.asfr.InterpolateASFR;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.data.population.calc.RescalePopulation;
import rtss.data.population.projection.ForwardPopulationT;
import rtss.data.population.projection.ForwardPopulationT.NewbornDeathRegistrationAge;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.ChartXY;
import rtss.util.plot.PopulationChart;
import rtss.ww2losses.ageline.AgeLineFactorIntensities;
import rtss.ww2losses.ageline.EvalAgeLineLossIntensities;
import rtss.ww2losses.ageline.PrintAgeLine;
import rtss.ww2losses.ageline.warmodel.WarAttritionModel;
import rtss.ww2losses.ageline.warmodel.WarAttritionModelParameters;
import rtss.ww2losses.helpers.ExportResults;
import rtss.ww2losses.helpers.MiscHelper;
import rtss.ww2losses.helpers.PeacetimeMortalityTables;
import rtss.ww2losses.helpers.PopulationContextCache;
import rtss.ww2losses.helpers.PrintHalfYears;
import rtss.ww2losses.helpers.PrintYears;
import rtss.ww2losses.helpers.ShowAgeSliceDeathHistory;
import rtss.ww2losses.helpers.ShowPopulationAgeSliceHistory;
import rtss.ww2losses.helpers.SmoothBirths;
import rtss.ww2losses.helpers.VerifyHalfYears;
import rtss.ww2losses.helpers.WarHelpers;
import rtss.ww2losses.helpers.diag.DiagHelper;
import rtss.ww2losses.model.Automation;
import rtss.ww2losses.model.Model;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population1941.AdjustPopulation1941;
import rtss.ww2losses.population1941.PopulationEarly1941;
import rtss.ww2losses.population1941.PopulationMiddle1941;
import rtss.ww2losses.population1941.PopulationMiddle1941.PopulationForwardingResult1941;
import rtss.ww2losses.population1941.AdjustPopulation1941vs1946;
import rtss.ww2losses.population194x.AdjustPopulation;
import rtss.ww2losses.population194x.MortalityTable_1940;
import rtss.ww2losses.struct.HalfYearEntries;
import rtss.ww2losses.struct.HalfYearEntry;
import rtss.ww2losses.struct.HalfYearEntries.HalfYearSelector;
import rtss.ww2losses.util.CalibrateASFR;
import rtss.ww2losses.util.RebalanceASFR;
import rtss.ww2losses.util.despike.DespikeComb;
import rtss.ww2losses.util.despike.DespikeZero;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main
{
    static
    {
        PopulationADH.setFilesVersion("ADH.v1");
    }

    public static void main(String[] args)
    {
        try
        {
            if (Util.False)
            {
                // Диагностика: распечатка хода указанной половозрастной линии
                PrintAgeLine.traceAgeYear(Area.USSR, Gender.MALE, 5.0);
            }

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
        // this.p1946_actual = PopulationADH.getPopulation(ap.area, 1946).toPopulationContext();
        this.p1946_actual = PopulationContextCache.get(area, "early-1946", () -> PopulationADH.getPopulation(ap.area, 1946).toPopulationContext());
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

    private Area area;
    private AreaParameters ap;
    private static int MAX_AGE = Population.MAX_AGE;
    public static final int DAYS_PER_YEAR = 365;

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

    /* весовые коэффциенты для факторов распределяющих потери по категориям */
    WarAttritionModelParameters wamp = new WarAttritionModelParameters()
            // доля потерь мужчин призывного возраста связанная с интенсивностью военных потерь РККА 
            .aw_conscript_combat(0.7)
            // доля потерь остальных групп (гражданского населения) связанная с интенсивностью военных потерь РККА
            .aw_civil_combat(0.2);

    /* 
     * интенсивность иммиграции в РСФСР из западных ССР по полугодиям с 1941.1
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
    private PopulationContext deficit1946_preimmigration;

    /* дефицит населения  на начало 1946 года, после поправки на иммиграцию */
    private PopulationContext deficit1946_postimmigration;

    /* интенсивность иммиграции (ддя РСФСР) */
    private AgeLineFactorIntensities immigration_intensity = null;

    public static enum Phase
    {
        PRELIMINARY, ACTUAL
    };

    private Phase phase;

    private Summary summary = new Summary();
    
    private static Map<Area, double[]> area2births = new HashMap<>();

    private static class Summary
    {
        double actual_warborn_deficit_at1946;
    }

    void main() throws Exception
    {
        /*
         * Установка конфигурации 
         */
        Util.out("");
        Util.out("**********************************************************************************");
        Util.out("Вычисление для " + area.toString());
        Util.out("");

        if (model != null)
        {
            /*
             * значения модели заданы внешним драйвером
             */
            if (wamp.equals(model.params.wamp))
                Automation.setAutomated(false);
            this.wamp = model.params.wamp;
            this.PrintDiagnostics = model.params.PrintDiagnostics;
            this.exportDirectory = model.params.exportDirectory;
        }
        else
        {
            model = new Model();
            model.params = null;
            // keep only model.results
        }

        if (ApplyAntibiotics)
        {
            Util.out("С учётом влияния антибиотиков");
        }
        else
        {
            Util.out("Без учёта влияния антибиотиков");
        }

        /*
         * Подготовить возрастные коэффциенты рождаемости 
         */
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
        mt1940.seal();
        peacetimeMortalityTables = new PeacetimeMortalityTables(mt1940, ApplyAntibiotics);
        if (Util.False)
        {
            PeacetimeMortalityTables.diagDisplay(mt1940, Locality.TOTAL, Gender.BOTH, 
                                                 "Возрастная смертность населения " + area.toString() + " в 1940 году, оба пола, все виды местностей");
        }
        if (Util.False)
        {
            peacetimeMortalityTables.diagPrintFirstEntries(10);
            peacetimeMortalityTables.diag_display_lx();
        }

        /* 
         * Население на начало 1941 года.
         * 
         * К населению на начало 1941 года прилагается крупнозернистая коррекция раскладки численности внутри 5-летних групп.
         * созданной автоматической дезагрегацией 5-летних групп в 1-годовые значения. 
         * Разбивка по 5-летним группам не меняется, но значения для некоторых возрастов перераспределяются 
         * по годам внутри групп так, чтобы избежать артефакта отрицательной величины потерь в 1941-1945 гг.
         */
        PopulationEarly1941 pe1941 = new PopulationEarly1941(ap);
        AdjustPopulation adjuster1941 = new AdjustPopulation1941(area);
        PopulationContext p_start1941 = pe1941.evaluate(adjuster1941);
        PopulationContext p_start1941_0 = p_start1941.clone();
        
        if (Util.False)
        {
            /*
             * Распечатать среднюю мирную смертность мужчин в возрастах 25-55 лет в населении в целом
             * для сравнения со смертностью в ГУЛАГе в 1941-1943 гг.
             */
            MiscHelper.showAverageMortality(Gender.MALE, p_start1941.toPopulation(), mt1940, 25, 55);

            /*
             * Распечатать среднюю мирную смертность населения в возрастах 18-45 лет в населении в целом
             * для сравнения со смертностью остарбайтеров
             */
            MiscHelper.showAverageMortality(Gender.MALE, p_start1941.toPopulation(), mt1940, 18, 45);
            MiscHelper.showAverageMortality(Gender.FEMALE, p_start1941.toPopulation(), mt1940, 18, 45);
            MiscHelper.showAverageMortality(Gender.BOTH, p_start1941.toPopulation(), mt1940, 18, 45);
        }

        /* 
         * Перераспределить население внутри 5-летних групп аггреграции,
         * для устранения артефакты отрицательной величины потерь в 1941-1945 гг.
         * для некоторых возрастных линий.
         */
        PopulationContext p_mid1941;
        PopulationContext p_mid1941_wam = null;
        PopulationContext deaths_1941_1st_halfyear;
        double births_1941_1st_halfyear;
        double[] births_1941_1st_halfyear_byday;
        for (int npass = 0;;)
        {
            /* 
             * Население на середину 1941 года по передвижке на основе ASFR.
             */
            PopulationForwardingResult1941 fr = new PopulationMiddle1941(ap)
                    .forward_1941_1st_halfyear(p_start1941,
                                               peacetimeMortalityTables,
                                               asfr_calibration,
                                               halfyearly_asfrs.getForTimepoint("1941.0"));
            p_mid1941 = fr.p_mid1941;
            if (p_mid1941_wam == null)
                p_mid1941_wam = fr.p_mid1941;
            deaths_1941_1st_halfyear = fr.observed_deaths_byGenderAge;
            births_1941_1st_halfyear = fr.observed_births;
            births_1941_1st_halfyear_byday = fr.births_byday;

            if (npass == 1)
                break;

            /*
             * Нужно ли перераспрелелить население внутри 5-летних групп?
             */
            WarAttritionModel wam = new WarAttritionModel(p_mid1941_wam, p1946_actual, wamp);
            AdjustPopulation1941vs1946 rp = new AdjustPopulation1941vs1946(ap, peacetimeMortalityTables, wam, p1946_actual, 0.01);
            PopulationContext p = rp.refine(p_start1941);
            if (p == null)
                break;

            if (npass++ > 10)
                throw new Exception("Коррекция населения на начало 1941 года не сходится");

            Util.assertion(Util.same(p.sum(), p_start1941.sum()));
            p_start1941 = p;
        }

        if (Util.False)
        {
            /* отобразить график населения на начало 1941 года */
            PopulationChart.display("Население " + area + " на начало 1941 года",
                                    pe1941.loaded(), "загруженное",
                                    p_start1941_0, "промежуточное",
                                    p_start1941, "конечное");
        }

        if (Util.False)
        {
            /* отобразить график населения на середину 1941 года */
            PopulationChart.display("Население " + area + " на середину 1941 года", p_mid1941, "");
        }

        if (Util.False)
        {
            /* сохранить график населения на середину 1941 года */
            ExportResults.exportImage("Население " + area + " на середину 1941 года", p_mid1941, ap, exportDirectory, "population-1941.2");
        }

        /*
         * Расчёт дефицита, возрастных линий и др. данных по полугодиям для населения наличного на середину 1941 года,
         * без учёта рождений после середины 1941 года.
         */
        if (ap.area == Area.USSR)
        {
            stage_1(Phase.ACTUAL, p_start1941, deaths_1941_1st_halfyear, births_1941_1st_halfyear, births_1941_1st_halfyear_byday, p_mid1941, null);
        }
        else if (ap.area == Area.RSFSR)
        {
            /*
             * Предварительный расчёт без учёта иммиграции.
             * Вычисляет иммиграцию.
             */
            stage_1(Phase.PRELIMINARY, p_start1941, deaths_1941_1st_halfyear, births_1941_1st_halfyear, births_1941_1st_halfyear_byday, p_mid1941,
                    null);

            /*
             * Перерасчёт с учётом иммиграции.
             */
            HalfYearEntries<HalfYearEntry> immigration_halves = halves;
            halves = null;

            stage_1(Phase.ACTUAL, p_start1941, deaths_1941_1st_halfyear, births_1941_1st_halfyear, births_1941_1st_halfyear_byday, p_mid1941,
                    immigration_halves);

            /*
             * Проверить, что новый расчёт иммиграции не отличается от начального.
             */
            double v1 = totalImmigration(immigration_halves);
            double v2 = totalImmigration(halves);

            // в случае расхождения повторять stage_1 итеративно до схождения
            if (!Util.same(v1, v2))
                throw new Exception("Расхождение в исчисленном объёме иммиграции на предварительном и окончательном шагах");
        }

        /*
         * Расчёт числа рождений по полугодиям начиная с середины 1941 года.
         */
        evalNewBirths();

        /*
         * Расчёт числа смертей от новых рождений (после середины 1941 года) при условии, если бы
         * детская смертность имела величину мирного времени. 
         */
        evalDeathsForNewBirths_UnderPeacetimeChildMortality();

        /*
         * Расчёт числа смертей от новых рождений (после середины 1941 года) при фактической военной величине 
         * детской смертности. Заполняет возрастные линии для населения родившегося после середины 1941 года. 
         */
        fitDeathsForNewBirths_UnderActualWartimeChildMortality();

        /*
         * Проверка и распечатка результатов
         */
        new VerifyHalfYears(ap, halves).verify(false);

        PrintHalfYears.print(ap, halves, model.results);
        PrintYears.print(ap, halves, model.results);

        PopulationContext allExcessDeathsByDeathAge = allExcessDeathsByDeathAge(true);
        PopulationContext allExcessDeathsByAgeAt1946 = allExcessDeathsByAgeAt1946(true);

        if (Util.False)
        {
            new PopulationChart("Избыточные смерти " + area + " в 1941-1945 гг. по возрасту в момент смерти")
                    .show("смерти", allExcessDeathsByDeathAge)
                    .display();
        }

        if (Util.False)
        {
            new PopulationChart("Избыточные смерти " + area + " в 1941-1945 гг. по возрасту на начало 1946")
                    .show("смерти", allExcessDeathsByAgeAt1946)
                    .display();
        }

        if (Util.False)
        {
            ShowAgeSliceDeathHistory.show(halves, Gender.BOTH, 0, 20);
        }

        ExportResults.exportResults(exportDirectory, ap, halves,
                                    allExcessDeathsByDeathAge,
                                    allExcessDeathsByAgeAt1946,
                                    deficit1946_preimmigration,
                                    deficit1946_postimmigration);
        
        area2births.put(area, birthCurve(halves));
        if (area2births.size() == 2)
            ExportResults.exportBirths(exportDirectory, area2births.get(Area.USSR), area2births.get(Area.RSFSR));

        evalSummary(allExcessDeathsByAgeAt1946(false));
    }

    private void stage_1(
            Phase phase,
            PopulationContext p_start1941,
            PopulationContext deaths_1941_1st_halfyear,
            double births_1941_1st_halfyear,
            double[] births_1941_1st_halfyear_byday,
            PopulationContext p_mid1941,
            HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        this.phase = phase;
        PrintAgeLine.setAreaPhase(area, phase, p1946_actual);

        /* передвижка по полугодиям для мирных условий */
        halves = evalHalves(p_start1941, deaths_1941_1st_halfyear, births_1941_1st_halfyear, births_1941_1st_halfyear_byday, p_mid1941,
                            immigration_halves);

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
        evalAgeLines(immigration_halves);
    }

    /* ================================================================================== */

    /*
     * Подготовить полугодовые сегменты.
     * Передвижка для мирного времени с шагом полгода от середины 1941 до начала 1946 года.
     */
    private HalfYearEntries<HalfYearEntry> evalHalves(
            PopulationContext p_start1941,
            PopulationContext deaths_1941_1st_halfyear,
            double births_1941_1st_halfyear,
            double[] births_1941_1st_halfyear_byday,
            PopulationContext p_mid1941,
            HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        HalfYearEntries<HalfYearEntry> halves = new HalfYearEntries<HalfYearEntry>();

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
        curr.expected_nonwar_births_byday = births_1941_1st_halfyear_byday;

        halves.add(curr);

        /* второе полугодие 1941 (на начало) */
        half = HalfYearSelector.SecondHalfYear;
        curr = new HalfYearEntry(year, half, p_mid1941.clone(), p_mid1941.clone());
        curr.peace_mt = peacetimeMortalityTables.getTable(1941, HalfYearSelector.SecondHalfYear);
        prev.next = curr;
        curr.prev = prev;
        prev = curr;
        halves.add(curr);

        /* остальные полугодия: сначала без рождений */
        evalHalves_without_births(halves, curr, p_mid1941, immigration_halves);

        /* остальные полугодия: с рождениями, предварительный расчёт их числа */
        evalHalves_with_births(halves, curr, p_mid1941, immigration_halves);

        /* сгладить число рождений по времени, сделав непрерывным */
        new SmoothBirths().init_nonwar(ap, halves).calc().to_nonwar(halves);

        /* остальные полугодия: с рождениями, конечный расчёт их числа */
        evalHalves_with_births(halves, curr, p_mid1941, immigration_halves);

        /*
         * Дополнительные данные для полугодий
         */
        for (HalfYearEntry he : halves)
        {
            if (he.year == 1946)
                break;

            if (he.peace_mt == null)
                he.peace_mt = peacetimeMortalityTables.getTable(he.year, he.halfyear);

            he.peace_lx_male = peacetimeMortalityTables.mt2lx(he.year, he.halfyear, he.peace_mt, Locality.TOTAL, Gender.MALE);
            he.peace_lx_female = peacetimeMortalityTables.mt2lx(he.year, he.halfyear, he.peace_mt, Locality.TOTAL, Gender.FEMALE);
        }

        return halves;
    }

    private void evalHalves_without_births(
            HalfYearEntries<HalfYearEntry> halves,
            HalfYearEntry he_mid1941,
            PopulationContext p_mid1941,
            HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        HalfYearEntry curr = he_mid1941;
        int year = he_mid1941.year;
        HalfYearSelector half = he_mid1941.halfyear;
        HalfYearEntry prev = he_mid1941;

        PopulationContext pxb = p_mid1941.clone();

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
            CombinedMortalityTable mt = peacetimeMortalityTables.getTable(current_year, current_half);
            curr.peace_mt = mt;

            if (Util.False)
            {
                Util.out(String.format("Передвижка XB %d.%d => %d.%d таблица %s",
                                       current_year, current_half.seq(1),
                                       year, half.seq(1),
                                       mt.comment()));
            }

            /* иммиграция */
            PopulationContext immigration = null;
            if (immigration_halves != null && immigration_halves.get(year, half).prev.immigration != null)
            {
                immigration = immigration_halves.get(year, half).prev.immigration;
                immigration = immigration.moveUpByDays(years2days(0.5));
            }

            /* передвижка на следующие полгода населения без учёта рождений */
            PopulationContext pxb_before = pxb.clone();

            ForwardPopulationT f_xb = new ForwardPopulationT();
            f_xb.setBirthRateTotal(0);
            f_xb.forward(pxb, mt, 0.5);
            if (immigration != null)
                pxb = pxb.add(immigration, ValueConstraint.NON_NEGATIVE);

            /* сохранить результаты в полугодовой записи */
            double extra_deaths_xb = pxb.clipLastDayAccumulation();
            curr = new HalfYearEntry(year, half, null, pxb.clone());
            prev.expected_nonwar_deaths = f_xb.getObservedDeaths() + extra_deaths_xb;

            curr.prev = prev;
            prev.next = curr;
            prev = curr;
            halves.add(curr);

            PrintAgeLine.printEvalHalves(curr.prev, pxb_before, pxb, immigration);
        }
    }

    private void evalHalves_with_births(
            HalfYearEntries<HalfYearEntry> halves,
            HalfYearEntry he_mid1941,
            PopulationContext p_mid1941,
            HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        HalfYearEntry curr = he_mid1941.next;
        PopulationContext pwb = p_mid1941.clone();

        /* продвигать с шагом по полгода до января 1946 */
        while (curr != null)
        {
            HalfYearEntry prev = curr.prev;
            int year = curr.year;
            HalfYearSelector half = curr.halfyear;

            if (Util.False)
            {
                Util.out(String.format("Передвижка WB %s => %s таблица %s", prev.id(), curr.id(), prev.peace_mt.comment()));
            }

            /* иммиграция */
            PopulationContext immigration = null;
            if (immigration_halves != null && immigration_halves.get(year, half).prev.immigration != null)
            {
                immigration = immigration_halves.get(year, half).prev.immigration;
                immigration = immigration.moveUpByDays(years2days(0.5));
            }

            /* передвижка на следующие полгода населения с учётом рождений */
            ForwardPopulationT f_wb = new ForwardPopulationT();
            if (prev.expected_nonwar_births_byday != null)
            {
                double[] m_births = WarHelpers.male_births(prev.expected_nonwar_births_byday);
                double[] f_births = WarHelpers.female_births(prev.expected_nonwar_births_byday);
                f_wb.setBirthCount(m_births, f_births);
            }
            else
            {
                f_wb.setBirthRateTotal(ap.CBR_1940);
            }
            f_wb.forward(pwb, prev.peace_mt, 0.5);
            if (immigration != null)
                pwb = pwb.add(immigration, ValueConstraint.NON_NEGATIVE);

            /* сохранить результаты в полугодовой записи */
            double extra_deaths_wb = pwb.clipLastDayAccumulation();
            Util.unused(extra_deaths_wb);
            curr.p_nonwar_with_births = pwb.clone();
            prev.expected_nonwar_births = f_wb.getObservedBirths();

            curr = curr.next;
        }
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

        PopulationContext deficit = p1946_expected_without_births.sub(p1946_actual_born_prewar, ValueConstraint.NONE);
        PopulationContext deficit_wb_raw = p1946_expected_with_births.sub(p1946_actual, ValueConstraint.NONE);

        /* =================================================== */

        if (Util.False && area == Area.USSR)
        {
            new DiagHelper(ap, halves).showEarlyAges();
        }

        if (Util.False && area == Area.USSR)
        {
            new DiagHelper(ap, halves).showPopulationContext("p_nonwar_with_births");
            // new DiagHelper(ap, halves).showPopulationContext("p_nonwar_without_births");
        }

        if (Util.False)
        {
            PopulationChart.display("Население " + area + " 1946 actual vs. expected w/o births",
                                    p1946_actual, "actual",
                                    p1946_expected_without_births, "expected");
        }

        if (Util.False && area == Area.USSR)
        {
            CombinedMortalityTable mt = this.peacetimeMortalityTables.getTable(1941, HalfYearSelector.FirstHalfYear);
            double[] lx = this.peacetimeMortalityTables.mt2lx(1941, HalfYearSelector.FirstHalfYear, mt, Locality.TOTAL, Gender.MALE);
            ChartXY.display("Кривая lx для 1941.1 MALE", lx);

            int hydays = years2days(0.5);
            double[] survival = DiagHelper.lx2survival(lx, hydays);
            ChartXY.display("Кривая survival для 1941.1 MALE", survival);

            for (HalfYearEntry he : halves)
            {
                if (he.index() <= 2)
                    PopulationChart.display("Население " + area + " " + he.id() + " (без рождений после середины 1941) ", he.p_nonwar_without_births,
                                            "1");
            }

            double[] p = halves.get(1941, HalfYearSelector.FirstHalfYear).p_nonwar_without_births.asArray(Locality.TOTAL, Gender.MALE);
            DiagHelper.viewProjection(p, survival, hydays);
        }

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

        /* =================================================== */

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

        if (PrintDiagnostics && phase == Phase.ACTUAL)
        {
            WarHelpers.validateDeficit(deficit);
        }

        if (Util.False)
        {
            /* график сверхсмертности */
            PopulationChart.display("Cверхсмертный дефицит населения " + area + " накопленный с середины 1941 по конец 1945 года",
                                    deficit,
                                    "1");
        }

        if (area == Area.USSR)
        {
            ExportResults.exportImage("Cверхсмертный дефицит населения " + area + " накопленный с середины 1941 по конец 1945 года",
                                      deficit, ap, exportDirectory, "deficit-1946");
        }
        else if (phase == Phase.ACTUAL)
        {
            ExportResults.exportImage("Cверхсмертный дефицит населения " + area + " накопленный с середины 1941 по конец 1945 года  (с иммиграцией)",
                                      deficit, ap, exportDirectory, "deficit-1946-with-immigration");
        }
        else
        {
            ExportResults.exportImage("Cверхсмертный дефицит населения " + area + " накопленный с середины 1941 по конец 1945 года (до иммиграции)",
                                      deficit, ap, exportDirectory, "deficit-1946-without-immigration");
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

            this.deficit1946_postimmigration = deficit_wb_raw;
        }
        else if (phase == Phase.PRELIMINARY)
        {
            if (this.deficit1946_preimmigration == null)
                this.deficit1946_preimmigration = deficit_wb_raw;
        }

        if (phase == Phase.ACTUAL)
        {
            v = deficit.sumDays(Gender.FEMALE, years2days(20.0), years2days(40.0));
            outk("Дефицит женщин в возрасте 20-40 лет на начало 1946 года, тыс. чел.", v);
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
    private void evalAgeLines(HalfYearEntries<HalfYearEntry> immigration_halves) throws Exception
    {
        /* 
         * вычислить коэфициенты интенсивности военных потерь для каждого возраста и пола,
         * подогнав их так, чтобы начальное (на середину 1941) население половозрастной линии 
         * приходило к конечному (на начало 1946) 
         */
        WarAttritionModel wam = new WarAttritionModel(halves.get("1941.2").p_nonwar_with_births,
                                                      p1946_actual,
                                                      wamp);
        EvalAgeLineLossIntensities eval = new EvalAgeLineLossIntensities(ap, phase, halves, wam);
        AgeLineFactorIntensities alis = eval.evalPreliminaryLossIntensity(p1946_actual, immigration_halves);

        if (Util.False)
        {
            alis.display("Интенсивность военных потерь " + area);
            PopulationContext p = alis.toPopulationContext();
            Util.unused(p);
        }

        if (ap.area == Area.RSFSR && phase == Phase.PRELIMINARY)
        {
            boolean diag = Util.False;

            if (diag && Util.True)
            {
                /*
                 * Диагностика
                 */
                Util.err("");
                Util.err("Участки отрицательной интенсивности потерь MALE:");
                Util.err(alis.dumpNegRegions(Gender.MALE));

                Util.err("");
                Util.err("Участки отрицательной интенсивности потерь FEMALE:");
                Util.err(alis.dumpNegRegions(Gender.FEMALE));

                alis.display("Интенсивность военных потерь " + area);
                PopulationContext p = alis.toPopulationContext();
                Util.unused(p);
            }

            /*
             * Для РСФСР для определенных групп (gender, age1-age2)
             * устранить отрицательные коэффициенты военных потерь (или провалы в смертности), вызываемые иммиграцией.
             * 
             * Интерполировать значения коэффициента интнсивности военных потерь (хранимые в @alis) между положительными 
             * точками age1-age2 для устранения промежуточных отрицательных значений.
             * 
             * Затем найти положительный коэф. иммиграционной интенсивности в этих возрастах/полах (для этих половозрастных линий).
             * 
             * Возраст указывается на середину 1941 года.
             */
            AgeLineFactorIntensities alis_initial = alis.clone();

            /*
             * Более-менее удовлетворительные значения @thresholdFactor: 0.1-0.3
             */
            final double thresholdFactor = 0.3;
            alis.unneg(thresholdFactor);

            if (diag && Util.True)
            {
                alis.display("Исправленная интенсивность военных потерь " + area);
                PopulationContext p_alis = alis.toPopulationContext();
                Util.unused(p_alis);
            }

            /* нормализованный полугодовой коэффициент интенсивности иммиграции в РСФСР */
            double[] ac_rsfsr_immigration = Util.normalize(rsfsr_immigration_intensity);

            /* вычислить интенсивность иммиграции */
            eval.setImmigration(ac_rsfsr_immigration);
            immigration_intensity = new AgeLineFactorIntensities();
            eval.evalMigration(p1946_actual, immigration_intensity, alis, alis_initial, Gender.MALE, 0, 80);
            eval.evalMigration(p1946_actual, immigration_intensity, alis, alis_initial, Gender.FEMALE, 0, 80);

            if (diag && Util.True)
            {
                immigration_intensity.display("Интенсивность иммиграции " + area);
                PopulationContext p_amig = immigration_intensity.toPopulationContext();
                Util.unused(p_amig);
            }
        }
        else if (ap.area == Area.RSFSR && phase == Phase.ACTUAL)
        {
            double[] ac_rsfsr_immigration = Util.normalize(rsfsr_immigration_intensity);
            eval.setImmigration(ac_rsfsr_immigration);
        }

        if (phase == Phase.ACTUAL)
        {
            /* распечатать участки с отрицательным ali (порождающим отрицательную величину excess deaths) */
            String neg_male = alis.dumpNegRegions(Gender.MALE);
            String neg_female = alis.dumpNegRegions(Gender.FEMALE);

            if (alis.hasNegativeRegions(neg_male))
            {
                Util.err("");
                Util.err("Участки отрицательной интенсивности потерь для мужчин, порождают отрицательную величину excess deaths:");
                Util.err("возраст: дни в середине 1941 - годы в середине 1941 - годы в начале 1941:");
                Util.err(neg_male);
            }

            if (alis.hasNegativeRegions(neg_female))
            {
                Util.err("");
                Util.err("Участки отрицательной интенсивности потерь для женщин, порождают отрицательную величину excess deaths:");
                Util.err("возраст: дни в середине 1941 - годы в середине 1941 - годы в начале 1941:");
                Util.err(neg_female);
            }

            if (alis.hasNegativeRegions(neg_male) || alis.hasNegativeRegions(neg_female))
            {
                // PopulationContext p = alis.toPopulationContext();
                // Util.noop();
            }
        }

        /* 
         * расчёт (и действительное построение) возрастных линий с учётов найденных коэфициентов интенсивности потерь
         * и иммиграционной интенсивности 
         */
        eval.processAgeLines(alis, immigration_intensity, p1946_actual);

        /* compare halves.last.actual_population vs. p1946_actual_born_prewar */
        PopulationContext diff = p1946_actual_born_prewar.sub(halves.last().actual_population, ValueConstraint.NONE);
        if (area == Area.USSR)
        {
            Util.assertion(Math.abs(diff.sum(0, MAX_AGE - 1)) < 100);
        }
        else if (!Automation.isAutomated())
        {
            Util.assertion(Math.abs(diff.sum(0, MAX_AGE - 1)) < 1000);
        }
        else
        {
            Util.assertion(Math.abs(diff.sum(0, MAX_AGE - 1)) < 2000);
        }

        if (Automation.isAutomated())
        {
            Util.assertion(Math.abs(diff.getYearValue(Gender.BOTH, MAX_AGE)) < 20_000);
        }
        else
        {
            Util.assertion(Math.abs(diff.getYearValue(Gender.BOTH, MAX_AGE)) < 5_000);
        }

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

        if (phase == Phase.ACTUAL)
        {
            outk("Избыточное число всех смертей", sum_all);
            outk(String.format("Избыточное число смертей мужчин призывного возраста (%.1f-%.1f лет)",
                               Constants.CONSCRIPT_AGE_FROM,
                               Constants.CONSCRIPT_AGE_TO),
                 sum_conscripts);
        }
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
     * по данным анамнестического опроса 1960 года 
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

        /* сгладить число рождений по времени, сделав непрерывным */
        new SmoothBirths().init_actual(ap, halves).calc().to_actual(halves);
    }

    /* ======================================================================================================= */

    /*
     * Вычислить:
     * 
     *   - ожидаемое число смертей от новых рождений в военное время (от фактического числа военных рождений)
     *     при условии детской смертности мирных условий,
     *     
     *   - ожидаемый остаток фактически рождённых в 1941.вт.пол. - 1945.вт.пол. к началу 1946 при детской смертности 
     *     мирных условий,
     *     
     *   - дефицит остатка на начало 1946 года из-за возросшей в военное время детской смертности. 
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
            double[] births = null;
            if (Util.False)
            {
                double nb1 = he.prev.actual_births;
                double nb2 = he.actual_births;
                double nb3 = (he.next != null) ? he.next.actual_births : nb2;
                births = WarHelpers.births(ndays, nb1, nb2, nb3);
            }
            else
            {
                births = he.actual_births_byday;
            }
            double[] m_births = WarHelpers.male_births(births);
            double[] f_births = WarHelpers.female_births(births);
            fw.setBirthCount(m_births, f_births);

            fw.forward(p, he.peace_mt, 0.5);

            // остаток родившихся за время войны
            he.next.wartime_born_remainder_UnderPeacetimeChildMortality = p.clone();

            // число смертей от рождений при мирной смертности
            Util.assertion(Util.same(fw.getObservedDeaths(), fw.deathsByGenderAge().sum()));
            he.actual_warborn_deaths_baseline_v1 = fw.getObservedDeaths();
            he.actual_peace_deaths_from_newborn = fw.deathsByGenderAge().clone();
        }

        double v1 = p.sum();
        double v2 = p1946_actual_born_postwar.sum();

        outk("Сверхсмертность рождённых во время войны, по дефициту к началу 1946 года, тыс. чел.", v1 - v2);
        summary.actual_warborn_deficit_at1946 = v1 - v2;
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

            if (Math.abs(m1 - m2) < 0.00001)
            {
                if (Util.same(m1, 0.5))
                    m1 = 0.05;
                else
                    throw new Exception("Итерация fitDeathsForNewBirths не сходится");
            }

        }

        double excess = 0;
        for (HalfYearEntry he = halves.get("1941.2"); he.year != 1946; he = he.next)
            excess += he.actual_warborn_deaths - he.actual_warborn_deaths_baseline();
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
            double[] births = null;
            if (Util.False)
            {
                double nb1 = he.prev.actual_births;
                double nb2 = he.actual_births;
                double nb3 = (he.next != null) ? he.next.actual_births : nb2;
                births = WarHelpers.births(ndays, nb1, nb2, nb3);
            }
            else
            {
                births = he.actual_births_byday;
            }
            double[] m_births = WarHelpers.male_births(births);
            double[] f_births = WarHelpers.female_births(births);
            fw.setBirthCount(m_births, f_births);

            List<PatchInstruction> instructions = new ArrayList<>();
            PatchInstruction instruction = new PatchInstruction(PatchOpcode.MultiplyWithDecay, 0, 5, multiplier * imr_fy_multiplier(he), 1.0);
            // PatchInstruction instruction = new PatchInstruction(PatchOpcode.Multiply, 0, 7, multiplier * imr_fy_multiplier(he));
            instructions.add(instruction);
            CombinedMortalityTable mt = PatchMortalityTable.patch(mt1940, instructions, "множитель смертности " + multiplier);

            /*
             * If computed mt is less lethal than peace_mt, then use peace_mt
             */
            if (compareTablesLethality(mt, he.peace_mt) == -1)
                mt = he.peace_mt;

            PopulationContext p0 = p.clone();
            // fw.setNewbornDeathRegistrationAge(NewbornDeathRegistrationAge.MIRROR_AGE);
            fw.setNewbornDeathRegistrationAge(NewbornDeathRegistrationAge.AT_AGE_DAY0);
            fw.forward(p, mt, 0.5);

            if (record)
            {
                // check that peacetime table is less lethal than wartime table 
                checkTableIsLessLethal(he.peace_mt, mt, he.id());

                // остаток родившихся за время войны
                he.next.wartime_born_remainder_UnderActualWartimeChildMortality = p.clone();

                // ввести остаток рождённых до конца полугодия в население начала следующего полугодия
                merge(p, he.next.actual_population);

                // число смертей от рождений
                Util.assertion(Util.same(fw.getObservedDeaths(), fw.deathsByGenderAge().sum()));

                he.actual_warborn_deaths = fw.getObservedDeaths();
                add(fw.deathsByGenderAge(), he.actual_deaths);

                if (Util.False)
                {
                    /*
                     * Количество избыточных детских смертей в полугодии вычисляется как разница с числом смертей в этом полугодии
                     * при детской смертности (в данном полугодии) мирных условий и при начальном детском населении полугодия 
                     * по непрерывной мирной продвижке с середины 1941 года, т.е. при мирной убыли в предыдущих полугодиях. 
                     */
                    add(he.actual_peace_deaths_from_newborn, he.actual_peace_deaths);

                    PopulationContext excess = fw.deathsByGenderAge().sub(he.actual_peace_deaths_from_newborn, ValueConstraint.NONE);
                    // контроль положительности delta раздельно по полам, сумме и возрастным значениям
                    validateDeathsForNewBirths(excess, he.id());
                    add(excess, he.actual_excess_wartime_deaths);
                }
                else
                {
                    /*
                     * Количество избыточных детских смертей в полугодии вычисляется как разница с числом смертей в этом полугодии
                     * при детской смертности (в данном полугодии) мирных условий и при начальном детском населении полугодия 
                     * по фактической военной продвижке, т.е. при фактической военной убыли в предыдущих полугодиях.
                     */
                    ForwardPopulationT fw_peace = new ForwardPopulationT();
                    fw_peace.setBirthCount(m_births, f_births);
                    fw_peace.setNewbornDeathRegistrationAge(NewbornDeathRegistrationAge.AT_AGE_DAY0);
                    fw_peace.forward(p0, he.peace_mt, 0.5);

                    he.actual_warborn_deaths_baseline_v2 = fw_peace.getObservedDeaths();

                    add(fw_peace.deathsByGenderAge(), he.actual_peace_deaths);

                    PopulationContext excess = fw.deathsByGenderAge().sub(fw_peace.deathsByGenderAge(), ValueConstraint.NONE);
                    // контроль положительности delta раздельно по полам, сумме и возрастным значениям
                    validateDeathsForNewBirths(excess, he.id());
                    add(excess, he.actual_excess_wartime_deaths);
                }
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

    /*
     * Контроль положительности избытка детских смертей по полам, сумме и возрастным значениям
     *     excess = newborn actual wartime deathsByGenderAge() - newborn peacetime deathsByGenderAge() 
     */
    private void validateDeathsForNewBirths(PopulationContext excess, String heid) throws Exception
    {
        for (Gender gender : Gender.TwoGenders)
        {
            for (int nd = 0; nd <= excess.MAX_DAY; nd++)
            {
                double v = excess.getDay(Locality.TOTAL, gender, nd);
                Util.assertion(v >= 0);
            }
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

    /*
     * Check that t1 is less lethal in children's age than t2
     */
    private void checkTableIsLessLethal(CombinedMortalityTable t1, CombinedMortalityTable t2, String heid) throws Exception
    {
        for (Gender gender : Gender.TwoGenders)
        {
            double[] qx1 = t1.getSingleTable(Locality.TOTAL, gender).qx();
            double[] qx2 = t2.getSingleTable(Locality.TOTAL, gender).qx();

            for (int age = 0; age <= 5; age++)
            {
                if (qx1[age] <= qx2[age])
                {
                    // ok
                }
                else
                {
                    Util.err(String.format("Peacetime table is more lethal than wartime table: %s %s %s %d : %.1f vs %.1f",
                                           area.name(), heid, gender.name(), age, qx1[age] * 1000, qx2[age] * 1000));
                }
            }
        }
    }

    /*
     * If @t1 is less lethal for ages 0-5 than @t2, return -1.
     * If @t1 is more lethal for ages 0-5 than @t2, return 1.
     * If they are the same, return 0;
     */
    private int compareTablesLethality(CombinedMortalityTable t1, CombinedMortalityTable t2) throws Exception
    {
        int result = 0;
        boolean nonuniform = false;

        for (Gender gender : Gender.TwoGenders)
        {
            if (nonuniform)
                break;

            double[] qx1 = t1.getSingleTable(Locality.TOTAL, gender).qx();
            double[] qx2 = t2.getSingleTable(Locality.TOTAL, gender).qx();

            for (int age = 0; age <= 5; age++)
            {
                if (Math.abs(qx1[age] - qx2[age]) < 0.0001)
                {
                    // ignore
                }
                else if (qx1[age] < qx2[age])
                {
                    if (result == 1)
                    {
                        nonuniform = true;
                        break;
                    }
                    result = -1;
                }
                else // if (qx1[age] > qx2[age])
                {
                    if (result == -1)
                    {
                        nonuniform = true;
                        break;
                    }
                    result = 1;
                }
            }
        }

        if (nonuniform)
        {
            Util.err("Различие таблиц неоднородно");
            StringBuilder sb;

            for (Gender gender : Gender.TwoGenders)
            {
                double[] qx1 = t1.getSingleTable(Locality.TOTAL, gender).qx();
                double[] qx2 = t2.getSingleTable(Locality.TOTAL, gender).qx();

                sb = new StringBuilder();
                sb.append(String.format("%-6s", gender.name().toUpperCase()));
                for (int age = 0; age <= 5; age++)
                    sb.append(String.format("  %5d", age));
                Util.out(sb.toString());

                sb = new StringBuilder("      ");
                for (int age = 0; age <= 5; age++)
                    sb.append(String.format("  %.3f", qx1[age]));
                Util.out(sb.toString());

                sb = new StringBuilder("      ");
                for (int age = 0; age <= 5; age++)
                    sb.append(String.format("  %.3f", qx2[age]));
                Util.out(sb.toString());

                sb = new StringBuilder("      ");
                for (int age = 0; age <= 5; age++)
                    sb.append(String.format("  %.3f", qx1[age] - qx2[age]));
                Util.out(sb.toString());
            }

            throw new Exception("различие таблиц неоднородно");
        }

        return result;
    }

    /* ======================================================================================================= */

    private void evalSummary(PopulationContext allExcessDeathsByAgeAt1946) throws Exception
    {
        final int nd45 = 9 * years2days(0.5);
        double v, v1, v2, v3;

        Util.out("");
        Util.out("Сводка:");
        Util.out("");

        /*
         * Потери наличного на начало войны населения
         */
        v = deficit1946_postimmigration.sumDays(nd45, deficit1946_postimmigration.MAX_DAY);
        outk("Дефицит наличного на начало войны населения к концу 1945 года, тыс. чел.", v);
        v = allExcessDeathsByAgeAt1946.sumDays(nd45, allExcessDeathsByAgeAt1946.MAX_DAY);
        outk("Избыточное число смертей в наличном на начало войны населения к концу 1945 года, тыс. чел.", v);

        /*
         * Потери женщин в возрасте 20-40 лет на 1946 год
         */
        v = deficit1946_postimmigration.sumDays(Gender.FEMALE, years2days(20.0), years2days(40.0));
        outk("Дефицит женщин в возрасте 20-40 лет на начало 1946 года, тыс. чел.", v);
        v = allExcessDeathsByAgeAt1946.sumDays(Gender.FEMALE, years2days(20.0), years2days(40.0));
        outk("Избыточное число смертей женщин в возрасте 20-40 лет на начало 1946 года, тыс. чел.", v);

        /*
         * Потери детей в возрасте 0-5.999 лет на начало войны (4.5 - 9.999 лет на 1946 год)
         */
        final int nd10y = years2days(10.0);
        v = deficit1946_postimmigration.sumDays(nd45, nd10y - 1);
        outk("Дефицит детей в возрасте 0-5.999 лет на начало войны, тыс. чел.", v);
        v = allExcessDeathsByAgeAt1946.sumDays(nd45, nd10y - 1);
        outk("Избыточные смерти детей в возрасте 0-5.999 лет на начало войны, тыс. чел.", v);

        /*
         * Потери рождённых во время войны
         */
        Util.out("");
        v = deficit1946_postimmigration.sumDays(0, nd45 - 1);
        outk("Дефицит рождённых после середины 1941 года и выживших к концу 1945 года, сравнительно с условиями мира, тыс. чел.", v);

        v1 = v2 = v3 = 0;
        for (HalfYearEntry he : halves)
        {
            if (!he.id().equals("1941.1"))
            {
                v1 += he.actual_births;
                v2 += he.expected_nonwar_births;
                v3 += he.actual_warborn_deaths - he.actual_warborn_deaths_baseline_v2;
            }
        }
        outk("Ожидаемое число рождений за период войны (середина 1941 - конец 1945) в условиях мира, тыс. чел.", v2);
        outk("Фактическое число рождений за время войны (середина 1941 - конец 1945), тыс. чел.", v1);
        outk("Число несостоявшихся рождений за период войны (середина 1941 - конец 1945), тыс. рождений", v2 - v1);
        outk("Дефицит фактически родившихся во время войны (середина 1941 - конец 1945) на конец 1945 года, тыс. чел.",
             summary.actual_warborn_deficit_at1946);
        outk("Число избыточных смертей фактически родившихся во время войны (середина 1941 - конец 1945) к концу 1945 года, тыс. чел.", v3);

        if (area == Area.RSFSR)
        {
            Util.out("Расхождения между графами рождений для РСФСР связано с тем, что некоторые из них частично учитывают иммиграцию военного времени,");
            Util.out("а другие относятся только к рождениям в населении РСФСР наличном на начало 1941 года.");
        }
    }

    /* ======================================================================================================= */

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
     * Составить половозрастную структуру всех смертей по возрасту в момент смерти
     */
    private PopulationContext allExcessDeathsByDeathAge(boolean despike) throws Exception
    {
        PopulationContext p = newPopulationContext();

        for (HalfYearEntry he : halves)
        {
            if (he.id().equals("1941.1") || he.id().equals("1946.1"))
                continue;

            p = p.add(he.actual_excess_wartime_deaths, ValueConstraint.NONE);

            if (Util.False && area == Area.USSR)
            {
                PopulationChart.display("actual_excess_wartime_deaths " + he.id(), he.actual_excess_wartime_deaths, "");
            }
        }

        /*
         * API передвижки возвращает структуру смертей за период передвижки индексированную
         * по возрасту населения на начало передвижки. Смерти новорожденных (родившихся уже
         * после начала передвижки) возвращаются в возрасте 0 днeй, т.к. отрицательная индексация
         * по возрасту не предусмотрена. Это создаёт ложный пик смертей в возрасте 0 дней. 
         * Разгладить пик на полгода.
         */
        if (despike)
        {
            PopulationContext p2 = DespikeZero.despike(p, years2days(0.5));
            if (Util.False && area == Area.USSR)
            {
                PopulationChart.display("DespikeZero " + area.name(), p, "before", p2, "after");
            }
            p = p2;
        }

        return p;
    }

    /*
     * Составить половозрастную стрктуру всех смертей по возрасту на начало 1946 года
     */
    private PopulationContext allExcessDeathsByAgeAt1946(boolean despike) throws Exception
    {
        PopulationContext p = newPopulationContext();

        for (HalfYearEntry he : halves)
        {
            if (he.id().equals("1941.1") || he.id().equals("1946.1"))
                continue;

            double offset = (1946 + 0) - (he.year + 0.5 * he.halfyear.seq(0));

            PopulationContext up = he.actual_excess_wartime_deaths.moveUpPreserving(offset);
            p = p.add(up, ValueConstraint.NONE);
        }

        /*
         * API передвижки возвращает структуру смертей за период передвижки индексированную
         * по возрасту населения на начало передвижки. Смерти новорожденных (родившихся уже
         * после начала передвижки) возвращаются в возрасте 0 днeй, т.к. отрицательная индексация
         * по возрасту не предусмотрена. Это создаёт ложный пик смертей в возрасте 0 дней,
         * повторяющийся каждое военное полугодие. 
         * Разгладить пик на предшествуюшие полгода.
         */
        if (despike)
        {
            PopulationContext p2 = DespikeComb.despike(p, years2days(4.9));
            if (Util.False && area == Area.USSR)
            {
                PopulationChart.display("DespikeComb " + area.name(), p, "before", p2, "after");
            }
            p = p2;
        }

        return p;
    }

    /* ======================================================================================================= */

    /*
     * Кривая числа рождений 
     */
    private double[] birthCurve(HalfYearEntries<HalfYearEntry> halves)
    {
        double[] curve = null;
        
        for (HalfYearEntry he : halves)
        {
            if (he.year == 1946)
                break;
            
            if (curve == null)
                curve = Util.dup(he.actual_births_byday);
            else
                curve = Util.concat(curve, he.actual_births_byday);
        }
        
        return curve;
    }
}