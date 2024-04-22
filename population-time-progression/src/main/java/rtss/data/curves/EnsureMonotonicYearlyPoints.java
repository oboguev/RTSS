package rtss.data.curves;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.mutable.MutableInt;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Util;

public class EnsureMonotonicYearlyPoints
{
    private Bin[] bins;
    private final double[] curve;
    private final int ppy;
    private String title;
    private int inflection;
    
    public EnsureMonotonicYearlyPoints(Bin[] bins, double[] curve, String title) throws Exception
    {
        this.bins = bins;
        this.curve = curve;
        this.ppy = CurveUtil.ppy(curve, bins);
        this.title = title;
    }
    
    public void fix() throws Exception
    {
        MutableInt infl = new MutableInt(0); 
        List<List<Integer>> xlist = CurveVerifier.locateContinuousNonMonotonicPoints(curve, bins, title, 1.0, infl);
        inflection = infl.getValue();
        
        for (int x : getOneLast(xlist))
            fixOneLast(x);
        for (int x : getOneFirst(xlist))
            fixOneFirst(x);
        for (int x : getOneMiddle(xlist))
            fixOneMiddle(x);
    }
    
    private List<Integer> getOneLast(List<List<Integer>> xlist)
    {
        List<Integer> list = new ArrayList<>();
        
        for (List<Integer> xl : xlist)
        {
            if (xl.size() != 1)
                continue;
            int x = xl.get(0);
            Bin bin = Bins.binForAge(x, bins);
            if (x == bin.age_x2)
                list.add(x);
        }
        
        return list;
    }

    private List<Integer> getOneFirst(List<List<Integer>> xlist)
    {
        List<Integer> list = new ArrayList<>();
        
        for (List<Integer> xl : xlist)
        {
            if (xl.size() != 1)
                continue;
            int x = xl.get(0);
            Bin bin = Bins.binForAge(x, bins);
            if (x == bin.age_x1)
                list.add(x);
        }
        
        return list;
    }

    private List<Integer> getOneMiddle(List<List<Integer>> xlist)
    {
        List<Integer> list = new ArrayList<>();
        
        for (List<Integer> xl : xlist)
        {
            if (xl.size() != 1)
                continue;
            int x = xl.get(0);
            Bin bin = Bins.binForAge(x, bins);
            if (x != bin.age_x1 && x != bin.age_x2)
                list.add(x);
        }
        
        return list;
    }
    
    /* ============================================================ */
    
    /*
     * Singular non-monotonic break point, last point in the segment
     */
    private void fixOneLast(int x)
    {
        Bin bin = Bins.binForAge(x, bins);
        
        if (x == inflection)
        {
            // ###
            Util.noop();
        }
        else if (x < inflection)
        {
            // ###
            Util.noop();
        }
        else
        {
            // ###
            Util.noop();
        }
    }

    /*
     * Singular non-monotonic break point, first point in the segment
     */
    private void fixOneFirst(int x)
    {
        Bin bin = Bins.binForAge(x, bins);

        if (x > inflection)
        {
            // ###
            Util.noop();
        }
        else
        {
            notImplemented();
        }
    }

    /*
     * Singular non-monotonic break point, middle point in the segment
     */
    private void fixOneMiddle(int x)
    {
        Bin bin = Bins.binForAge(x, bins);

        if (x > inflection)
        {
            // ###
            Util.noop();
        }
        else
        {
            notImplemented();
        }
    }
    
    private void notImplemented()
    {
        Util.err(String.format("Unable to make curve %s well-monotonic, method unimplemented at %s", 
                               title, Util.stackFrame(1)));
    }
}
