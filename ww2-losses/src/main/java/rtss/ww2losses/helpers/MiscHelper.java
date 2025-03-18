package rtss.ww2losses.helpers;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class MiscHelper
{
    public static void showAverageMortality(Gender gender, Population p, CombinedMortalityTable cmt, int age1, int age2) throws Exception
    {
        double[] qx = cmt.getSingleTable(Locality.TOTAL, gender).qx();
        
        double sum = 0;
        double deaths = 0;
        
        for (int age = age1; age <= age2; age++)
        {
            double v = p.get(gender, age);
            sum += v;
            deaths += v * qx[age];
        }
        
        Util.out(String.format("Доля умирающих за год в мирных условиях %s %d-%d: %.1f промилле", gender.name(), age1, age2, 1000 * deaths / sum));
    }
}
