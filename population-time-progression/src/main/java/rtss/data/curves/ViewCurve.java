package rtss.data.curves;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.plot.ChartXYSplineAdvanced;

/**
 * Display a chart with bin and curve(s).
 * 
 * @args is a sequence of curves passed as double[] and optionally preceded by titles.
 * All curves must have the same length.
 */
public class ViewCurve
{
    public static void view(String chartTitle, Bin[] bins, Object... args) throws Exception
    {
        List<String> curveTitles = new ArrayList<>();
        List<double[]> curves = new ArrayList<>();

        String curveTitle = null;
        int cnum = 0;
        int xlen = -1;

        for (Object o : args)
        {
            if (o instanceof String)
            {
                curveTitle = (String) o;
            }
            else if (o instanceof double[])
            {
                cnum++;
                if (curveTitle == null)
                    curveTitle = "curve" + cnum;
                curveTitles.add(curveTitle);
                curveTitle = null;
                
                double[] y = (double[]) o;
                curves.add(y);

                if (xlen == -1)
                    xlen = y.length;
                else if (y.length != xlen)
                    throw new Exception("Curve lengths differ");
            }
        }

        int ywidth = Bins.widths_in_years(bins);
        int ppy = xlen / ywidth;
        if (ppy <= 0 || xlen != ppy * ywidth)
            throw new Exception("Curve length and bins length do not match");

        double[] xxx = Bins.ppy_x(bins, ppy);
        ChartXYSplineAdvanced chart = new ChartXYSplineAdvanced(chartTitle, "x", "y").showSplinePane(false);
        chart.addSeries("bins", xxx, Bins.ppy_y(bins, ppy));
        
        for (int k = 0; k < curves.size(); k++)
            chart.addSeries(curveTitles.get(k), xxx, curves.get(k));

        chart.display();
    }
}
