package rtss.data.population.synthetic;

import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.util.Util;

public class PopulationADH
{
    public static void zzz(Area area, int year) throws Exception
    {
        String path = String.format("population_data/%s/%s-population-ADH.xlsx", area.name(), area.name());
        double[] m = PopulationFromExcel.loadCounts(path, Gender.MALE, year);
        double[] f = PopulationFromExcel.loadCounts(path, Gender.FEMALE, year);
        // ###
        Util.unused(m);
        Util.unused(f);
    }
}
