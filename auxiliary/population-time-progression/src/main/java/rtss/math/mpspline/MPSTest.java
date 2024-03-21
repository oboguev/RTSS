package rtss.math.mpspline;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import com.opencsv.CSVReader;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.selectors.Area;
import rtss.util.Util;

public class MPSTest
{
    public static void main(String[] args)
    {
        try
        {
            new MPSTest().do_main();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        List<Bin> binlist = new ArrayList<>();

        for (String[] sa : loadCensusSource(Area.RSFSR))
        {
            int age = Integer.parseInt(sa[0]);
            double avg = Double.parseDouble(sa[1]);

            Bin bin = new Bin(age, age, avg);
            binlist.add(bin);
        }

        Bin[] bins = Bins.bins(binlist);

        double[] yy = MeanPreservingIterativeSpline.eval(bins, 10);

        Util.noop();
    }

    private List<String[]> loadCensusSource(Area area) throws Exception
    {
        String s = Util.loadResource(String.format("ww2losses/%s_census_1959_data.txt", area.name()));
        s = removeComments(s);
        s = s.replace("\t", " ").replaceAll(" +", " ").replace(" ", ",");
        try (CSVReader reader = new CSVReader(new StringReader(s)))
        {
            return reader.readAll();
        }
    }

    private String removeComments(String rdata)
    {
        StringBuilder sb = new StringBuilder();

        rdata = rdata.replace("\r\n", "\n");
        for (String line : rdata.split("\n"))
        {
            String lt = line.trim();
            if (lt.startsWith("#") || lt.length() == 0)
                continue;
            sb.append(line);
            sb.append("\n");
        }

        return sb.toString();
    }
}
