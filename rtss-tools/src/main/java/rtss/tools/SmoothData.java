package rtss.tools;

import java.io.File;

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
 *      x1
 *      x2
 *      x3
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

        double[] va = null;

        switch (c.method)
        {
        case CenteredMovingAverage:
            va = SmoothSeries.smoothCenteredMovingAverage(c.data, c.window);
            break;

        case MedianThenAverage:
            va = SmoothSeries.smoothMedianThenAverage(c.data, c.medianWindow, c.averageWindow);
            break;

        case Whittaker:
            va = SmoothSeries.smoothWhittaker(c.data, c.lambda, c.weights);
            break;

        default:
            throw new IllegalArgumentException();
        }

        StringBuilder sb = new StringBuilder();

        for (double v : va)
        {
            if (sb.length() != 0)
                sb.append("\n");
            sb.append(String.format("%.4f", v));
        }

        text = sb.toString();

        if (File.separatorChar == '\\')
            text = text.replace("\n", "\r\n");

        Clipboard.put(text);
    }
}
