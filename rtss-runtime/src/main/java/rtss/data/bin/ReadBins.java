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

    /**
     * Parse multiple series from a string.
     * Each line should have the same number of columns (year range + N values).
     * Returns a List where each element is a Bin[] for one series.
     *
     * Example input:
     * 1970-1974 1.0 2.0 3.0
     * 1975-1979 1.1 2.1 3.1
     * 1980 1.2 2.2 3.2
     *
     * This would return a List of 3 Bin[] arrays, one for each column of values.
     */
    public static List<Bin[]> fromStringMultiSeries(String binsAsString) throws Exception
    {
        List<List<Bin>> seriesList = null;
        int expectedColumnCount = -1;
        int lineNumber = 0;

        for (String line : binsAsString.replace("\r", "").split("\n"))
        {
            lineNumber++;

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

            if (tokens.size() < 2)
                throw new Exception("Invalid line " + lineNumber + ": " + line);

            // First non-empty line determines the number of series
            int numSeries = tokens.size() - 1;
            if (seriesList == null)
            {
                expectedColumnCount = numSeries;
                seriesList = new ArrayList<>();
                for (int i = 0; i < numSeries; i++)
                {
                    seriesList.add(new ArrayList<>());
                }
            }
            else if (numSeries != expectedColumnCount)
            {
                throw new Exception("Line " + lineNumber + " has " + numSeries + " value(s), expected " + expectedColumnCount + ": " + line);
            }

            // Parse the year range
            String s = tokens.get(0);
            int x1, x2;
            if (s.contains("-"))
            {
                String[] parts = s.trim().split("\\s*-\\s*");
                if (parts.length != 2)
                    throw new Exception("Invalid year range on line " + lineNumber + ": " + line);
                x1 = Integer.parseInt(parts[0]);
                x2 = Integer.parseInt(parts[1]);
            }
            else
            {
                x1 = x2 = Integer.valueOf(s);
            }

            // Parse each series value and add a Bin to the corresponding series
            for (int i = 0; i < numSeries; i++)
            {
                double value = Double.valueOf(tokens.get(i + 1).replace(",", ""));
                seriesList.get(i).add(new Bin(x1, x2, value));
            }
        }

        if (seriesList == null || seriesList.isEmpty())
        {
            throw new Exception("No data found in input");
        }

        // Convert each series from List<Bin> to Bin[]
        List<Bin[]> result = new ArrayList<>();
        for (List<Bin> series : seriesList)
        {
            result.add(Bins.bins(series));
        }

        return result;
    }
}
