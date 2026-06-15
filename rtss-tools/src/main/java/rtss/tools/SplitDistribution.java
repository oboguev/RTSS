package rtss.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.CurveVerifier;
import rtss.math.interpolate.disaggregate.csasra.DisaggregateVariableWidthSeries;
import rtss.util.Clipboard;
import rtss.util.UI;
import rtss.util.Util;

/*
 * Disaggregate data expressed as sum per bin,
 * such as population per town size range.
 * 
 * Then split it at indicated points and reaggregate. 
 * 
 * Input data on the clipboard is a sequence of lines:
 * 
 *      split x
 *      .... 
 *      split x 
 *  
 *      x-x  y
 *      x-x  y
 *      x    y
 *      x    y
 *      .... 
 *      x-x  y
 *      x-x  y
 *  
 * First part of a line can be a range of a single-year value (bin width = 1).
 * 
 *  Example:
 *  
 *      split 1000
 *      split 2000
 *      split 2500
 *  
 *      1-2,999         970,800
 *      3,000-4,999     2,312,878
 *      5,000-9,999     5,385,703
 */
public class SplitDistribution
{
    private List<Integer> splits = new ArrayList<>();
    private final String nl = "\n";

    public static void main(String[] args)
    {
        try
        {
            new SplitDistribution().do_main();
            Util.out("*** Result was placed on the clipboard.");
        }
        catch (Throwable ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void do_main() throws Exception
    {
        String text = Clipboard.getText();
        if (text == null || text.length() == 0)
            throw new Exception("No data on the clipboard");
        text = extractSplits(text);

        List<Bin[]> binlist = Bins.fromStringMultiSeries(text);

        /*
         * Reduce to averages
         */
        for (Bin[] bins : binlist)
        {
            for (Bin bin : bins)
                bin.avg /= bin.widths_in_years;
        }

        StringBuilder sb = new StringBuilder();
        do_main_multi(sb, binlist);
        text = sb.toString();

        if (File.separatorChar == '\\')
            text = text.replace("\n", "\r\n");

        Clipboard.put(text);
    }

    private String extractSplits(String text) throws Exception
    {
        StringBuilder sb = new StringBuilder();

        for (String line : text.replace("\r\n", "\n").split("\n"))
        {
            line = Util.stripComment(line);
            line = Util.despace(line);
            if (line.isEmpty())
                continue;

            if (line.startsWith("split "))
            {
                String sv = line.substring("split ".length());
                splits.add(Integer.parseInt(sv));
            }
            else
            {
                sb.append(line + nl);
            }
        }

        return sb.toString();
    }

    /* ============================================================================================================= */

    private double[] unbin_csasra(Bin[] bins)
    {
        // final int ppy = 1;
        // final double[] xxx = Bins.ppy_x(bins, ppy);
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

    /* ============================================================================================================= */

    private void do_main_multi(StringBuilder sb, List<Bin[]> binlist) throws Exception
    {
        List<Bin[]> rebinlist = new ArrayList<>();

        for (Bin[] bins : binlist)
        {
            double[] yy = null;

            yy = unbin_csasra(bins);

            Bin[] rebin = Bins.splitBinStructure(bins, splits);
            rebin_sum(rebin, yy);

            rebinlist.add(rebin);
        }

        Bin[] srcbins0 = binlist.get(0);
        Bin[] bins0 = rebinlist.get(0);

        if (Bins.firstBin(srcbins0).age_x1 != Bins.firstBin(bins0).age_x1)
            throw new Exception("Mismatching bin structure");

        if (Bins.lastBin(srcbins0).age_x2 != Bins.lastBin(bins0).age_x2)
            throw new Exception("Mismatching bin structure");

        for (int k = 1; k < rebinlist.size(); k++)
        {
            Bin[] bins2 = rebinlist.get(0);

            if (bins0.length != bins2.length)
                throw new Exception("Mismatching bin structure");

            for (int j = 0; j < bins0.length; j++)
            {
                if (bins0[j].age_x1 != bins2[j].age_x1)
                    throw new Exception("Mismatching bin structure");

                if (bins0[j].age_x2 != bins2[j].age_x2)
                    throw new Exception("Mismatching bin structure");
            }
        }
        
        for (Bin[] bins : binlist)
        {
            for (Bin bin : bins)
                bin.avg *= bin.widths_in_years;
        }

        for (int nc = 0; nc < rebinlist.size(); nc++)
        {
            Util.checkSame(sum(binlist.get(nc)), sum(rebinlist.get(nc)));
        }

        for (int nr = 0; nr < bins0.length; nr++)
        {
            sb.append(String.format("%d %d", bins0[nr].age_x1, bins0[nr].age_x2));
            for (int nc = 0; nc < rebinlist.size(); nc++)
            {
                sb.append(String.format(" %.4f", rebinlist.get(nc)[nr].avg));
            }
            sb.append(nl);
        }
    }

    private void rebin_sum(Bin[] bins, double[] yy) throws Exception
    {
        if (yy.length != Bins.widths_in_years(bins))
            throw new Exception("Mismatched lengths");

        int x0 = Bins.firstBin(bins).age_x1;

        for (Bin bin : bins)
        {
            bin.avg = 0;

            for (int x = bin.age_x1; x <= bin.age_x2; x++)
                bin.avg += yy[x - x0];
        }
    }

    private double sum(Bin[] bins) throws Exception
    {
        double v = 0;
        for (Bin bin : bins)
            v += bin.avg;
        return v;
    }
}
