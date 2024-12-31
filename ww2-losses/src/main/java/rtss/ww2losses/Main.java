package rtss.ww2losses;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.population_194x.MortalityTable_1940;
import rtss.ww2losses.population_194x.Population_In_Middle_1941;
import rtss.ww2losses.util.HalfYearEntries;

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
    }

    private Area area;
    private AreaParameters ap;

    /*
     * данные для полугодий начиная с середины 1941 и по начало 1946 года
     */
    private HalfYearEntries<HalfYearEntry> halves = new HalfYearEntries<HalfYearEntry>();

    private void main() throws Exception
    {
        Util.out("*************************************");
        Util.out("Вычисление для " + area.name());
        Util.out("");

        evalHalves();

        Util.noop();
    }

    /*
     * Подготовить полугодовые точки  
     */
    private void evalHalves() throws Exception
    {
        /* таблица смертности для 1940 года */
        CombinedMortalityTable mt1940 = new MortalityTable_1940(ap).evaluate();

        /* население на середину 1941 года */
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        PopulationByLocality p = new Population_In_Middle_1941(ap).evaluate(fctx);
        PopulationByLocality px = fctx.end(p);

        int year = 1941;
        int half = HalfYearEntries.StartOfSecondHalfYear;
        HalfYearEntry prev = new HalfYearEntry(year, half, px, px);
        halves.add(prev);

        /* подготовиться к продвижке населения с учётом рождений после середины 1941 года */
        PopulationByLocality pwb = p.clone();

        /* подготовиться к продвижке населения без учёта рождений после середины 1941 года (только наличного на середину 1941 года) */
        PopulationByLocality pxb = p.clone();
        PopulationForwardingContext fctx_xb = fctx.clone();

        /* продвигать с шагом по полгода до января 1946 */
        for (;;)
        {
            ForwardPopulationT fw;

            if (half == HalfYearEntries.StartOfFirstHalfYear)
            {
                if (year == 1946)
                    break;
                half = HalfYearEntries.StartOfSecondHalfYear;
            }
            else
            {
                half = HalfYearEntries.StartOfFirstHalfYear;
                year++;
            }

            /* определить таблицу смертности, с учётом падения детской смертности из-за введения антибиотиков */
            // ###
            CombinedMortalityTable mt = mt1940;

            /* продвижка на следующие полгода населения с учётом рождений */
            fw = new ForwardPopulationT();
            fw.setBirthRateTotal(ap.CBR_1940);
            pwb = fw.forward(pwb, fctx, mt, 0.5);

            /* продвижка на следующие полгода населения без учёта рождений */
            fw = new ForwardPopulationT();
            fw.setBirthRateTotal(0);
            pxb = fw.forward(pxb, fctx_xb, mt, 0.5);

            /* сохранить результаты в полугодовой записи */
            HalfYearEntry curr = new HalfYearEntry(year, half, fctx.end(pwb), fctx_xb.end(pxb));
            curr.prev = prev;
            prev.next = curr;
            prev = curr;
            halves.add(curr);
        }
    }
}
