package rtss.data.curves;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Util;

public class EnsureMonotonic
{
    private double[] curve;
    private Bin[] bins;
    @SuppressWarnings("unused")
    private String debug_title;
    private int ppy;
    private Bin first;
    
    private EnsureMonotonic(double[] curve, Bin[] bins, String debug_title) throws Exception
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
    public static void ensureMonotonicallyDecreasing_1_4_5_9(double[] curve, Bin[] bins, String debug_title) throws Exception
    {
        new EnsureMonotonic(curve, bins, debug_title).ensureMonotonicallyDecreasing_1_4_5_9();
    }

    private void ensureMonotonicallyDecreasing_1_4_5_9() throws Exception
    {
        Bin b1 =  bin_1_4(); 
        Bin b2 =  bin_5_9();
        if (b1 == null || b2 == null || b1.prev == null || b2.next == null)
            return;
        
        double[] seg1 = CurveUtil.seg(curve, b1, bins, ppy);
        double[] seg2 = CurveUtil.seg(curve, b2, bins, ppy);
        int x1 = ppy * (b1.age_x1 - first.age_x1);
        int x2 = ppy * (b2.age_x2 + 1 - first.age_x1) - 1;
        double[] seg12 = Util.splice(curve, x1, x2);
        String sig = "";
        
        sig += CurveUtil.isMonotonicallyDescending(seg1) ? "." : "N";
        sig += CurveUtil.isMonotonicallyDescending(seg2) ? "." : "N";
        sig += CurveUtil.isMonotonicallyDescending(seg12) ? "." : "N";
        if (sig.equals("..."))
            return;
        
        switch (sig)
        {
        case "...": 
            return;
        
        case "NNN":
            fixTwo(b1, b2);
            break;
        
        case ".NN":
            fixOne(b2);
            break;
        
        case "N.N":
            // not in data we use, but handle it
            fixOne(b1);
            break;
        
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
    
    private void fixOne(Bin b)
    {
        // ###
    }

    private void fixTwo(Bin b1, Bin b2)
    {
        // ###
    }
}
