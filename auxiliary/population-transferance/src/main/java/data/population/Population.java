package data.population;

import java.util.HashMap;
import java.util.Map;

import my.Util;

public class Population
{
    public static final int MAX_AGE = 100;

    /****************************************************************************************************/

    private Population rural;
    private Population total;
    private Population urban;
    
    public void loadCombined(String path) throws Exception
    {
        rural = loadCombined(path, "rural");
        urban = loadCombined(path, "urban");
        total = loadCombined(path, "total");
        
        for (int age = 0; age <= MAX_AGE; age++)
        {
            if (rural.male(age) + urban.male(age) != total.male(age))
                mismatch();

            if (rural.female(age) + urban.female(age) != total.female(age))
                mismatch();

            if (rural.fm(age) + urban.fm(age) != total.fm(age))
                mismatch();
        }
    }
    
    public Population loadCombined(String path, String locale) throws Exception
    {
        Population p = new Population();
        p.loadSingle(String.format("%s/%s.txt", path, locale));
        return p;
    }

    /****************************************************************************************************/
    
    public double male(int age) throws Exception
    {
        if (!male.containsKey(age))
            throw new Exception("Missing data");
        return male.get(age);
    }
    
    public double female(int age) throws Exception
    {
        if (!female.containsKey(age))
            throw new Exception("Missing data");
        return female.get(age);
    }

    public double fm(int age) throws Exception
    {
        return both(age);
    }
    
    public double both(int age) throws Exception
    {
        if (!both.containsKey(age))
            throw new Exception("Missing data");
        return both.get(age);
    }

    private Map<Integer, Double> male;
    private Map<Integer, Double> female;
    private Map<Integer, Double> both;
    
    private double male_unknown = 0;
    private double male_total = 0;
    private double female_unknown = 0;
    private double female_total = 0;
    private double both_unknown = 0;
    private double both_total = 0;
    
    public void loadSingle(String path) throws Exception
    {
        male = new HashMap<>();
        female = new HashMap<>();
        both = new HashMap<>();
        
        String rdata = Util.loadResource(path);
        rdata = rdata.replace("\r\n", "\n");
        for (String line : rdata.split("\n"))
        {
            char unicode_feff = '\uFEFF';
            line = line.replace("" + unicode_feff, "");
                    
            int k = line.indexOf('#');
            if (k != -1)
                line = line.substring(0, k);
            line = line.replace("\t", " ").replaceAll(" +", " ").trim();
            if (line.length() == 0)
                continue;
            
            String[] el = line.split(" ");
            if (el.length != 4)
                throw new Exception("Invalid format of population table");
            
            int m = asInt(el[1]);
            int f = asInt(el[2]);
            int b = asInt(el[3]);
            
            String age = el[0];
            if (age.equals("" + MAX_AGE + "+"))
                age = "" + MAX_AGE;
            
            if (age.equals("unknown"))
            {
                male_unknown = m;
                female_unknown = f;
                both_unknown = b;
            }
            else if (age.equals("total"))
            {
                male_total = m;
                female_total = f;
                both_total = b;
            }
            else
            {
                int a = asInt(age);
                if (a < 0 || a > MAX_AGE)
                    throw new Exception("Invalid value in population table");
                
                if (male.containsKey(a))
                    throw new Exception("Duplicate value in population table");
                
                male.put(a, (double) m);
                female.put(a, (double) f);
                both.put(a, (double) b);
            }
        }

        validateSingle();
    }
    
    private void validateSingle() throws Exception
    {
        double sum_m = 0;
        double sum_f = 0;
        double sum_b = 0;
        
        for (int age = 0; age <= MAX_AGE; age++)
        {
            if (!male.containsKey(age) || !female.containsKey(age) || !both.containsKey(age))
                throw new Exception("Mising entry in population table");
            
            double m = male.get(age);
            double f = female.get(age);
            double b = both.get(age);
            
            if (m + f != b)
                mismatch();
            
            sum_m += m;
            sum_f += f;
            sum_b += b;
        }
        
        if (male_total + female_total != both_total)
            mismatch();

        if (male_unknown + female_unknown != both_unknown)
            mismatch();

        if (sum_m + male_unknown != male_total)
            mismatch();
        
        if (sum_f + female_unknown != female_total)
            mismatch();
    
        if (sum_b + both_unknown != both_total)
            mismatch();
    }
    
    /****************************************************************************************************/

    private void mismatch() throws Exception
    {
        throw new Exception("Mismatching data in population table");
    }

    private int asInt(String s)
    {
        return Integer.parseInt(s.replace(",", ""));
    }
}