package rtss.math.algorithms.pclm;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Util;

public class ExposuresPCLMExtraTest
{
    public static void main(String[] args)
    {
        try
        {
            test_1();
        }
        catch (Exception ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private static void test_1() throws Exception
    {

        List<Bin> blist = new ArrayList<>();
        blist.add(new Bin(0, 0, 146.200000));
        blist.add(new Bin(1, 4, 21.368391));
        blist.add(new Bin(5, 9, 4.191192));
        blist.add(new Bin(10, 14, 2.895799));
        blist.add(new Bin(15, 19, 3.493882));
        blist.add(new Bin(20, 24, 5.186503));
        blist.add(new Bin(25, 29, 5.982036));
        blist.add(new Bin(30, 34, 7.571193));
        blist.add(new Bin(35, 39, 9.455018));
        blist.add(new Bin(40, 44, 13.507938));
        blist.add(new Bin(45, 49, 18.035380));
        blist.add(new Bin(50, 54, 22.639785));
        blist.add(new Bin(55, 59, 30.233541));
        blist.add(new Bin(60, 64, 39.978885));
        blist.add(new Bin(65, 69, 54.649953));
        blist.add(new Bin(70, 74, 75.867667));
        blist.add(new Bin(75, 79, 104.076277));
        blist.add(new Bin(80, 84, 152.155627));
        blist.add(new Bin(85, 100, 200.234977));

        Bin[] bins = Bins.bins(blist);

        final double[] exposures = { 369023, 529514, 690006, 850498, 1010990, 1171481, 1325919, 1400482, 1386966, 1290151, 1080254, 976028, 930510,
                                     945127, 1017081, 1185021, 1263336, 1278775, 1227185, 1113684, 922638, 788173, 680656, 598639, 538894, 499507,
                                     477164, 471430, 482116, 508784, 560184, 593112, 614193, 622203, 617308, 596795, 577280, 555930, 533100, 508895,
                                     482799, 458225, 434518, 411690, 389768, 367964, 350519, 336482, 325802, 318234, 314995, 308614, 300438, 290377,
                                     278575, 264400, 252004, 240603, 230226, 220768, 212743, 202922, 191936, 179787, 166612, 151924, 139070, 127333,
                                     116679, 106994, 98396, 89646, 80957, 72301, 63700, 54912, 47312, 40502, 34391, 28884, 23770, 19641, 16210, 13368,
                                     11011, 9050, 7445, 6127, 5048, 4165, 3444, 2857, 2381, 1998, 1692, 1451, 1265, 1127, 1031, 974, 946 };

        final double lambda = 0.0001;
        final int ppy = 1;

        int width = Bins.lastBin(bins).age_x2 - Bins.firstBin(bins).age_x1 + 1;
        if (exposures.length != width * ppy)
            throw new IllegalArgumentException("exposures width");

        // ExposuresPCLM.MAX_ITERATIONS = 1000;

        Util.out("");
        Util.out("Regular PCLM");
        Util.out("");
        double[] yy = new PCLM(bins, lambda, ppy).pclm();
        verify(bins, ppy, yy);

        Util.out("");
        Util.out("ExposuresPCLM");
        Util.out("");
        yy = new ExposuresPCLM(bins, exposures, lambda, ppy).pclm();
        verify(bins, ppy, yy);
    }
    
    private static void verify(Bin[] bins, int ppy, double[] yy) throws Exception
    {
        int width = Bins.lastBin(bins).age_x2 - Bins.firstBin(bins).age_x1 + 1;
        if (yy.length != width * ppy)
            throw new Exception("pclm width");

        for (Bin bin : bins)
        {
            double[] y = Util.splice(yy, bin.x1(ppy), bin.x2(ppy));

            if (Util.False && bin.next == null && Util.average(y) < bin.avg)
                continue;

            if (Util.False && Util.differ(Util.average(y), bin.avg, 0.001))
                throw new Exception("Curve does not preserve mean values of the bins");

            double yyav = Util.average(y);

            double pct = 100.0 * (yyav - bin.avg) / bin.avg;

            Util.out(String.format("%d-%d %.5f -> %.5f diff = %.2f%%", bin.age_x1, bin.age_x2, bin.avg, yyav, pct));
        }
    }
}
