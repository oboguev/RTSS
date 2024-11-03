package rtss.mexico.agri.entities;

import java.util.HashMap;

import rtss.mexico.agri.calc.CaloriesIn;

/*
 * Эта структура вычисляется для каждого года.
 * 
 * Она содержит название продукта и число калорий, которое он даёт на человека в день.
 */
public class CaloricContent extends HashMap<String, Double>
{
    private static final long serialVersionUID = 1L;

    public static CaloricContent eval(CultureSet cs, int year, long population) throws Exception
    {
        CaloricContent cc = new CaloricContent();

        int ndays = 365;
        if (java.time.Year.of(year).isLeap())
            ndays = 366;

        for (Culture c : cs.cultures())
        {
            CultureYear cy = c.cultureYear(year);

            if (cy != null)
            {
                double cal = CaloriesIn.in(cy);
                cal /= population;
                cal /= ndays;
                if (cal != 0)
                    cc.put(c.name, cal);
            }
        }

        return cc;
    }
    
    public double sum()
    {
        double v = 0;
        for (String cname : keySet())
            v += get(cname);
        return v;
    }
}
