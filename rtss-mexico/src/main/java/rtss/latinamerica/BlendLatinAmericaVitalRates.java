package rtss.latinamerica;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private Map<String, Double> xm = new HashMap<>();
    private List<String> Countries = new ArrayList<>();

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
        rc = Excel.readSheet("latinamerica/Latin-America-Vital-Rates-Yearly.xlsx", false, "Все страны");

        do_country("Венесуэла", "Venezuela", new Glue(1950, 1959));
        do_country("Коста-Рика", "Costa Rica", new Glue(1950, 1959));
        do_country("Гватемала", "Guatemala", new Glue(1950, 1959));
        do_country("Колумбия", "Colombia", new Glue(1950, 1959));
        do_country("Эквадор", "Ecuador", new Glue(1950, 1959));
        do_country("Бразилия", "Brazil", new Skip());
        do_country("Парагвай", "Paraguay", new OnlyWpp());
        do_country("Перу", "Peru", new Glue(1950, 1959));
        do_country("Боливия", "Bolivia", new Skip()); // ручная обработа
        do_country("Сальвадор", "El Salvador", new Glue(1950, 1959));
        do_country("Чили", "Chile", new Glue(1950, 1959));
        do_country("Аргентина", "Argentina", new Glue(1950, 1959));
        do_country("Мексика", "Mexico", new MexGlue(1960, 1969));
        do_country("Куба", "Cuba", new ImmediateJoin());
        do_country("Панама", "Panama", new Glue(1950, 1959)); // но смертность проверить отдельно
        do_country("Доминиканская республика", "Dominican Republic", new OnlyWpp());
        do_country("Гондурас", "Honduras", new Skip()); // ручная обработа
        do_country("Уругвай", "Uruguay", new Glue(1950, 1964)); // но смертность проверить отдельно
        do_country("Аргентина-Ротман", "Argentina-Rothman", new Glue(1950, 1964));

        do_output(1850, 2023);
    }

    private void do_country(String rname, String ename, Joiner joiner) throws Exception
    {
        Countries.add(rname);
        
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

        if (jr != null)
        {
            for (int y = jr.ystart; y <= jr.yend; y++)
                xm.put(rname + ".CBR." + y, jr.cbr[y - jr.ystart]);

            for (int y = jr.ystart; y <= jr.yend; y++)
                xm.put(rname + ".CDR." + y, jr.cdr[y - jr.ystart]);
        }
    }

    private void do_output(int y1, int y2)
    {
        StringBuilder sb = new StringBuilder();
        for (String rname : Countries)
        {
            if (sb.length() != 0)
                sb.append(",,");
            sb.append("\"" + rname + "\"");
        }
        Util.out("год," + sb.toString());
        
        for (int y = y1; y <= y2; y++)
        {
            sb = new StringBuilder("" + y);
            
            for (String rname : Countries)
            {
                Double cbr = xm.get(rname + ".CBR." + y);
                Double cdr = xm.get(rname + ".CDR." + y);

                sb.append(",");
                if (cbr != null)
                    sb.append(String.format("%.1f", cbr));
                
                sb.append(",");
                if (cbr != null)
                    sb.append(String.format("%.1f", cdr));
            }
            
            Util.out(sb.toString());
        }
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

    public abstract static class Joiner
    {
        /*
         * Соеединить историчесий участок за годы [hy1 ... hy2] с рождаемостью и смертностью hcbr/hcdr
         * и участок wpp за годы [wy1 ... wy2] с рождаемостью и смертностью wcbr/wcdr
         */
        public abstract JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr)
                throws Exception;
    }

    public static class JoinerResult
    {
        /*
         * Итог соединения: интервал [ystart ... yend]
         * с рождаемостью и смертностью cbr/cdr
         */
        public int ystart;
        public int yend;

        public double[] cbr;
        public double[] cdr;
    }

    public static class Glue extends Joiner
    {
        private final int gy1;
        private final int gy2;

        public Glue(int gy1, int gy2)
        {
            this.gy1 = gy1;
            this.gy2 = gy2;
        }

        @Override
        public JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr) throws Exception
        {
            if (hcdr.length != hcbr.length || hcbr.length != hy2 - hy1 + 1)
                throw new IllegalArgumentException("Incorrrect historical array");

            if (wcdr.length != wcbr.length || wcbr.length != wy2 - wy1 + 1)
                throw new IllegalArgumentException("Incorrrect WPP array");

            JoinerResult jr = new JoinerResult();

            if (gy2 < gy1)
                throw new IllegalArgumentException("Incorrect glue interval");

            if (gy1 < hy1 || gy2 > hy2)
                throw new IllegalArgumentException("Glue interval is outside historical interval");

            if (gy1 < wy1 || gy2 > wy2)
                throw new IllegalArgumentException("Glue interval is outside WPP interval");

            jr.ystart = hy1;
            jr.yend = wy2;
            jr.cbr = new double[jr.yend - jr.ystart + 1];
            jr.cdr = new double[jr.yend - jr.ystart + 1];

            /*
             * gy1 ... gy2 are blended years.
             * Pure WPP starts at gy2 + 1.
             *
             * For Glue(1950, 1959):
             *   1950 ... 1959 : smooth blend
             *   1960 ...      : WPP
             */
            final int pureWppYear = gy2 + 1;
            final double denom = pureWppYear - gy1;

            for (int y = jr.ystart; y <= jr.yend; y++)
            {
                int ix = y - jr.ystart;

                if (y < gy1)
                {
                    if (y < hy1 || y > hy2)
                        throw new IllegalArgumentException("Year is outside historical interval: " + y);

                    jr.cbr[ix] = hcbr[y - hy1];
                    jr.cdr[ix] = hcdr[y - hy1];
                }
                else if (y < pureWppYear)
                {
                    if (y < hy1 || y > hy2)
                        throw new IllegalArgumentException("Year is outside historical interval: " + y);

                    if (y < wy1 || y > wy2)
                        throw new IllegalArgumentException("Year is outside WPP interval: " + y);

                    double u = (y - gy1) / denom;
                    double u2 = u * u;
                    double u3 = u2 * u;
                    double w = u3 * (10.0 - 15.0 * u + 6.0 * u2);

                    double hCbr = hcbr[y - hy1];
                    double hCdr = hcdr[y - hy1];
                    double wCbr = wcbr[y - wy1];
                    double wCdr = wcdr[y - wy1];

                    jr.cbr[ix] = (1.0 - w) * hCbr + w * wCbr;
                    jr.cdr[ix] = (1.0 - w) * hCdr + w * wCdr;
                }
                else
                {
                    if (y < wy1 || y > wy2)
                        throw new IllegalArgumentException("Year is outside WPP interval: " + y);

                    jr.cbr[ix] = wcbr[y - wy1];
                    jr.cdr[ix] = wcdr[y - wy1];
                }
            }

            return jr;
        }
    }

    public static class MexGlue extends Joiner
    {
        private final int gy1;
        private final int gy2;

        public MexGlue(int gy1, int gy2)
        {
            this.gy1 = gy1;
            this.gy2 = gy2;
        }

        @Override
        public JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr) throws Exception
        {
            if (hcdr.length != hcbr.length || hcbr.length != hy2 - hy1 + 1)
                throw new IllegalArgumentException("Incorrrect historical array");

            if (wcdr.length != wcbr.length || wcbr.length != wy2 - wy1 + 1)
                throw new IllegalArgumentException("Incorrrect WPP array");

            JoinerResult jr = new JoinerResult();

            if (gy2 < gy1)
                throw new IllegalArgumentException("Incorrect Mexican transition interval");

            /*
             * For Mexico:
             *
             *   hy1 ... gy1-1 : historical series
             *   gy1 ... gy2   : WPP with a fading level correction
             *   gy2+1 ...     : pure WPP
             *
             * Example:
             *
             *   new MexGlue(1960, 1969)
             *
             * means:
             *
             *   ... 1959      : historical
             *   1960 ... 1969 : WPP adjusted to continue historical level,
             *                   with adjustment fading out smoothly
             *   1970 ...      : pure WPP
             */
            final int transitionStart = gy1;
            final int pureWppYear = gy2 + 1;
            final int lastHistoricalYear = gy1 - 1;

            if (lastHistoricalYear < hy1 || lastHistoricalYear > hy2)
                throw new IllegalArgumentException("Mexican last historical year is outside historical interval");

            if (transitionStart < wy1 || gy2 > wy2)
                throw new IllegalArgumentException("Mexican transition interval is outside WPP interval");

            jr.ystart = hy1;
            jr.yend = wy2;
            jr.cbr = new double[jr.yend - jr.ystart + 1];
            jr.cdr = new double[jr.yend - jr.ystart + 1];

            /*
             * Extrapolate the historical value one year forward.
             *
             * This defines the level to which WPP is matched at transitionStart.
             * If there is no previous historical year, use a flat extrapolation.
             */
            double lastHCbr = hcbr[lastHistoricalYear - hy1];
            double lastHCdr = hcdr[lastHistoricalYear - hy1];

            double prevHCbr;
            double prevHCdr;

            if (lastHistoricalYear - 1 >= hy1)
            {
                prevHCbr = hcbr[lastHistoricalYear - 1 - hy1];
                prevHCdr = hcdr[lastHistoricalYear - 1 - hy1];
            }
            else
            {
                prevHCbr = lastHCbr;
                prevHCdr = lastHCdr;
            }

            double projectedHCbr = lastHCbr + (lastHCbr - prevHCbr);
            double projectedHCdr = lastHCdr + (lastHCdr - prevHCdr);

            /*
             * Difference between WPP and the projected historical continuation
             * at the first transition year.
             *
             * During the transition this difference is subtracted from WPP and
             * then smoothly faded to zero.
             */
            double deltaCbr = wcbr[transitionStart - wy1] - projectedHCbr;
            double deltaCdr = wcdr[transitionStart - wy1] - projectedHCdr;

            final double denom = pureWppYear - transitionStart;

            for (int y = jr.ystart; y <= jr.yend; y++)
            {
                int ix = y - jr.ystart;

                if (y < transitionStart)
                {
                    if (y < hy1 || y > hy2)
                        throw new IllegalArgumentException("Year is outside historical interval: " + y);

                    jr.cbr[ix] = hcbr[y - hy1];
                    jr.cdr[ix] = hcdr[y - hy1];
                }
                else if (y < pureWppYear)
                {
                    if (y < wy1 || y > wy2)
                        throw new IllegalArgumentException("Year is outside WPP interval: " + y);

                    double u = (y - transitionStart) / denom;
                    double u2 = u * u;
                    double u3 = u2 * u;

                    /*
                     * Quintic smootherstep:
                     *
                     *   w(0) = 0
                     *   w(1) = 1
                     *
                     * with zero first and second derivatives at both ends.
                     */
                    double w = u3 * (10.0 - 15.0 * u + 6.0 * u2);

                    jr.cbr[ix] = wcbr[y - wy1] - deltaCbr * (1.0 - w);
                    jr.cdr[ix] = wcdr[y - wy1] - deltaCdr * (1.0 - w);
                }
                else
                {
                    if (y < wy1 || y > wy2)
                        throw new IllegalArgumentException("Year is outside WPP interval: " + y);

                    jr.cbr[ix] = wcbr[y - wy1];
                    jr.cdr[ix] = wcdr[y - wy1];
                }
            }
            return jr;
        }
    }

    public static class OnlyWpp extends Joiner
    {
        @Override
        public JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr) throws Exception
        {
            if (wcdr.length != wcbr.length || wcbr.length != wy2 - wy1 + 1)
                throw new IllegalArgumentException("Incorrrect WPP array");

            JoinerResult jr = new JoinerResult();
            jr.ystart = wy1;
            jr.yend = wy2;
            jr.cbr = Util.dup(wcbr);
            jr.cdr = Util.dup(wcdr);
            return jr;
        }
    }

    public static class ImmediateJoin extends Joiner
    {
        @Override
        public JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr) throws Exception
        {
            if (hcdr.length != hcbr.length || hcbr.length != hy2 - hy1 + 1)
                throw new IllegalArgumentException("Incorrrect historical array");

            if (wcdr.length != wcbr.length || wcbr.length != wy2 - wy1 + 1)
                throw new IllegalArgumentException("Incorrrect WPP array");

            if (wy1 != hy2 + 1)
                throw new IllegalArgumentException("Unexpected gap or overlap");

            JoinerResult jr = new JoinerResult();
            jr.ystart = hy1;
            jr.yend = wy2;
            jr.cbr = Util.concat(hcbr, wcbr);
            jr.cdr = Util.concat(hcdr, wcdr);
            return jr;
        }
    }

    public static class Skip extends Joiner
    {
        @Override
        public JoinerResult join(int hy1, int hy2, double[] hcbr, double[] hcdr, int wy1, int wy2, double[] wcbr, double[] wcdr) throws Exception
        {
            return null;
        }
    }
}
