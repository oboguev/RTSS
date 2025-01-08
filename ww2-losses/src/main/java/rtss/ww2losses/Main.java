package rtss.ww2losses;

import java.util.ArrayList;
import java.util.List;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.data.population.Population;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
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
     * данные для полугодий начиная с середины 1941 и по начало 1946 года
     */
    private HalfYearEntries<HalfYearEntry> halves = new HalfYearEntries<HalfYearEntry>();

    private void main() throws Exception
    {
        Util.out("");
        Util.out("**********************************************************************************");
        Util.out("Вычисление для " + area.name());
        Util.out("");

        evalHalves();
        evalDeficit1946();

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

            /* продвижка на следующие полгода населения без учёта рождений */
            ForwardPopulationT fw;
            fw = new ForwardPopulationT();
            fw.setBirthRateTotal(0);
            pxb = fw.forward(pxb, fctx_xb, mt, 0.5);

            /* продвижка на следующие полгода населения с учётом рождений */
            fw = new ForwardPopulationT();
            fw.setBirthRateTotal(ap.CBR_1940);
            pwb = fw.forward(pwb, fctx, mt, 0.5);

            /* сохранить результаты в полугодовой записи */
            curr = new HalfYearEntry(year, half, fctx.end(pwb), fctx_xb.end(pxb));
            prev.expected_nonwar_births = fw.getObservedBirths();
            prev.expected_nonwar_deaths = fw.getObservedDeaths();
            
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
        
        switch(year)
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
    }

    private void split_p1946() throws Exception
    {
        p1946_actual_born_postwar = p1946_actual.selectByAge(0, 4.5);
        p1946_actual_born_prewar = p1946_actual.selectByAge(4.5, MAX_AGE + 1);
        Util.noop();
    }

    private String f2k(double v)
    {
        String s = String.format("%,15.0f", v);
        while (s.startsWith(" "))
            s = s.substring(1);
        return s;
    }
}
