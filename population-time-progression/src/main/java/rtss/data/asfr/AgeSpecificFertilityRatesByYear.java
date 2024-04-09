package rtss.data.asfr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.selectors.Area;
import rtss.util.excel.Excel;

public class AgeSpecificFertilityRatesByYear
{
    private Map<Integer, AgeSpecificFertilityRates> m = new HashMap<>();

    private AgeSpecificFertilityRatesByYear(String path) throws Exception
    {
        do_load(path);
    }

    public void setForYear(int year, AgeSpecificFertilityRates asfr)
    {
        if (m.containsKey(year))
            throw new IllegalArgumentException();
        m.put(year, asfr);
    }

    public AgeSpecificFertilityRates getForYear(int year)
    {
        return m.get(year);
    }

    public static AgeSpecificFertilityRatesByYear load(Area area) throws Exception
    {
        String path = String.format("age_specific_fertility_rates/%s/%s-ASFR.xlsx", area.name(), area.name());
        return load(path);
    }

    public static AgeSpecificFertilityRatesByYear load(String path) throws Exception
    {
        return new AgeSpecificFertilityRatesByYear(path);
    }

    /* ============================================================================================== */

    private int ncol;
    private int nrow;
    private String path;

    private void do_load(String path) throws Exception
    {
        this.path = path;

        List<List<Object>> rows = Excel.readSheet(path, "Data");

        nrow = 0;
        int years_row = -1;
        List<Integer> age_x1 = new ArrayList<>();
        List<Integer> age_x2 = new ArrayList<>();

        for (List<Object> row : rows)
        {
            if (years_row == -1 && row.size() != 0 && row.get(0) instanceof String)
            {
                String s = (String) row.get(0);
                if (s.trim().toLowerCase().equals("год"))
                {
                    years_row = nrow;
                    parse_years(row, 1, age_x1, age_x2);
                }
            }
            else if (years_row != -1 && nrow > years_row)
            {
                parse_data_row(row, age_x1, age_x2);
            }

            nrow++;
        }
    }

    private void parse_years(List<Object> row, int begin_ncol, List<Integer> age_x1, List<Integer> age_x2) throws Exception
    {
        for (ncol = begin_ncol; ncol < row.size(); ncol++)
        {
            Object o = row.get(ncol);
            if (!(o instanceof String))
                invalidCell();

            String s = (String) o;
            s = s.replace("–", "-").replace("‒", "-").replace("—", "-").replace("—", "-");
            String[] sa = s.split("-");

            if (sa.length != 2)
                invalidCell();

            try
            {
                age_x1.add(Integer.parseInt(sa[0]));
                age_x2.add(Integer.parseInt(sa[1]));
            }
            catch (Exception ex)
            {
                invalidCell(ex);
            }
        }
    }

    private void parse_data_row(List<Object> row, List<Integer> age_x1, List<Integer> age_x2) throws Exception
    {
        if (row.size() != age_x1.size() + 1)
        {
            String msg = String.format("Invalid row in resource file %s, row=%d, incorrect length", path, nrow + 1);
            throw new Exception(msg);
        }
        
        int year = asInteger(row, 0);
        List<Bin> list = new ArrayList<Bin>();
        
        for (int k = 0; k < age_x1.size(); k++)
            list.add(new Bin(age_x1.get(k), age_x2.get(k), asDouble(row, k + 1)));
        
        Bin[] bins = Bins.bins(list);
        
        if (m.containsKey(year))
        {
            String msg = String.format("Invalid row in resource file %s, row=%d, duplicate year", path, nrow + 1);
            throw new Exception(msg);
        }
        
        m.put(year, new AgeSpecificFertilityRates(bins));
    }

    private int asInteger(List<Object> row, int nc) throws Exception
    {
        ncol = nc;
        Object o = row.get(nc);
        double d = 0;
        
        if (o instanceof Double)
        {
            d = (Double) o;
        }
        else if (o instanceof String)
        {
            String s = (String) o;
            try
            {
                d = Double.parseDouble(s.trim());
            }
            catch (Exception ex)
            {
                invalidCell(ex);
            }
        }
        else
        {
            invalidCell();
        }
        
        long xl = Math.round(d);
        if (xl < Integer.MIN_VALUE || xl > Integer.MAX_VALUE)
            invalidCell();
        
        if (Math.abs(xl - d) > 0.0001)
            invalidCell();

        return (int) xl;
    }

    private double asDouble(List<Object> row, int nc) throws Exception
    {
        ncol = nc;
        Object o = row.get(nc);
        double d = 0;
        
        if (o instanceof Double)
        {
            d = (Double) o;
        }
        else if (o instanceof String)
        {
            String s = (String) o;
            try
            {
                d = Double.parseDouble(s.trim());
            }
            catch (Exception ex)
            {
                invalidCell(ex);
            }
        }
        else
        {
            invalidCell();
        }
        
        return d;
    }

    private void invalidCell() throws Exception
    {
        invalidCell(null);
    }

    private void invalidCell(Throwable t) throws Exception
    {
        String msg = String.format("Invalid cell value in resource file %s, row=%d, col=%d", path, nrow + 1, ncol + 1);
        if (t == null)
            throw new Exception(msg);
        else
            throw new Exception(msg, t);
    }
}
