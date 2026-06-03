package rtss.tools.util;

import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;

public class UnbinCbrCdrCommand
{
    public static class Command
    {
        public Double sigma;
        public List<Bin[]> binlist;
    }

    public static Command parse(String text) throws Exception
    {
        Command c = new Command();

        text = text.replace("\r", "");
        String[] lines = text.split("\n");
        
        StringBuilder sb = new StringBuilder(); 

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

            if (tokens.length >= 2 && tokens[0].equals("sigma"))
            {
                c.sigma = Double.parseDouble(tokens[1]);
            }
            else 
            {
                sb.append(line + "\n");
            }
        }
        
        c.binlist = Bins.fromStringMultiSeries(sb.toString());
        
        return c;
    }
}
