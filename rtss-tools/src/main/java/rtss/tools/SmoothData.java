package rtss.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rtss.math.algorithms.smooth.SmoothSeries;
import rtss.tools.util.SmoothDataCommand;
import rtss.util.Clipboard;
import rtss.util.Util;

/*
 * Сгладить ряд данных.
 * 
 * Входные данные на clipbaord -- последовательность строк: 
 *  
 *      method  CenteredMovingAverage | MedianThenAverage | Whittaker  # метод
 *      # опции -- см. ниже
 *      # данные (последовательность для сглаживания):
 *      x1 x2 x3
 *      x1 x2 x3
 *      x1 x2 x3
 *      ....
 *      
 * Опции:      
 * 
 *      method   CenteredMovingAverage
 *      window    5
 *      
 *      method    MedianThenAverage
 *      median-window   3
 *      average-window  5
 *      
 *      method     Whittaker
 *      lambda     50.0
 *      lambda-1   30.0
 *      lambda-2   100.0
 *      lambda-1   3
 *      lambda-2   5
 *      # кризисные годы (война, эпидемия)
 *      start-year 1900
 *      weight     1933 0.1
 *      weight     1941 0.2
 *      weight     1942 0.1
 *      weight     1942 0.3
 */
public class SmoothData
{
    public static void main(String[] args)
    {
        try
        {
            new SmoothData().do_main();
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
        SmoothDataCommand c = SmoothDataCommand.parse(text);

        List<double[]> list = new ArrayList<>();

        for (int ns = 0; ns < c.series.size(); ns++)
        {
            double[] series = c.series.get(ns);

            double[] va = null;

            switch (c.method)
            {
            case CenteredMovingAverage:
                va = SmoothSeries.smoothCenteredMovingAverage(series, c.window);
                break;

            case MedianThenAverage:
                va = SmoothSeries.smoothMedianThenAverage(series, c.medianWindow, c.averageWindow);
                break;

            case Whittaker:
                Double lambda = c.lambdas.get(ns + 1);
                if (lambda == null)
                    lambda = c.lambda;
                va = SmoothSeries.smoothWhittaker(series, lambda, c.weights);
                break;

            default:
                throw new IllegalArgumentException();
            }

            list.add(va);
        }

        int nrows = c.series.get(0).length;
        StringBuilder sb = new StringBuilder();

        for (int nr = 0; nr < nrows; nr++)
        {
            for (int ns = 0; ns < list.size(); ns++)
            {
                if (ns != 0)
                    sb.append(" ");
                double[] va = list.get(ns);
                double v = va[nr];
                sb.append(String.format("%.4f", v));
            }
                    
            sb.append("\n");
        }

        text = sb.toString();

        if (File.separatorChar == '\\')
            text = text.replace("\n", "\r\n");

        Clipboard.put(text);
    }
}
