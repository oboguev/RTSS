package rtss.tools.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.util.Util;

public class SmoothDataCommand
{
    public SmoothingMethod method = SmoothingMethod.MedianThenAverage;
    public int window = 5;
    public int medianWindow = 3;
    public int averageWindow = 5;
    public double lambda = 50.0;
    public Map<Integer,Double> lambdas = new HashMap<>();

    public int startyear = 0;
    public List<double[]> series = new ArrayList<>();
    public double[] weights = null;

    private Map<Integer, Double> mweights = new HashMap<>();
    private List<List<Double>> rows = new ArrayList<>();

    public static enum SmoothingMethod
    {
        CenteredMovingAverage, MedianThenAverage, Whittaker
    }

    public static SmoothDataCommand parse(String text) throws Exception
    {
        SmoothDataCommand c = new SmoothDataCommand();

        text = text.replace("\r", "");
        String[] lines = text.split("\n");
        
        Integer dataColumnCount = null;

        for (String line : lines)
        {
            line = line.replaceAll("\\s+", " ");

            int commentIndex = line.indexOf('#');
            if (commentIndex != -1)
            {
                line = line.substring(0, commentIndex);
            }

            line = line.trim();

            if (line.isEmpty())
                continue;

            String[] tokens = line.split("\\s+");

            if (tokens.length == 2 && tokens[0].equals("method"))
            {
                c.method = SmoothingMethod.valueOf(tokens[1]);
            }
            else if (tokens.length == 2 && tokens[0].equals("window"))
            {
                c.window = Integer.parseInt(tokens[1]);
            }
            else if (tokens.length == 2 && tokens[0].equals("median-window"))
            {
                c.medianWindow = Integer.parseInt(tokens[1]);
            }
            else if (tokens.length == 2 && tokens[0].equals("average-window"))
            {
                c.averageWindow = Integer.parseInt(tokens[1]);
            }
            else if (tokens.length == 2 && tokens[0].equals("startyear"))
            {
                c.startyear = Integer.parseInt(tokens[1]);
            }
            else if (tokens.length == 2 && tokens[0].equals("start-year"))
            {
                c.startyear = Integer.parseInt(tokens[1]);
            }
            else if (tokens.length == 2 && tokens[0].equals("lambda"))
            {
                c.lambda = Double.parseDouble(tokens[1]);
            }
            else if (tokens.length == 2 && tokens[0].startsWith("lambda-"))
            {
                String key = Util.stripStart(tokens[0], "lambda-");
                int ix = Integer.parseInt(key);
                double xlambda = Double.parseDouble(tokens[1]);
                c.lambdas.put(ix, xlambda);
            }
            else if (tokens.length == 3 && tokens[0].equals("weight"))
            {
                int y = Integer.parseInt(tokens[1]);
                double w = Double.parseDouble(tokens[2]);
                c.mweights.put(y, w);
            }
            else if (tokens.length >= 1)
            {
                int ncols = tokens.length ;
                if (dataColumnCount == null)
                    dataColumnCount = ncols;
                if (dataColumnCount != ncols)
                    throw new IllegalArgumentException("Непостоянное число колонок данных");
                List<Double> list = new ArrayList<>();
                for (int nc = 0; nc < ncols; nc++)
                {
                    double v = Double.parseDouble(tokens[nc]);
                    list.add(v);
                }
                    
                c.rows.add(list);
            }
            else
            {
                throw new IllegalArgumentException("Malformattted input line");
            }
        }

        if (c.rows.size() == 0)
            throw new IllegalArgumentException("Missing data");
        
        /* ------------------------------------------------------ */
        
        int nr = c.rows.size();        // число строк
        int ns = c.rows.get(0).size(); // число серий: x, y, z, ...

        // создать массивы для серий
        for (int k = 0; k < ns; k++)
            c.series.add(new double[nr]);

        // транспонировать rows -> series
        for (int r = 0; r < nr; r++)
        {
            List<Double> row = c.rows.get(r);

            if (row.size() != ns)
                throw new Exception("Inconsistent row length at row " + r);

            for (int k = 0; k < ns; k++)
            {
                Double v = row.get(k);

                if (v == null)
                    throw new Exception("Null value at row " + r + ", column " + k);

                c.series.get(k)[r] = v;
            }
        }
        
        /* ------------------------------------------------------ */

        if (c.mweights.size() != 0)
        {
            c.weights = new double[c.series.get(0).length];
            Arrays.fill(c.weights, 1.0);
            for (int year : c.mweights.keySet())
            {
                int ix = year - c.startyear;
                c.weights[ix] = c.mweights.get(year);
            }
        }

        return c;
    }
}
