package rtss.data.population.synthetic;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.population.Population;
import rtss.data.selectors.Gender;
import rtss.util.excel.Excel;

public class PopulationFromExcel
{
    private static final String[] keyMales = { "male", "males", "мужчины", "муж", "муж.", "м" };
    private static final String[] keyFemales = { "female", "females", "женщины", "жен", "жен.", "ж" };
    private static final String[] keyAge = { "age", "ages", "возраст", "возрасты", "возрастная группа", "возрастные группы" };
    
    private static final double MAX_DIFF = 2;

    public static double[] loadCounts(String path, Gender gender, int year) throws Exception
    {
        /*
         * parse excel rows and fill them into the bins
         */
        List<Object> ages = loadAges(path, gender);
        List<Object> values = loadValues(path, gender, "" + year);
        
        Double total = null;
        Double unknown = null;
        List<Bin> list = new ArrayList<>();
        double sum = 0;
        
        for (int nrow = 1; nrow <= ages.size(); nrow++)
        {
            Object oa = ages.get(nrow - 1);
            String sa = null;
            if (oa == null)
            {
                continue;
            }
            else if (oa instanceof String)
            {
                sa = (String) oa;
            }
            else if (oa instanceof Double)
            {
                double da = (double) oa;
                long la = Math.round(da);
                if (Math.abs(la - da) > 0.001)
                    throw new Exception("Unexpected value type in the age column");
                sa = "" + la;
            }
            else
            {
                throw new Exception("Unexpected value type in the age column");
            }
            
            sa = sa.trim().toLowerCase();
            if (sa.equals(""))
                continue;
            if (sa.startsWith("провер") || sa.startsWith("check") || sa.startsWith("verif"))
                continue;
            
            if (sa.equals("всего"))
                sa = "total";
            if (sa.startsWith("неизвестно") || sa.startsWith("не изв"))
                sa = "unknown";

            sa = sa.replace(" и старше", "-" + Population.MAX_AGE);
            sa = sa.replace("+", "-" + Population.MAX_AGE);
            
            Object ov = values.get(nrow - 1);
            double v;
            if (ov instanceof String)
            {
                v = Double.parseDouble((String) ov);
            }
            else if (ov instanceof Double)
            {
                v = (double) ov;
            }
            else
            {
                throw new Exception("Unexpected value type in population numbers column");
            }
            
            if (sa.equals("total"))
            {
                if (total != null)
                    throw new Exception("Duplicate total");
                total = v;
            }
            else if (sa.equals("unknown"))
            {
                if (unknown != null)
                    throw new Exception("Duplicate unknown");
                unknown = v;
            }
            else if (sa.contains("-"))
            {
                String[] ss = sa.split("-");
                if (ss.length != 2)
                    throw new Exception("Unexpected value in population numbers column");
                int age_x1 = Integer.parseInt(ss[0]);
                int age_x2 = Integer.parseInt(ss[1]);
                list.add(new Bin(age_x1, age_x2, v));
                sum += v;
            }
            else
            {
                int age_x = Integer.parseInt(sa);
                list.add(new Bin(age_x, age_x, v));
                sum += v;
            }
        }

        if (unknown != null)
            sum += unknown;
        
        if (total != null)
        {
            if (Math.abs(total - sum) > MAX_DIFF)
                throw new Exception("Population bins mismatch designated total");
        }
        
        Bin[] bins = Bins.bins(list);
        
        // ### interpolate
        
        return null;
    }

    private static List<Object> loadAges(String path, Gender gender) throws Exception
    {
        return loadValues(path, gender, keyAge);
    }
    
    private static List<Object> loadValues(String path, Gender gender, String... matchingColumnNames) throws Exception
    {
        return Excel.loadColumn(path, key(gender), matchingColumnNames);
    }
    
    private static String[] key(Gender gender) throws Exception
    {
        switch (gender)
        {
        case MALE:
            return keyMales;
        case FEMALE:
            return keyFemales;
        default:
            throw new Exception("Invalid gender selector");
        }
    }
}
