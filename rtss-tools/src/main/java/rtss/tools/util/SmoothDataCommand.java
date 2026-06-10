package rtss.tools.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmoothDataCommand
{
    public SmoothingMethod method;
    public int window = 5;
    public int medianWindow = 3;
    public int averageWindow = 5;
    public double lambda = 50.0;

    public int startyear = 0;
    public double[] data = null;
    public double[] weights = null;

    private Map<Integer, Double> mweights = new HashMap<>();
    private List<Double> datalist = new ArrayList<>();

    public static enum SmoothingMethod
    {
        CenteredMovingAverage, MedianThenAverage, Whittaker
    }

    public static SmoothDataCommand parse(String text) throws Exception
    {
        SmoothDataCommand c = new SmoothDataCommand();

        text = text.replace("\r", "");
        String[] lines = text.split("\n");

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
            else if (tokens.length == 3 && tokens[0].equals("weight"))
            {
                int y = Integer.parseInt(tokens[1]);
                double w = Double.parseDouble(tokens[1]);
                c.mweights.put(y, w);
            }
            else if (tokens.length == 1)
            {
                double v = Double.parseDouble(tokens[0]);
                c.datalist.add(v);
            }
            else
            {
                throw new IllegalArgumentException("Malformattted input line");
            }
        }

        if (c.datalist.size() == 0)
            throw new IllegalArgumentException("Missing data");

        c.data = c.datalist.stream()
                .mapToDouble(Double::doubleValue)
                .toArray();

        if (c.mweights.size() != 0)
        {
            c.weights = new double[c.data.length];
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
