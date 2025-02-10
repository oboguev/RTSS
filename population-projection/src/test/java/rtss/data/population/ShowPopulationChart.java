package rtss.data.population;

import rtss.data.population.calc.RescalePopulation;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
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
            // new ShowPopulationChart().show_1();
            new ShowPopulationChart().show_2();
            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }

    private final boolean DoSmoothPopulation = Util.True;

    @SuppressWarnings("unused")
    private void show_1() throws Exception
    {
        Population p1926_census = PopulationByLocality.census(Area.USSR, 1926).smooth(DoSmoothPopulation).forLocality(Locality.TOTAL);
        Population p1927_adh = PopulationADH.getPopulationByLocality(Area.USSR, 1927).forLocality(Locality.TOTAL);
        p1927_adh = RescalePopulation.scaleTo(p1927_adh, p1926_census.sum(Gender.BOTH, 0, Population.MAX_AGE));

        new PopulationChart("Население на момент переписи 1926 года")
                .show("перепись", p1926_census)
                .show("АДХ", p1927_adh)
                .display();
    }

    @SuppressWarnings("unused")
    private void show_2() throws Exception
    {
        final boolean DoSmoothPopulation = true;

        Population p1926_original  = PopulationByLocality.census(Area.USSR, 1926).smooth(DoSmoothPopulation).forLocality(Locality.TOTAL);
        Population p1939_original = PopulationByLocality.census(Area.USSR, 1939).smooth(DoSmoothPopulation).forLocality(Locality.TOTAL);
        Population p1937_original = PopulationByLocality.census(Area.USSR, 1937).smooth(DoSmoothPopulation).forLocality(Locality.TOTAL);
        Population p1941 = PopulationADH.getPopulationByLocality(Area.USSR, 1941).forLocality(Locality.TOTAL);

        new PopulationChart("Население масштабированное на 1941 год")
                .scale("1941", p1941)
                // .show("1926", p1926_original)
                .show("1937", p1937_original)
                .show("1939", p1939_original)
                .display();
    }
}
