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
        public List<Double> x = new ArrayList<>();
        public List<List<Double>> y = new ArrayList<>();
    }

    public static Command parse(String text)
    {
        Command c = new Command();

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
                double vx = Double.parseDouble(tokens[0]);
                c.x.add(vx);
                
                List<Double> yy = new ArrayList<>();
                
                for (int nt = 1; nt < tokens.length; nt++)
                {
                    double y = Double.parseDouble(tokens[nt].replace(",", ""));
                    yy.add(y);
                }
                
                c.y.add(yy);
            }
            else
            {
                throw new IllegalArgumentException("Malformattted input line");
            }
        }
        
        if (c.y.size() == 0)
            throw new IllegalArgumentException("Missing data");
        
        int nys0 = c.y.get(0).size();
        if (nys0 == 0)
            throw new IllegalArgumentException("Missing data");

        for (int nr = 0; nr < c.y.size(); nr++)
        {
            int nys = c.y.get(nr).size();
            if (nys != nys0) 
                throw new IllegalArgumentException("Изменяющееся число колонок");
        }

        return c;
    }
}
