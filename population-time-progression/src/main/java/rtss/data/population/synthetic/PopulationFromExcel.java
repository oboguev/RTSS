package rtss.data.population.synthetic;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableDouble;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.util.excel.ExcelLoader;

public class PopulationFromExcel extends ExcelLoader
{
    private static final double MAX_DIFF = 3;

    public static double[] loadCounts(String path, Gender gender, int year, MutableDouble v_unknown) throws Exception
    {
        return loadCounts(path, gender, "" + year, v_unknown);
    }
    
    public static double[] loadCounts(String path, Gender gender, String year, MutableDouble v_unknown) throws Exception
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
                String sv = (String) ov;
                v = Double.parseDouble(sv.replace(",", ""));
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
        {
            sum += unknown;
            if (v_unknown != null)
                v_unknown.setValue(unknown);
        }
        else
        {
            if (v_unknown != null)
                v_unknown.setValue(0);
        }

        if (total != null)
        {
            if (Math.abs(total - sum) > MAX_DIFF)
                throw new Exception("Population bins mismatch designated total");
        }

        Bin[] bins = Bins.bins(list);
        bins = Bins.sum2avg(bins);
        if (Bins.firstBin(bins).age_x1 != 0 || Bins.lastBin(bins).age_x2 != Population.MAX_AGE)
            throw new Exception("Invalid population age range");

        String title = "Population " + gender.toString() + year;
        double[] counts = bins2yearly(bins, title);
        
        double sum1 = Util.sum(counts);
        double sum2 = Bins.sum(bins);
        
        if (Util.differ(sum1, sum2))
            throw new Exception("Curve count mismatches bin count");
        
        return counts;
    }

    private static double[] bins2yearly(Bin[] bins, String title) throws Exception
    {
        boolean interpolate = false;

        for (Bin bin : bins)
        {
            if (bin.widths_in_years != 1)
                interpolate = true;
        }

        if (!interpolate)
        {
            double[] v = new double[Population.MAX_AGE + 1];
            for (int k = 0; k < v.length; k++)
                v[k] = bins[k].avg;
            return v;
        }
        
        return InterpolatePopulationAsMeanPreservingCurve.curve(bins, title);
    }
}
