package rtss.data.curves.ensure;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.curves.CurveUtil;
import rtss.util.Util;

public class EnsureMonotonicCurve
{
    private double[] curve;
    private Bin[] bins;
    @SuppressWarnings("unused")
    private String debug_title;
    private int ppy;
    private Bin first;

    private EnsureMonotonicCurve(double[] curve, Bin[] bins, String debug_title) throws Exception
    {
        this.curve = curve;
        this.bins = bins;
        this.debug_title = debug_title;
        this.ppy = CurveUtil.ppy(curve, bins);
        this.first = Bins.firstBin(bins);
    }

    /*
     * Резкое падение смертности в возрастных группах 1-4 и 5-9 часто вызывает перехлёысты сплайна в этом диапазоне.
     * Изменить ход кривой сделав её монотонно уменьшающейся, но сохраняя средние значения.
     */
    public static boolean ensureMonotonicallyDecreasing_1_4_5_9(double[] curve, Bin[] bins, String debug_title) throws Exception
    {
        return new EnsureMonotonicCurve(curve, bins, debug_title).ensureMonotonicallyDecreasing_1_4_5_9();
    }

    private boolean ensureMonotonicallyDecreasing_1_4_5_9() throws Exception
    {
        Bin b1 = bin_1_4();
        Bin b2 = bin_5_9();
        if (b1 == null || b2 == null || b1.prev == null || b2.next == null)
            return false;

        double[] seg1 = CurveUtil.seg(curve, b1, bins, ppy);
        double[] seg2 = CurveUtil.seg(curve, b2, bins, ppy);
        int x1 = ppy * (b1.age_x1 - first.age_x1);
        int x2 = ppy * (b2.age_x2 + 1 - first.age_x1) - 1;
        double[] seg12 = Util.splice(curve, x1, x2);
        String sig = "";

        sig += CurveUtil.isMonotonicallyDescending(seg1) ? "." : "x";
        sig += CurveUtil.isMonotonicallyDescending(seg2) ? "." : "x";
        sig += CurveUtil.isMonotonicallyDescending(seg12) ? "." : "x";

        switch (sig)
        {
        case "...":
            return true;

        case "xxx":
            return fixTwo(b1, b2);

        case ".xx":
            if (fixOne(b2, false))
                return true;
            return fixTwo(b1, b2);

        case "x.x":
            // not present in the data we use, but do handle it anyway
            if (fixOne(b1, false))
                return true;
            return fixTwo(b1, b2);

        default:
            throw new Exception("Internal error");
        }
    }

    private Bin bin_1_4()
    {
        Bin b = Bins.binForAge(1, bins);
        if (b != null && b.age_x1 == 1 && b.age_x2 == 4)
            return b;
        else
            return null;
    }

    private Bin bin_5_9()
    {
        Bin b = Bins.binForAge(5, bins);
        if (b != null && b.age_x1 == 5 && b.age_x2 == 9)
            return b;
        else
            return null;
    }

    /* ================================================================= */

    private boolean fixOne(Bin b, boolean force) throws Exception
    {
        int x1 = ppy * (b.age_x1 - first.age_x1);
        int x2 = ppy * (b.age_x2 + 1 - first.age_x1) - 1;

        double[] seg = CurveUtil.seg(curve, b, bins, ppy);

        double v1;
        double v2;

        if (b.prev == null)
        {
            v1 = curve[x1];
        }
        else
        {
            v1 = 2 * curve[x1 - 1] - curve[x1 - 2];
        }

        if (b.next == null)
        {
            v2 = curve[x2];
        }
        else
        {
            v2 = 2 * curve[x2 + 1] - curve[x2 + 2];
        }

        if (!Util.within(b.avg, v1, v2))
            return false;
        
        double[] line = seg.clone();
        CurveUtil.straight_line(line, 0, v1, seg.length - 1, v2);
        if (v1 == v2)
            return doneOne(b, line);

        double vmin = Util.min(line);
        double vmax = Util.max(line);

        double avg = Util.average(line);
        double a1, a2, a = 1;

        if (avg == b.avg)
        {
            return doneOne(b, line);
        }
        else if (avg < b.avg)
        {
            // should increase avg by decreasing a
            do
            {
                a /= 2;

                if (a < 0.1  && !force)
                    return false;
                if (a < 0.01)
                    throw new Exception("Unable to fix the curve");
                
                seg = CurveUtil.distort(line, vmin, vmax, a);
            }
            while (Util.average(seg) < b.avg);
            a2 = 1;
            a1 = a;
        }
        else // if (avg > b.avg)
        {
            // should decrease avg by increasing a
            do
            {
                a *= 2;

                if (a > 10  && !force)
                    return false;
                if (a > 100)
                    throw new Exception("Unable to fix the curve");
                
                seg = CurveUtil.distort(line, vmin, vmax, a);
            }
            while (Util.average(seg) > b.avg);
            a1 = 1;
            a2 = a;
        }
        
        if (Util.average(seg) == b.avg)
            return doneOne(b, seg);
        
        /*
         * Perform binary search between a1 (low) and a2 (high) 
         */
        for (;;)
        {
            a = (a1 + a2) / 2;

            if (!force && (a < 0.1 || a > 10))
                return false;
            
            seg = CurveUtil.distort(line, vmin, vmax, a);
            avg = Util.average(seg);
            if (avg == b.avg || !Util.differ(avg, b.avg))
            {
                CurveUtil.insert(curve, b, bins, seg);
                // CurveUtil.exportCurveSegmentToClipboard(curve, b.prev.age_x1, b.next.age_x2, ppy);
                // Util.out(String.format("Fixed age range %d-%d in %s, with a = %f", b.age_x1, b.age_x2, debug_title, a));
                return true;
            }
            else if (avg < b.avg)
            {
                // should increase avg by decreasing a
                a2 = a;
            }
            else // if (avg < b.avg)
            {
                // should decrease avg by increasing a
                a1 = a;
            }
        }
    }
    
    private boolean doneOne(Bin b, double[] seg) throws Exception
    {
        CurveUtil.insert(curve, b, bins, seg);
        return true;
    }

    /* ================================================================= */

    private boolean fixTwo(Bin b1, Bin b2) throws Exception
    {
        if (!(b1.avg > b2.avg))
            return false;
        
        // double[] seg1 = CurveUtil.seg(curve, b1, bins, ppy);
        // double[] seg2 = CurveUtil.seg(curve, b2, bins, ppy);
        // double[] seg12 = Util.splice(curve, x1, x2);

        int x1 = ppy * (b1.age_x1 - first.age_x1);
        int x2 = ppy * (b2.age_x2 + 1 - first.age_x1) - 1;

        double v1;
        double v2;

        if (b1.prev == null)
        {
            v1 = curve[x1];
        }
        else
        {
            v1 = 2 * curve[x1 - 1] - curve[x1 - 2];
        }

        if (b2.next == null)
        {
            v2 = curve[x2];
        }
        else
        {
            v2 = 2 * curve[x2 + 1] - curve[x2 + 2];
        }
        
        if (!Util.within(b1.avg, v1, v2))
            return fixThree(b1, b2, b2.next);

        if (!Util.within(b2.avg, v1, v2))
            return fixThree(b1, b2, b2.next);
        /*
         * move vmid in range ]b2.avg ... b1.avg[
         */
        
        Util.err(String.format("Unable to fix non-monotonic curve %s, method unimplemented: %s", debug_title, Util.stackFrame(0)));
        // Util.out("FixTwo: " + debug_title);

        return false;
    }

    /* ================================================================= */

    private boolean fixThree(Bin b1, Bin b2, Bin b3) throws Exception
    {
        Util.err(String.format("Unable to fix non-monotonic curve %s, method unimplemented: %s", debug_title, Util.stackFrame(0)));
        // Util.out("FixThree: " + debug_title);
        return false;
    }
}
