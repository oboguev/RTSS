package rtss.ww2losses;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.ww2losses.data.Bin;
import rtss.data.Points;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.util.Util;

public class PostProcess
{
    public static final int PointsPerYear = 100;
    
    private int nbins;
    private List<Bin> bins = new ArrayList<>();
    private int nyears;
    private Bin firstBin;
    private Bin lastBin;
    Map<Integer, Bin> yx2bin = new HashMap<>();
    
    private Points points;
    
    public void initCensusSource(List<String[]> list, int age_from, int age_to) throws Exception
    {
        PopulationByLocality rsfsr_1959 = PopulationByLocality.census(Area.RSFSR, 1959);
        PopulationByLocality ussr_1959 = PopulationByLocality.census(Area.USSR, 1959);
        
        
        Bin prev = null;
        int ix = 0;
        
        for (String[] sa : list)
        {
            int age = Integer.parseInt(sa[0]);
            double avg = Double.parseDouble(sa[1]);
            
            if (age < age_from || age > age_to)
                continue;
                
            Bin bin = new Bin(age, age + 1, avg);
            bins.add(bin);
            bin.index = ix++;

            bin.prev = prev;
            if (prev != null)
                prev.next = bin;
            prev = bin;

            if (firstBin == null)
                firstBin = bin;
            lastBin = bin;
        }
        
        nbins = bins.size();
        
        nyears = 0;
        int yx = 0;
        for (Bin bin : bins)
        {
            if (bin.widths_in_years < 1)
                throw new Exception("Invalid bin width");
            
            if (bin.next != null && bin.age_x2 != bin.next.age_x1)
                throw new Exception("Bins not continuous");
            
            nyears += bin.widths_in_years;
            
            for (int k = 0; k < bin.widths_in_years; k++)
            {
                yx2bin.put(yx++, bin);
            }
        }
    }
    
    public void initInterpolationData(List<String[]> list) throws Exception
    {
        list = trimPadding(list);

        int npoints = list.size();
        if (npoints != nyears * PointsPerYear)
            throw new Exception("Incorrect interpolation data size");
        points = new Points(npoints); 
        
        for (int k = 0; k < npoints; k++)
        {
            String[] sa = list.get(k);
            points.x[k] = Double.parseDouble(sa[0]);
            points.y[k] = Double.parseDouble(sa[1]);
            points.avg[k] = Double.parseDouble(sa[2]);
        }

        double[] yearly = points2yearly(points);
        checkMean(yearly);

        // printYearly();
    }
    
    private List<String[]> trimPadding(List<String[]> list) throws Exception
    {
        List<String[]> xlist = new ArrayList<>();
        
        for (String[] sa : list)
        {
            double x = Double.parseDouble(sa[0]);
            if (x >= firstBin.age_x1 && x < lastBin.age_x2)
                xlist.add(sa);
        }
        
        return xlist;
    }
    
    private boolean validateMean(double[] yearly)
    {
        double[] avg = yearly2bin_avg(yearly);
        
        for (Bin bin : bins)
        {
            double pct = 100 * (avg[bin.index] - bin.avg) / bin.avg;
            if (Math.abs(pct) > 0.01)
                return false;
        }
        
        return true;
    }
    
    private void checkMean(double[] yearly) throws Exception
    {
        if (!validateMean(yearly))
            throw new Exception("Mean is broken");
    }
    
    private int yx(int year)
    {
        return year - firstBin.age_x1;
    }

    private Bin yx2bin(int yx)
    {
        return yx2bin.get(yx);
    }
    
    @SuppressWarnings("unused")
    private Bin year2bin(int year)
    {
        return yx2bin(yx(year));
    }
    
    public double[] points2yearly(Points points)
    {
        double[] yy = new double[points.npoints / PointsPerYear];
        Arrays.fill(yy, 0);
        
        for (int ix = 0; ix < points.npoints; ix++)
        {
            int yx = ix / PointsPerYear;
            yy[yx] += points.y[ix];
        }
        
        for (int yx = 0; yx < yy.length; yx++)
        {
            yy[yx] /= PointsPerYear;
        }
        
        return yy;
    }
    
    private double[] yearly2bin_avg(double[] yearly)
    {
        double[] avg = new double[nbins];
        for (Bin bin : bins)
        {
            for (int year = bin.age_x1; year < bin.age_x2; year++)
            {
                avg[bin.index] += yearly[yx(year)];
            }
            avg[bin.index] /= bin.widths_in_years;
        }
        return avg;
    }
    
    private double[] yearly_avg()
    {
        double[] a = new double[points.npoints / PointsPerYear];
        Arrays.fill(a, 0);
        for (Bin bin : bins)
        {
            for (int ix = 0; ix < bin.widths_in_years; ix++)
            {
                a[ix + bin.age_x1 - firstBin.age_x1] = bin.avg;
            }
        }
        return a;
    }

    public void printYearly() throws Exception
    {
        printYearly(points2yearly(points));
    }

    private void printYearly(double[] yearly) throws Exception
    {

        Util.out("");
        Util.out("****************************************************************");
        Util.out("Interpolated population data, by years of age, for a whole year:");
        Util.out("");

        double[] a = yearly_avg();
        
        for (int yx = 0; yx < yearly.length; yx++)
        {
            String neg = "";
            if (yearly[yx] < 0)
                neg = "  NEG";
            String s = String.format("%10d %16s %16s%s", firstBin.age_x1 + yx, Util.f2s(yearly[yx]), Util.f2s(a[yx]), neg);
            Util.out(s);
        }
        
        if (validateMean(yearly))
        {
            Util.out("");
            Util.out("Mean is valid");
        }
        else
        {
            Util.out("");
            Util.out("!!! MEAN IS BROKEN !!!");
        }
    }
    
    /*******************************************************************/
    
    public void postProcess() throws Exception
    {
        /* 4-year window */
        int winsize = 4 * PointsPerYear;
        int min_np = -1;
        double min_sum = 0;
        
        for (int np = 0; np < points.npoints - winsize; np++) 
        {
            double[] yw = Util.splice(points.y, np, np + winsize - 1);
            double sum = Util.sum(yw) / PointsPerYear;
            if (min_np == -1 || sum < min_sum)
            {
                min_np = np;
                min_sum = sum;
            }
        }
        
        int np1 = min_np;
        int np2 = np1 + winsize;
        int np_mid = (np1 + np2) / 2;
        
        Util.out(String.format("Minimum births window: age = [%f - %f - %f], total births: %d", 
                               points.x[np1], points.x[np_mid], points.x[np2], Math.round(min_sum)));
        Util.noop();
    }
}
