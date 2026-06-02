package rtss.tools;

import java.io.File;

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
 *      method    ConstrainedCubicSplineInterpolator   № тип сплайна
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
            c.from = c.x[0];

        if (c.to == null)
            c.to = c.x[c.x.length - 1];

        if (c.step == null)
            c.step = 1.0;

        if (c.method == null)
            c.method = "ConstrainedCubicSplineInterpolator";

        if (c.offset == null)
            c.offset = 0.0;

        PolynomialSplineFunction sp = null;
        UnivariateFunction f = null;

        switch (c.method)
        {
        case "ConstrainedCubicSplineInterpolator":
            sp = new ConstrainedCubicSplineInterpolator().interpolate(c.x, c.y);
            f = new FunctionRangeExtenderDirect(sp);
            break;

        case "AkimaSplineInterpolator":
            sp = new AkimaSplineInterpolator().interpolate(c.x, c.y);
            f = new FunctionRangeExtenderDirect(sp);
            break;

        case "SteffenSplineInterpolator":
            sp = new SteffenSplineInterpolator().interpolate(c.x, c.y);
            f = new FunctionRangeExtenderDirect(sp);
            break;

        default:
            throw new IllegalArgumentException("Invalid spline method: " + c.method);
        }

        StringBuilder sb = new StringBuilder();

        for (double x = c.from + c.offset; x <= c.to; x += c.step)
        {
            double y = f.value(x);
            sb.append(String.format("%s %.4f\n", f2s(x), y));
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
}
