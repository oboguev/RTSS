package rtss.tools;

import java.io.File;

import rtss.util.Clipboard;
import rtss.util.Util;

/*
 * Расколоть корзину распределения жителей по городам
 * 
 * Входныке значения (одной строкой или в разных строках): 
 *  
 *    населённость города от (включительно)
 *    населённость города до (включительно)
 *    число городов (населённых пунктов)
 *    общее число жителей в этой категории населённых пунктов
 *    раскол по (населённость города для раскола) 
 */
public class SplitTownPopulation
{
    public static void main(String[] args)
    {
        try
        {
            new SplitTownPopulation().do_main();
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
        text = Util.stripMultiLineComment(text);
        text = Util.despace(text);
        String[] tokens = text.split(" ");
        if (tokens.length != 5)
            throw new Exception("Invalid arguments");

        if (tokens[0].equals("менее"))
            tokens[0] = "1";

        Integer town_pop_range_1 = Integer.parseInt(tokens[0].replace(",", ""));
        Integer town_pop_range_2 = Integer.parseInt(tokens[1].replace(",", ""));
        Integer num_towns = Integer.parseInt(tokens[2].replace(",", ""));
        Integer sum_population = Integer.parseInt(tokens[3].replace(",", ""));
        Integer split = Integer.parseInt(tokens[4].replace(",", ""));

        int incr = 0;
        if ((town_pop_range_2 % 10) == 0)
        {
            town_pop_range_2--;
            incr++;
        }

        Result r = split(town_pop_range_1, town_pop_range_2, split, num_towns, sum_population);

        Util.checkSame(r.belowPlaces + r.abovePlaces, num_towns);
        Util.checkSame(r.belowPopulation + r.abovePopulation, sum_population);
        
        StringBuilder sb = new StringBuilder(); 

        sb.append(String.format("%,d %,d %,d %,d", r.x1, r.threshold - 1 + incr, Math.round(r.belowPlaces), Math.round(r.belowPopulation)));
        sb.append("\n");
        sb.append(String.format("%,d %,d %,d %,d", r.threshold, r.x2 + incr, Math.round(r.abovePlaces), Math.round(r.abovePopulation)));
        sb.append("\n");
        
        text = sb.toString();
        
        Util.out(text);
        
        if (File.separatorChar == '\\')
            text = text.replace("\n", "\r\n");

        Clipboard.put(text);
        
        Util.out("");
        Util.out("*** Result was was placed on the clipboard.");
    }

    /* ================================================================= */

    public static final class Result
    {
        public final int x1;
        public final int x2;
        public final int threshold;

        public final double belowPlaces;
        public final double belowPopulation;

        public final double abovePlaces;
        public final double abovePopulation;

        public final double beta;

        private Result(int x1, int x2, int threshold,
                double belowPlaces, double belowPopulation,
                double abovePlaces, double abovePopulation,
                double beta)
        {
            this.x1 = x1;
            this.x2 = x2;
            this.threshold = threshold;
            this.belowPlaces = belowPlaces;
            this.belowPopulation = belowPopulation;
            this.abovePlaces = abovePlaces;
            this.abovePopulation = abovePopulation;
            this.beta = beta;
        }
    }

    /*
     * Splits bin [x1, x2] into:
     *
     *   below: [x1, threshold - 1]
     *   above: [threshold, x2]
     *
     * The distribution inside the source bin is assumed to be:
     *
     *   f(x) = A * x^(-alpha)
     *
     * with alpha chosen so that:
     *
     *   sum f(x)     = places
     *   sum x * f(x) = population
     */
    public static Result split(int x1, int x2, int threshold,
            double places, double population) throws Exception
    {
        if (x1 <= 0)
            throw new Exception("Power-law split requires x1 > 0");

        if (x1 > x2)
            throw new Exception("Invalid bin range");

        if (places < 0 || population < 0)
            throw new Exception("Negative input value");

        if (places == 0)
        {
            if (population != 0)
                throw new Exception("Population is non-zero while places is zero");

            return new Result(x1, x2, threshold, 0, 0, 0, 0, 0);
        }

        double mean = population / places;

        if (mean < x1 || mean > x2)
            throw new Exception("Mean settlement size is outside the bin range");

        if (threshold <= x1)
            return new Result(x1, x2, threshold, 0, 0, places, population, 0);

        if (threshold > x2)
            return new Result(x1, x2, threshold, places, population, 0, 0, 0);

        double alpha = findAlpha(x1, x2, mean);

        Sums total = sums(x1, x2, alpha);
        Sums above = sums(threshold, x2, alpha);

        double scale = places / total.z;

        double abovePlaces = scale * above.z;
        double abovePopulation = scale * above.zx;

        double belowPlaces = places - abovePlaces;
        double belowPopulation = population - abovePopulation;

        return new Result(x1, x2, threshold,
                          belowPlaces, belowPopulation,
                          abovePlaces, abovePopulation,
                          alpha);
    }

    private static double findAlpha(int x1, int x2, double targetMean)
    {
        double meanAtZero = (x1 + x2) / 2.0;

        if (Math.abs(targetMean - meanAtZero) < 1e-12)
            return 0.0;

        double lo = -1.0;
        double hi = 1.0;

        while (mean(x1, x2, lo) < targetMean)
            lo *= 2.0;

        while (mean(x1, x2, hi) > targetMean)
            hi *= 2.0;

        for (int i = 0; i < 100; i++)
        {
            double mid = (lo + hi) / 2.0;

            if (mean(x1, x2, mid) > targetMean)
                lo = mid;
            else
                hi = mid;
        }

        return (lo + hi) / 2.0;
    }

    private static double mean(int x1, int x2, double alpha)
    {
        Sums s = sums(x1, x2, alpha);
        return s.zx / s.z;
    }

    private static Sums sums(int x1, int x2, double alpha)
    {
        double z = 0;
        double zx = 0;

        for (int x = x1; x <= x2; x++)
        {
            double w = Math.pow(x, -alpha);
            z += w;
            zx += x * w;
        }

        return new Sums(z, zx);
    }

    private static final class Sums
    {
        final double z;
        final double zx;

        Sums(double z, double zx)
        {
            this.z = z;
            this.zx = zx;
        }
    }
}
