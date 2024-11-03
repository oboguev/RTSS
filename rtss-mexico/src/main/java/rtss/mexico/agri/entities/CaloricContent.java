package rtss.mexico.agri.entities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /*
     * Сумма всех калорий
     */
    public double sum()
    {
        double v = 0;
        for (String cname : keySet())
            v += get(cname);
        return v;
    }

    /*
     * Сумма калорий только для перечисленных продуктов
     */
    public double sum(Set<String> cnames) throws Exception
    {
        double v = 0;
        for (String cname : keySet())
        {
            if (cnames.contains(cname))
                v += get(cname);
        }
        return v;
    }

    /*
     * Вычитание: cc1 - cc2
     */
    public static CaloricContent sub(CaloricContent cc1, CaloricContent cc2) throws Exception
    {
        CaloricContent cc = new CaloricContent();
        Set<String> xs = new HashSet<>();
        xs.addAll(cc1.keySet());
        xs.addAll(cc2.keySet());

        for (String cname : xs)
        {
            Double v1 = cc1.get(cname);
            Double v2 = cc2.get(cname);
            if (v1 == null)
                v1 = 0.0;
            if (v2 == null)
                v2 = 0.0;
            cc.put(cname, v1 - v2);
        }

        return cc;
    }
    
    public List<CaloricElement> sort() throws Exception
    {
        List<CaloricElement> list = new ArrayList<>();
        for (String cname : keySet())
            list.add(new CaloricElement(cname, get(cname)));
        Collections.sort(list);
        return list;
    }

    static public class CaloricElement implements Comparable<CaloricElement>
    {
        public final String name;
        public final double calories;
        
        public CaloricElement(String name, double calories)
        {
            this.name = name;
            this.calories = calories;
        }

        @Override
        public int compareTo(CaloricElement o)
        {
            if (this.calories < o.calories)
                return 1;
            else if (this.calories > o.calories)
                return -1;
            else
                return 0;
        }
    }
}
