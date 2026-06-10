package rtss.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import rtss.math.interpolate.ConstrainedCubicSplineInterpolator;
import rtss.math.interpolate.FunctionRangeExtenderDirect;
import rtss.math.interpolate.SteffenSplineInterpolator;
import rtss.tools.util.SplineDataCommand;
import rtss.tools.util.SplineDataCommand.Command;
import rtss.util.Clipboard;
import rtss.util.Util;

/*
 * Интерполировать данные сплайном.
 * 
 * Входные данные на clipbaord -- последовательность строк: 
 *  
 *      method    ConstrainedCubicSplineInterpolator   # тип сплайна
 *      from  1900     # начальное значение x для интерполяции
 *      to    2000     # конечное значение x для интерполяции
 *      step   1.0     # шаг по x
 *      offset 1.0     # сдвиг по x
 *      x y
 *      x y
 *      x y
 */
public class SplineData
{
    public static void main(String[] args)
    {
        try
        {
            new SplineData().do_main();
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
        Command c = SplineDataCommand.parse(text);

        if (c.from == null)
            c.from = c.x.get(0);

        if (c.to == null)
            c.to = c.x.get(c.x.size() - 1);

        if (c.step == null)
            c.step = 1.0;

        if (c.method == null)
            c.method = "ConstrainedCubicSplineInterpolator";

        if (c.offset == null)
            c.offset = 0.0;

        int nys = c.y.get(0).size();
        List<UnivariateFunction> ff = new ArrayList<>();

        for (int ny = 0; ny < nys; ny++)
        {
            PolynomialSplineFunction sp = null;
            UnivariateFunction f = null;

            switch (c.method)
            {
            case "ConstrainedCubicSplineInterpolator":
                sp = new ConstrainedCubicSplineInterpolator().interpolate(l2a(c.x), l2a(c.y, ny));
                f = new FunctionRangeExtenderDirect(sp);
                break;

            case "AkimaSplineInterpolator":
                sp = new AkimaSplineInterpolator().interpolate(l2a(c.x), l2a(c.y, ny));
                f = new FunctionRangeExtenderDirect(sp);
                break;

            case "SteffenSplineInterpolator":
                sp = new SteffenSplineInterpolator().interpolate(l2a(c.x), l2a(c.y, ny));
                f = new FunctionRangeExtenderDirect(sp);
                break;

            default:
                throw new IllegalArgumentException("Invalid spline method: " + c.method);
            }
            
            ff.add(f);
        }

        StringBuilder sb = new StringBuilder();

        for (double x = c.from + c.offset; x <= c.to; x += c.step)
        {
            sb.append(String.format("%s", f2s(x)));
            for (int ny = 0; ny < nys; ny++)
            {
                double y = ff.get(ny).value(x);
                sb.append(String.format(" %.4f", y));
            }
            sb.append("\n");
        }

        text = sb.toString();

        if (File.separatorChar == '\\')
            text = text.replace("\n", "\r\n");

        Clipboard.put(text);
    }

    private String f2s(double v) throws Exception
    {
        String s = String.format("%s", v);
        if (s.endsWith(".0"))
            s = Util.stripTail(s, ".0");
        return s;
    }

    private double[] l2a(List<Double> list)
    {
        double[] a = new double[list.size()];
        for (int k = 0; k < list.size(); k++)
            a[k] = list.get(k);
        return a;
    }

    private double[] l2a(List<List<Double>> list, int ny)
    {
        double[] a = new double[list.size()];
        for (int k = 0; k < list.size(); k++)
            a[k] = list.get(k).get(ny);
        return a;
    }
}
