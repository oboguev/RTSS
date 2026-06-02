package rtss.tools.util;

import java.util.ArrayList;
import java.util.List;

public class SplineDataCommand
{
    public static class Command
    {
        public String method;
        public Double from;
        public Double to;
        public Double step;
        public Double offset;
        public double[] x;
        public double[] y;
    }

    public static Command parse(String text)
    {
        Command c = new Command();

        text = text.replace("\r", "");
        String[] lines = text.split("\n");

        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();

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
            {
                continue;
            }

            String[] tokens = line.split("\\s+");

            if (tokens.length >= 2 && tokens[0].equals("method"))
            {
                c.method = tokens[1];
            }
            else if (tokens.length >= 2 && tokens[0].equals("from"))
            {
                c.from = Double.parseDouble(tokens[1]);
            }
            else if (tokens.length >= 2 && tokens[0].equals("to"))
            {
                c.to = Double.parseDouble(tokens[1]);
            }
            else if (tokens.length >= 2 && tokens[0].equals("step"))
            {
                c.step = Double.parseDouble(tokens[1]);
            }
            else if (tokens.length >= 2 && tokens[0].equals("offset"))
            {
                c.offset = Double.parseDouble(tokens[1]);
            }
            else if (tokens.length >= 2)
            {
                double x = Double.parseDouble(tokens[0]);
                double y = Double.parseDouble(tokens[1]);
                xList.add(x);
                yList.add(y);
            }
            else
            {
                throw new IllegalArgumentException("Malformattted input line");
            }
        }

        c.x = new double[xList.size()];
        c.y = new double[yList.size()];
        for (int i = 0; i < xList.size(); i++)
        {
            c.x[i] = xList.get(i);
            c.y[i] = yList.get(i);
        }

        return c;
    }
}
