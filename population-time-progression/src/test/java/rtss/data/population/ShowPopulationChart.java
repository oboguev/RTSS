package rtss.data.population;

import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;

public class ShowPopulationChart
{
    public static void main(String[] args)
    {
        try
        {
            new ShowPopulationChart().show();
            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }

    private final boolean DoSmoothPopulation = Util.True;

    private void show() throws Exception
    {
        Population p1926_census = PopulationByLocality.census(Area.USSR, 1926).smooth(DoSmoothPopulation).forLocality(Locality.TOTAL);
        Population p1927_adh = PopulationADH.getPopulationByLocality(Area.USSR, 1927).forLocality(Locality.TOTAL);
        p1927_adh = RescalePopulation.scaleTo(p1927_adh, p1926_census.sum(Gender.BOTH, 0, Population.MAX_AGE));
        
        new PopulationChart("Население на момент переписи 1926 года")
                .show("перепись", p1926_census)
                .show("АДХ", p1927_adh)
                .display();
    }
}
