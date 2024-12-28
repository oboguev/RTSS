package rtss.ww2losses;

import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.params.AreaParameters;
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
        this.ap = AreaParameters.forArea(Area.USSR);
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
        
        PopulationForwardingContext fctx = new PopulationForwardingContext();
        PopulationByLocality p = new Population_In_Middle_1941(ap).evaluate(fctx);
        PopulationByLocality px = fctx.end(p);
        
        int year = 1941;
        int half = HalfYearEntries.FirstHalfYear;

        halves.add(new HalfYearEntry(year, half, px, px));
        
        for (;;)
        {
            if (half == HalfYearEntries.FirstHalfYear)
            {
                half = HalfYearEntries.SecondHalfYear;
                if (year == 1946)
                    break;
            }
            else
            {
                half = HalfYearEntries.FirstHalfYear;
                year++;
            }
            
            // ### calc
            halves.add(new HalfYearEntry(year, half, null, null));
        }
        
        Util.noop();
    }
}
