package rtss.data.population;

import rtss.data.population.struct.Population;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;

public class ViewPopulation
{
    public static void main(String[] args)
    {
        try
        {
            new ViewPopulation().view();
            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
    
    PopulationChart chart = new PopulationChart();
    
    String suffix = null;
    
    private void view() throws Exception
    {
        // view(Area.USSR, 1941);
        view(Area.USSR, 1946);
        
        PopulationADH.setFilesVersion("ADH.v1");
        suffix = "-v1";
        
        // view(Area.USSR, 1941);
        view(Area.USSR, 1946);

        chart.display();
    }
    
    private void view(Area area, int year) throws Exception
    {
        view(area, "" + year);
    }

    private void view(Area area, String year) throws Exception
    {
        Population p = PopulationADH.getPopulation(area, year);
        String title = area.toString() + "-" + year;
        if (suffix != null)
            title += suffix;
        chart.show(title, p);
    }
}
