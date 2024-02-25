package my;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    public void initCensusSource(List<String[]> list) throws Exception
    {
        nbins = list.size();
        
        Bin prev = null;
        
        for (int k = 0; k < nbins; k++)
        {
            String[] sa = list.get(k);
            int age_x1 = Integer.parseInt(sa[0]);
            int age_x2 = Integer.parseInt(sa[1]);
            double avg = Double.parseDouble(sa[2]);
            
            Bin bin = new Bin(age_x1, age_x2, avg);
            bins.add(bin);
            bin.index = k;

            bin.prev = prev;
            if (prev != null)
                prev.next = bin;
            prev = bin;

            if (k == 0)
                firstBin = bin;
            lastBin = bin;
        }
        
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
        int npoints = list.size();
        if (npoints != nyears * PointsPerYear)
            throw new Exception("Incorrect interpolation data size");
        points = new Points(npoints); 
        
        for (int k = 0; k < npoints; k++)
        {
            String[] sa = list.get(k);
            points.x[k] = Double.parseDouble(sa[1]);
            points.y[k] = Double.parseDouble(sa[2]);
            points.avg[k] = Double.parseDouble(sa[3]);
        }

        double[] yearly = points2yearly(points);
        double[] avg = yearly2bin_avg(yearly);
        
        for (Bin bin : bins)
        {
            double pct = 100 * (avg[bin.index] - bin.avg) / bin.avg;
            if (Math.abs(pct) > 0.01)
                throw new Exception("Mean is broken");
        }

        printYearly();
    }
    
    private double[] points2yearly(Points points)
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
                int yx = year - firstBin.age_x1;
                avg[bin.index] += yearly[yx];
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
    }
}
