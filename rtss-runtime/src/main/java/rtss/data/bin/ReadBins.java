package rtss.data.bin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rtss.util.Util;

public class ReadBins
{
    public static Bin[] fromString(String binsAsString) throws Exception
    {
        List<Bin> bins = new ArrayList<>();

        for (String line : binsAsString.replace("\r", "").split("\n"))
        {
            // strip comment
            int commentPos = line.indexOf('#');
            if (commentPos >= 0)
                line = line.substring(0, commentPos);

            // despace
            line = Util.despace(line);
            if (line.isEmpty())
                continue;

            // break into tokens
            List<String> tokens;
            if (line.trim().contains(" "))
                tokens = Arrays.asList(line.trim().split("[ ]+"));
            else
                tokens = Arrays.asList(line.trim().split("[ ,]+"));

            if (tokens.size() != 2)
                throw new Exception("Invalid line: " + line);

            double value = Double.valueOf(tokens.get(1).replace(",", ""));
            String s = tokens.get(0);
            if (s.contains("-"))
            {
                String[] parts = s.trim().split("\\s*-\\s*");
                if (parts.length != 2)
                    throw new Exception("Invalid line: " + line);
                int x1 = Integer.parseInt(parts[0]);
                int x2 = Integer.parseInt(parts[1]);
                bins.add(new Bin(x1, x2, value));
            }
            else
            {
                int x = Integer.valueOf(s);
                bins.add(new Bin(x, x, value));
            }
        }

        return Bins.bins(bins);
    }
}
