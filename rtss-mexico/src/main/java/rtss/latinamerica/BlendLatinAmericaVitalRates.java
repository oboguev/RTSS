package rtss.latinamerica;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.math.interpolate.disaggregate.wcsasra.DecomposeCbrCdr;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class BlendLatinAmericaVitalRates
{
    private ExcelRC rc;

    public static void main(String[] args)
    {
        try
        {
            new BlendLatinAmericaVitalRates().do_main();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        rc = Excel.readSheet("Latin-America-Vital-Rates-Yearly.xlsx", false, "Все страны");

        do_country("Венесуэла", "Venezuela", new Glue(1950, 1959));
        do_country("Коста-Рика", "Costa Rica", new Glue(1950, 1959));
        do_country("Гватемала", "Guatemala", new Glue(1950, 1959));
        do_country("Колумбия", "Colombia", new Glue(1950, 1959));
        do_country("Эквадор", "Ecuador", new Glue(1950, 1959));
        do_country("Бразилия", "Brazil", new Skip());
        do_country("Парагвай", "Paraguay", new OnlyWpp());
        do_country("Перу", "Peru", new Glue(1950, 1959));
        // ### do_country("Боливия", "Bolivia");
        do_country("Сальвадор", "El Salvador", new Glue(1950, 1959));
        do_country("Чили", "Chile", new Glue(1950, 1959));
        do_country("Аргентина", "Argentina", new Glue(1950, 1959));
        // ### do_country("Мексика", "Mexico", 1960, 1970);
        do_country("Куба", "Cuba", new InterpolateGap());
        do_country("Панама", "Panama", new Glue(1950, 1959)); // но смертность проверить отдельно
        do_country("Доминиканская республика", "Dominican Republic", new OnlyWpp());
        // ### do_country("Гондурас", "Honduras");
        do_country("Уругвай", "Uruguay", new Glue(1950, 1964)); // но смертность проверить отдельно
        do_country("Аргентина-Ротман", "Argentina-Rothman", new Glue(1950, 1964));
    }

    private void do_country(String rname, String ename, Joiner joiner) throws Exception
    {
        int col = findCountryColumn(rname, ename);
        Bin[] cbrs = loadBins(col);
        Bin[] cdrs = loadBins(col + 1);

        DecomposeCbrCdr dc = new DecomposeCbrCdr();
        dc.smoothingSigma = 0.3;
        dc.maxInnerIterations = 500_000;

        if (cbrs.length != 0 && cdrs.length != 0)
        {
            dc.decompose(cbrs, cdrs);
        }
        else
        {
            dc.cbr = dc.cdr = new double[0];
        }

        double[] wcbr = loadWppRates(col, 1950, 2023);
        double[] wcdr = loadWppRates(col + 1, 1950, 2023);

        int hy1 = 0;
        int hy2 = 0;

        if (dc.cbr.length != 0)
        {
            hy1 = Bins.firstBin(cbrs).age_x1;
            hy2 = Bins.lastBin(cbrs).age_x2;
        }

        JoinerResult jr = joiner.join(hy1, hy2, dc.cbr, dc.cdr, 1950, 2023, wcbr, wcdr);
        
        // #### output
    }

    private int findCountryColumn(String rname, String ename) throws Exception
    {
        int ncols = rc.get(0).size();
        verify_eq("страна", rc.asString(0, 0));
        verify_eq("годы", rc.asString(1, 0));

        for (int col = 1; col <= ncols - 2; col += 2)
        {
            if (ename.equals(rc.asString(0, col)) &&
                null == rc.asString(0, col + 1) &&
                rname.equals(rc.asString(1, col)) &&
                null == rc.asString(1, col + 1))
            {
                return col;
            }

        }

        throw new Exception("Cannot locate country " + rname);
    }

    private void verify_eq(String s1, String s2) throws Exception
    {
        if (!s1.equals(s2))
            throw new Exception("unexpected cell value");
    }

    private static final Pattern patYYYY_YYYY = Pattern.compile("^(1\\d{3})-(1\\d{3})$");
    private static final Pattern patYYYY = Pattern.compile("^([12]\\d{3})$");

    private Bin[] loadBins(int col) throws Exception
    {
        List<Bin> list = new ArrayList<>();

        for (int nr = 2; nr <= rc.size(); nr++)
        {
            String ys = rc.asString(nr, 0);
            if (ys != null)
            {
                Matcher m = patYYYY_YYYY.matcher(ys);
                if (m.matches())
                {
                    int y1 = Integer.parseInt(m.group(1));
                    int y2 = Integer.parseInt(m.group(2));
                    Double v = rc.asDouble(nr, col);
                    if (v != null)
                        list.add(new Bin(y1, y2, v));
                }
            }
        }

        return Bins.bins(list);
    }

    private double[] loadWppRates(int col, int y1, int y2) throws Exception
    {
        Double[] result = new Double[y2 - y1 + 1];
        double[] r = new double[y2 - y1 + 1];

        for (int nr = 2; nr <= rc.size(); nr++)
        {
            String ys = rc.asString(nr, 0);
            if (ys != null)
            {
                if (ys.endsWith(".0"))
                    ys = Util.stripTail(ys, ".0");

                Matcher m = patYYYY.matcher(ys);
                if (m.matches())
                {
                    int y = Integer.parseInt(m.group(1));
                    if (y >= y1 && y <= y2)
                    {
                        result[y - y1] = rc.asRequiredDouble(nr, col);
                    }
                }
            }
        }

        for (int k = 0; k < result.length; k++)
            r[k] = result[k];

        return r;
    }

    public static class JoinerResult
    {
        public int ystart;
        public int yend;

        public double[] cbr;
        public double[] cdr;
    }

    public abstract static class Joiner
    {
        public abstract JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr) throws Exception;
    }

    public static class Glue extends Joiner
    {
        private final int y1;
        private final int y2;

        public Glue(int y1, int y2)
        {
            this.y1 = y1;
            this.y2 = y2;
        }

        @Override
        public JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr) throws Exception
        {
            // ####
            return null;
        }
    }

    public static class InterpolateGap extends Joiner
    {
        @Override
        public JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr) throws Exception
        {
            // ####
            return null;
        }
    }

    public static class Skip extends Joiner
    {
        @Override
        public JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr) throws Exception
        {
            // ####
            return null;
        }
    }

    public static class OnlyWpp extends Joiner
    {
        @Override
        public JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr) throws Exception
        {
            // ####
            return null;
        }
    }
}
