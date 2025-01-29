package rtss.ww2losses;

import rtss.data.ValueConstraint;
import rtss.data.asfr.AgeSpecificFertilityRatesByTimepoint;
import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.asfr.InterpolateASFR;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.Population;
import rtss.data.population.forward.PopulationContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.ww2losses.old2.HalfYearEntry;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population_194x.MortalityTable_1940;
import rtss.ww2losses.util.CalibrateASFR;

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
    private static boolean AppyAntibiotics = Util.True;

    /*
     * Распечатывать диагностический вывод
     */
    private static boolean PrintDiagnostics = Util.True;

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
    private CombinedMortalityTable mt1940;

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

        /* таблица смертности для 1940 года */
        mt1940 = new MortalityTable_1940(ap).evaluate();

        // ###
    }

    /* вычесть население Тувы из населения начала 1946 года */
    private void adjustForTuva() throws Exception
    {
        // ###
    }

    private void split_p1946() throws Exception
    {
        int nd_4_5 = age2day(4.5);

        p1946_actual_born_postwar = p1946_actual.selectByAgeDays(0, nd_4_5);
        p1946_actual_born_prewar = p1946_actual.selectByAgeDays(nd_4_5 + 1, age2day(MAX_AGE + 1));

        double v_total = p1946_actual.sum();
        double v_prewar = p1946_actual_born_prewar.sum();
        double v_postwar = p1946_actual_born_postwar.sum();

        if (Util.differ(v_total, v_prewar + v_postwar))
            Util.err("Ошибка расщепления");
    }

    private int age2day(double age)
    {
        final int DAYS_PER_YEAR = 365;
        return (int) Math.round(age * DAYS_PER_YEAR);
    }
}
