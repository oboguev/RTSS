package rtss.data.mortality.synthetic;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelLoader;

public class MortalityRatesFromExcel extends ExcelLoader
{
    public static Bin[] loadRates(String path, Gender gender, int year) throws Exception
    {
        return loadRates(path, gender, "" + year);
    }

    public static Bin[] loadRates(String path, Gender gender, String year) throws Exception
    {
        /*
         * parse excel rows and fill them into the bins
         */
        List<Object> ages = loadAges(path, gender);
        List<Object> values = loadValues(path, gender, "" + year);

        List<Bin> list = new ArrayList<>();

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
                throw new Exception("Unexpected value type in mortaility rates value column");
            }

            if (sa.contains("-"))
            {
                String[] ss = sa.split("-");
                if (ss.length != 2)
                    throw new Exception("Unexpected value type in mortaility rates value column");
                int age_x1 = Integer.parseInt(ss[0]);
                int age_x2 = Integer.parseInt(ss[1]);
                list.add(new Bin(age_x1, age_x2, v));
            }
            else
            {
                int age_x = Integer.parseInt(sa);
                list.add(new Bin(age_x, age_x, v));
            }
        }

        Bin[] bins = Bins.bins(list);
        if (Bins.firstBin(bins).age_x1 != 0 || Bins.lastBin(bins).age_x2 != Population.MAX_AGE)
            throw new Exception("Invalid population age range");

        return bins;
    }

    /* ===================================================================================================== */

    public static Bin[] loadAgeQx(String filepath, Locality locality, Gender gender) throws Exception
    {
        String[] matchingSheetNames = new String[1];
        matchingSheetNames[0] = String.format("%s-%s", Util.properCase(locality.name()), Util.properCase(gender.name()));

        String[] matchingColumnNames = new String[1];
        matchingColumnNames[0] = "возраст";

        List<Object> ages = Excel.loadColumn(filepath, matchingSheetNames, matchingColumnNames);

        matchingColumnNames[0] = "qx";
        List<Object> values = Excel.loadColumn(filepath, matchingSheetNames, matchingColumnNames);
        
        List<Bin> list = new ArrayList<>();
        
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
                throw new Exception("Unexpected value type in mortaility rates value column");
            }

            if (sa.contains("-"))
            {
                String[] ss = sa.split("-");
                if (ss.length != 2)
                    throw new Exception("Unexpected value type in mortaility rates value column");
                int age_x1 = Integer.parseInt(ss[0]);
                int age_x2 = Integer.parseInt(ss[1]);
                list.add(new Bin(age_x1, age_x2, v));
            }
            else
            {
                int age_x = Integer.parseInt(sa);
                list.add(new Bin(age_x, age_x, v));
            }
        }        

        Bin[] bins = Bins.bins(list);
        if (Bins.firstBin(bins).age_x1 != 0 || Bins.lastBin(bins).age_x2 != Population.MAX_AGE)
            throw new Exception("Invalid population age range");

        return bins;
    }
}
