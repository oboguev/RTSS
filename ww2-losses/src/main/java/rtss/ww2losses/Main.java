package rtss.ww2losses;

import java.util.ArrayList;
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
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population_194x.MortalityTable_1940;
import rtss.ww2losses.population_194x.Population_In_Middle_1941;
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

    private Area area;
    private AreaParameters ap;
    private static int MAX_AGE = Population.MAX_AGE;

    /* фактическое население на начало 1946 года */
    private PopulationByLocality p1946_actual;

    /* фактическое население на начало 1946 года рождённое до середины 1941*/
    private PopulationByLocality p1946_actual_born_prewar;

    /* фактическое население на начало 1946 года рождённое после середины 1941*/
    private PopulationByLocality p1946_actual_born_postwar;

    private static boolean AppyAntibiotics = Util.True;

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

    private final AgeSpecificFertilityRatesByYear asfrs = AgeSpecificFertilityRatesByYear.load("age_specific_fertility_rates/survey-1960.xlsx");

    /*
     * данные для полугодий начиная с середины 1941 и по начало 1946 года
     */
    private HalfYearEntries<HalfYearEntry> halves = new HalfYearEntries<HalfYearEntry>();

    private void main() throws Exception
    {
        Util.out("");
        Util.out("**********************************************************************************");
        Util.out("Вычисление для " + area.toString());
        Util.out("");

        evalHalves();
        evalDeficit1946();
        evalBirths();

        Util.noop();
    }

    /*
     * Подготовить полугодовые сегменты
     */
    private void evalHalves() throws Exception
    {
        /* таблица смертности для 1940 года */
        CombinedMortalityTable mt1940 = new MortalityTable_1940(ap).evaluate();

        /* население на середину 1941 года */
        Population_In_Middle_1941 pm1941 = new Population_In_Middle_1941(ap);
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        PopulationByLocality p = pm1941.evaluate(fctx, mt1940);
        PopulationByLocality px = fctx.end(p);

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

        /* подготовиться к продвижке населения с учётом рождений после середины 1941 года */
        PopulationByLocality pwb = p.clone();

        /* подготовиться к продвижке населения без учёта рождений после середины 1941 года (только наличного на середину 1941 года) */
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

            /* продвижка на следующие полгода населения с учётом рождений */
            ForwardPopulationT fw1 = new ForwardPopulationT();
            fw1.setBirthRateTotal(ap.CBR_1940);
            pwb = fw1.forward(pwb, fctx, mt, 0.5);

            /* продвижка на следующие полгода населения без учёта рождений */
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
    }

    /* 
     * Определить таблицу смертности с учётом падения детской смертности из-за введения антибиотиков 
     */
    private CombinedMortalityTable year_mt(CombinedMortalityTable mt1940, int year) throws Exception
    {
        if (!AppyAntibiotics)
            return mt1940;

        double scale0;

        switch (year)
        {
        case 1940:
        case 1941:
        case 1942:
            return mt1940;

        case 1943:
            scale0 = 0.76;
            break;

        case 1944:
            scale0 = 0.53;
            break;

        case 1945:
            scale0 = 0.45;
            break;

        default:
            throw new IllegalArgumentException();
        }

        PatchInstruction instruction = new PatchInstruction(PatchOpcode.MultiplyWithDecay, 0, 5, scale0, 1.0);
        List<PatchInstruction> instructions = new ArrayList<>();
        instructions.add(instruction);

        CombinedMortalityTable xmt = PatchMortalityTable.patch(mt1940, instructions, "поправка антибиотиков для " + year);

        return xmt;
    }

    /* ======================================================================================================= */

    private void evalDeficit1946() throws Exception
    {
        double v;
        PopulationByLocality p1946_expected_with_births = halves.last().p_nonwar_with_births;
        PopulationByLocality p1946_expected_without_births = halves.last().p_nonwar_without_births;
        PopulationByLocality p1946_expected_newonly = p1946_expected_with_births.sub(p1946_expected_without_births);

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
        deficit = deficit.sub(emigration());

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

    private void evalBirths() throws Exception
    {
        double cumulative_excess_war_deaths_fertile_f = 0;

        for (HalfYearEntry he : halves)
        {
            if (he.next == null)
                break;

            /*
             * число женщин фертильного возраста
             */
            double f1 = he.p_nonwar_without_births.sum(Locality.TOTAL, Gender.FEMALE, 15, 54);
            double f2 = he.next.p_nonwar_without_births.sum(Locality.TOTAL, Gender.FEMALE, 15, 54);
            double f = (f1 + f2) / 2;
            f -= cumulative_excess_war_deaths_fertile_f;
            f -= he.excess_war_deaths_fertile_f / 2;
            cumulative_excess_war_deaths_fertile_f += he.excess_war_deaths_fertile_f;

            PopulationByLocality pf = he.p_nonwar_without_births.selectByAge(15, 54);
            pf = RescalePopulation.scaleTotal(pf, 1.0, f);

            /*
             * число фактических рождений
             */
            if (he.year != 1941)
                he.actual_births = 0.5 * asfrs.getForYear(he.year).births(pf);
        }
        
        /*
         * Для 1941 года (оба полугодия)
         */
        HalfYearEntry he1 = halves.get(0);
        HalfYearEntry he2 = halves.get(1);
        he1.actual_births = he1.expected_nonwar_births;
        
        PopulationByLocality pf = he2.p_nonwar_without_births.selectByAge(15, 54);
        double year_births = asfrs.getForYear(1941).births(pf);
        he2.actual_births = year_births - he1.actual_births; 
        
        Util.out("");
        Util.out("Дефицит числа рождений, по полугодиям:");
        for (HalfYearEntry he : halves)
        {
            double bd = he.expected_nonwar_births - he.actual_births;
            Util.out(String.format("%s %s", he.toString(), f2k(bd / 1000.0)));
        }

        Util.out("");
        Util.out("Фактическое число рождений, по полугодиям:");
        for (HalfYearEntry he : halves)
        {
            Util.out(String.format("%s %s", he.toString(), f2k(he.actual_births / 1000.0)));
        }
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
