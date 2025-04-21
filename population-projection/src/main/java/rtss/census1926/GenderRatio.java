package rtss.census1926;

import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.ChartXYSPlineBasic;

/*
 * Соотношение полов для возрастных групп по переписи 1926 года
 */
public class GenderRatio
{
    public static void main(String[] args)
    {
        try
        {
            new GenderRatio(Area.USSR).do_main();

            Util.out("");
            Util.out("************************************************************");
            Util.out("");
            
            new GenderRatio(Area.RSFSR).do_main();
            
            new ChartXYSPlineBasic("Соотношение полов M/F по переписи 1926 года", "age", "на 100 женщи")
            .addSeries("СССР", r_ussr)
            .addSeries("РСФСР", r_rsfsr)
            .display();
        }
        catch (Exception ex)
        {
            Util.err("** Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
    
    private final Area area;
    private static double[] r_ussr;
    private static double[] r_rsfsr;
    
    private GenderRatio(Area area)
    {
        this.area = area;
    }
    
    private void do_main() throws Exception
    {
        double[] r = new double[PopulationByLocality.MAX_AGE + 1];
        
        Util.out(String.format("Соотношение полов M/F по переписи 1926 года для %s, число мужчин на 100 женщин", area.toString()));
        Util.out("");
        PopulationByLocality p = PopulationByLocality.census(area, 1926).smooth(true);
        
        for (int age = 0; age <= PopulationByLocality.MAX_AGE; age++)
        {
            double m = p.get(Locality.TOTAL, Gender.MALE, age);
            double f = p.get(Locality.TOTAL, Gender.FEMALE, age);
            Util.out(String.format("%-3d %3.1f", age, 100 * m / f));
            r[age] = 100 * m / f;
        }
        
        switch (area)
        {
        case USSR:
            r_ussr = r;
            break;
            
        case RSFSR:
            r_rsfsr = r;
            break;
        }
    }
}
