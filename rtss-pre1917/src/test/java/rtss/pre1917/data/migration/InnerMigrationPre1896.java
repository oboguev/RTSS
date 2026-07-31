package rtss.pre1917.data.migration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.CurveVerifier;
import rtss.math.interpolate.disaggregate.csasra.DisaggregateVariableWidthSeries;
import rtss.pre1917.data.TerritoryNames;
import rtss.util.UI;
import rtss.util.Util;
import rtss.util.excel.Excel;
import rtss.util.excel.ExcelRC;

public class InnerMigrationPre1896
{
    public static void main(String[] args)
    {
        try
        {
            new InnerMigrationPre1896().do_main();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        ExcelRC rc = Excel.readSheet("inner-migration/1881-1895/inner-migration-1881-1895.xlsx", true, "data-outbound");

        List<Object> tnames = rc.columnValues("губерния");

        Map<String, List<Object>> in_values = new HashMap<>();
        in_values.put("1885-1892", rc.columnValues("1885-1892"));
        for (int year = 1893; year <= 1904; year++)
            in_values.put("" + year, rc.columnValues("" + year));

        StringBuilder sb = new StringBuilder("губерния");
        for (int year = 1885; year <= 1904; year++)
            sb.append(String.format(",%d", year));
        Util.out(sb.toString());

        for (int nr = 0; nr < tnames.size(); nr++)
        {
            String tname = ExcelRC.asString(tnames.get(nr));
            if (tname == null || tname.length() == 0)
                continue;
            if (tname.toLowerCase().startsWith("всего "))
                continue;
            tname = TerritoryNames.canonic(tname);

            process(tname, nr, in_values);
        }
    }

    private void process(String tname, int nr, Map<String, List<Object>> in_values) throws Exception
    {
        List<Bin> binlist = new ArrayList<>();
        add(binlist, tname, nr, in_values, 1885, 1892);
        for (int year = 1893; year <= 1904; year++)
            add(binlist, tname, nr, in_values, year, year);
        Bin[] bins = Bins.bins(binlist);

        for (Bin bin : bins)
            bin.avg /= bin.widths_in_years;

        double[] yvs = unbin(tname, bins);

        char quote = '"';
        StringBuilder sb = new StringBuilder(quote + tname + quote);
        for (int year = 1885; year <= 1904; year++)
            sb.append(String.format(",%d", Math.round(yvs[year - 1885])));
        Util.out(sb.toString());
    }

    private void add(List<Bin> binlist, String tname, int nr, Map<String, List<Object>> in_values, int y1, int y2) throws Exception
    {
        String key = (y1 == y2) ? "" + y1 : String.format("%d-%d", y1, y2);
        List<Object> values = in_values.get(key);
        String s = ExcelRC.asString(values.get(nr));
        double v = 0;

        if (s == null || s.equals("") || s.equals("—"))
        {
            // v = 0;
        }
        else
        {
            v = ExcelRC.asDouble(values.get(nr));
        }

        binlist.add(new Bin(y1, y2, v));
    }

    /* ============================================================================================================= */

    private double[] unbin(String tname, Bin[] bins)
    {
        Bin first = Bins.firstBin(bins);

        if (first.avg == 0)
        {
            List<Double> res = new ArrayList<>();
            for (int year = first.age_x1; year <= first.age_x2; year++)
                res.add(0.0);

            for (Bin bin = first.next; bin != null; bin = bin.next)
            {
                if (bin.age_x1 != bin.age_x2)
                    throw new IllegalArgumentException();
                res.add(bin.avg);
            }

            double[] v = res.stream()
                    .mapToDouble(Double::doubleValue)
                    .toArray();
            return v;
        }

        double[] v = unbin_csasra(bins);

        switch (tname)
        {
        case "Курская":
        case "Нижегородская":
            for (int year = first.age_x1; year <= first.age_x2; year++)
                v[year - first.age_x1] = first.avg;
            break;
        }

        return v;
    }

    private double[] unbin_csasra(Bin[] bins)
    {
        final double[] averages = Bins.midpoint_y(bins);

        final int[] intervalWidths = Bins.widths(bins);
        final int maxIterations = 1_000_000;
        final double positivityThreshold = 1e-6;
        final double maxAbsConvergenceDifference = 1e-3;
        final double maxRelConvergenceDifference = 1e-4;
        final double smoothingSigma = 1.0;
        final boolean linearizeFirstSegment = false;

        try
        {
            double[] yyy = DisaggregateVariableWidthSeries.disaggregate(averages,
                                                                        intervalWidths,
                                                                        maxIterations,
                                                                        smoothingSigma,
                                                                        positivityThreshold,
                                                                        maxAbsConvergenceDifference,
                                                                        maxRelConvergenceDifference,
                                                                        linearizeFirstSegment);

            if (!Util.isNonNegative(yyy))
                throw new Exception("Error calculating curve (negative value)");

            CurveVerifier.validate_means(yyy, bins);

            return yyy;
        }
        catch (Exception ex)
        {
            UI.messageBox("Unable to unbin with CSASRA: " + ex.getLocalizedMessage(), "Acknowledge");
            return null;
        }
    }
}
