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
        checkMean(yearly);

        // printYearly();
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
        double[] yearly = points2yearly(points);
        
        yearly = fix_16(yearly);
        checkMean(yearly);
        
        yearly = fix_60_69(yearly);
        checkMean(yearly);

        yearly = fix_50_59(yearly);
        checkMean(yearly);

        yearly = fix_40_49(yearly);
        checkMean(yearly);

        yearly = fix_30_39(yearly);
        checkMean(yearly);

        yearly = fix_25_29(yearly);
        checkMean(yearly);

        yearly = fix_20_25(yearly);
        checkMean(yearly);

        printYearly(yearly);
    }
    
    private double[] fix_16(double[] yearly)
    {
        yearly = yearly.clone();
        int yx15 = yx(15);
        int yx16 = yx(16);
        int yx17 = yx(17);
        int yx18 = yx(18);
        double avg = yx2bin(yx16).avg;
        
        yearly[yx16] = (2.0 / 3.0) * (avg - yearly[yx15]);
        yearly[yx17] = 2 * avg - yearly[yx16]; 
                
        return yearly;
    }
    
    private double[] fix_60_69(double[] yearly)
    {
        yearly = yearly.clone();
        Bin bin = year2bin(60);
        resample_next(yearly, yx(60), yx(69), bin.avg, bin.avg * 0.1, false);
        return yearly;
    }

    private double[] fix_50_59(double[] yearly)
    {
        return fix_range(yearly, 50, false);
    }

    private double[] fix_40_49(double[] yearly)
    {
        return fix_range(yearly, 40, false);
    }

    private double[] fix_30_39(double[] yearly)
    {
        return fix_range(yearly, 30, true);
    }
    
    private double[] fix_25_29(double[] yearly)
    {
        return fix_range(yearly, 25, true);
    }
    
    private double[] fix_20_25(double[] yearly)
    {
        return fix_range(yearly, 20, true);
    }
    
    private double[] fix_range(double[] yearly, int year, boolean contstrainByPrevious)
    {
        Bin bin = year2bin(year);
        return fix_range(yearly, year, bin.age_x2 - 1, contstrainByPrevious);
    }

    private double[] fix_range(double[] yearly, int year1, int year2, boolean contstrainByPrevious)
    {
        yearly = yearly.clone();
        Bin bin = year2bin(year1);
        resample_next(yearly, yx(year1), yx(year2), bin.avg, yearly[yx(year2 + 1)], contstrainByPrevious);
        return yearly;
    }

    /*
     * Modify yearly[yx1...yx2] such that average(yearly[yx1...yx2]) = avg and yearly[yx2] = v2 
     */
    private void resample(double[] yearly, int yx1, int yx2, double avg, double v2)
    {
        int sn = 0;
        int cn = 0;
        
        for (int yx = yx1; yx <= yx2; yx++)
        {
            sn += yx;
            cn++;
        }
        
        LinearUtil.AB ab = LinearUtil.solve(yx2, 1, v2, sn, cn, cn * avg);
        for (int yx = yx1; yx <= yx2; yx++)
            yearly[yx] = ab.a * yx + ab.b;
    }
    
    /*
     * Modify yearly[yx1...yx2] such that average(yearly[yx1...yx2]) = avg and projected yearly[yx2 + 1] = v2 
     */
    private void resample_next(double[] yearly, int yx1, int yx2, double avg, double v2, boolean contstrainByPrevious)
    {
        ExponentialInterpolation.resample_next(yearly, yx1, yx2, avg, v2);
        
        if (!contstrainByPrevious || yearly[yx1] < yearly[yx1 - 1])
            return;
        
        double[] exp = yearly.clone();
        
        resample_next_linear(yearly, yx1, yx2, avg, v2);
        double[] lin = yearly.clone();
        
        Bin bin = yx2bin(yx1 - 1);
        double r = yearly[yx(bin.age_x1)] / yearly[yx(bin.age_x2 - 1)];
        r = Math.pow(r,  1.0 / bin.widths_in_years);
        
        double a = (yearly[yx1 - 1] / r - lin[yx1]) / (exp[yx1] - lin[yx1]);
        for (int yx = yx1; yx <= yx2; yx++)
        {
            yearly[yx] = a * exp[yx] + (1 - a) * lin[yx];
        }
    }

    private void resample_next_linear(double[] yearly, int yx1, int yx2, double avg, double v2)
    {
        int sn = 0;
        int cn = 0;
        
        for (int yx = yx1; yx <= yx2; yx++)
        {
            sn += yx;
            cn++;
        }
        
        LinearUtil.AB ab = LinearUtil.solve(yx2 + 1, 1, v2, sn, cn, cn * avg);
        for (int yx = yx1; yx <= yx2; yx++)
            yearly[yx] = ab.a * yx + ab.b;
    }
}
