package rtss.math.algorithms.pclm;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.util.Util;

public class ExposuresPCLMExtraTestFixed
{
    public static void main(String[] args)
    {
        try
        {
            Util.out("============== test 1 ==============");
            test_1();
            
            Util.out("");
            Util.out("============== test 2 ==============");
            test_2();
        }
        catch (Exception ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private static void test_1() throws Exception
    {
        /* ADH-RSFSR 1946 MALE */
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

        Util.out("");
        Util.out("========================================");
        Util.out("Regular PCLM (preserves simple average)");
        Util.out("========================================");
        double[] yy = new PCLM(bins, lambda, ppy).pclm();
        verifySimple(bins, ppy, yy, null);

        Util.out("");
        Util.out("=======================================================");
        Util.out("ExposuresPCLM (preserves exposure-weighted average)");
        Util.out("=======================================================");
        yy = new ExposuresPCLM(bins, exposures, lambda, ppy).pclm();
        verifySimple(bins, ppy, yy, null);
        Util.out("");
        Util.out("Now checking EXPOSURE-WEIGHTED conservation:");
        verifyExposureWeighted(bins, ppy, yy, exposures);

        Util.out("");
        Util.out("=======================================================");
        Util.out("Exposure distribution analysis:");
        Util.out("=======================================================");
        analyzeExposureDistribution(bins, exposures, ppy);
    }

    private static void test_2() throws Exception
    {
        /* ADH-RSFSR 1946 FEMALE */
        List<Bin> blist = new ArrayList<>();
        blist.add(new Bin(0, 0, 124.300000));
        blist.add(new Bin(1, 4, 19.703302));
        blist.add(new Bin(5, 9, 3.394227));
        blist.add(new Bin(10, 14, 2.097797));
        blist.add(new Bin(15, 19, 2.596623));
        blist.add(new Bin(20, 24, 3.394227));
        blist.add(new Bin(25, 29, 3.693163));
        blist.add(new Bin(30, 34, 4.191192));
        blist.add(new Bin(35, 39, 4.688972));
        blist.add(new Bin(40, 44, 5.186503));
        blist.add(new Bin(45, 49, 6.975557));
        blist.add(new Bin(50, 54, 8.364819));
        blist.add(new Bin(55, 59, 13.113262));
        blist.add(new Bin(60, 64, 19.605267));
        blist.add(new Bin(65, 69, 32.364682));
        blist.add(new Bin(70, 74, 53.609496));
        blist.add(new Bin(75, 79, 78.082925));
        blist.add(new Bin(80, 84, 107.474327));
        blist.add(new Bin(85, 100, 147.088936));

        Bin[] bins = Bins.bins(blist);

        final double[] exposures = { 370786, 522895, 675004, 827113, 979222, 1131331, 1280492, 1357259, 1353841, 1274077, 1089536, 1000971, 966605,
                                     988398, 1064489, 1227473, 1324231, 1377012, 1381221, 1338063, 1237519, 1149408, 1061429, 974699, 889944, 798426,
                                     749865, 732824, 747618, 793268, 886885, 945399, 981365, 992370, 978982, 934180, 901365, 872759, 849006, 829689,
                                     817054, 797477, 773460, 744885, 712124, 673539, 639894, 609119, 581267, 556180, 533864, 513487, 495099, 478621,
                                     463929, 451550, 438075, 424119, 409637, 394619, 379401, 362170, 343333, 322936, 301159, 277264, 256192, 236895,
                                     219329, 203320, 189138, 174458, 159697, 144823, 129884, 114527, 100876, 88366, 76893, 66338, 56405, 47971, 40644,
                                     34271, 28709, 23756, 19721, 16377, 13615, 11336, 9459, 7919, 6659, 5636, 4812, 4159, 3651, 3272, 3008, 2848,
                                     2771 };

        final double lambda = 0.0001;
        final int ppy = 1;

        int width = Bins.lastBin(bins).age_x2 - Bins.firstBin(bins).age_x1 + 1;
        if (exposures.length != width * ppy)
            throw new IllegalArgumentException("exposures width");

        Util.out("");
        Util.out("========================================");
        Util.out("Regular PCLM (preserves simple average)");
        Util.out("========================================");
        double[] yy = new PCLM(bins, lambda, ppy).pclm();
        verifySimple(bins, ppy, yy, null);

        Util.out("");
        Util.out("=======================================================");
        Util.out("ExposuresPCLM (preserves exposure-weighted average)");
        Util.out("=======================================================");
        yy = new ExposuresPCLM(bins, exposures, lambda, ppy).setMaxIterations(1000).pclm();
        verifySimple(bins, ppy, yy, null);
        Util.out("");
        Util.out("Now checking EXPOSURE-WEIGHTED conservation:");
        verifyExposureWeighted(bins, ppy, yy, exposures);

        Util.out("");
        Util.out("=======================================================");
        Util.out("Exposure distribution analysis:");
        Util.out("=======================================================");
        analyzeExposureDistribution(bins, exposures, ppy);
    }

    private static void verifySimple(Bin[] bins, int ppy, double[] yy, double[] exposures) throws Exception
    {
        int width = Bins.lastBin(bins).age_x2 - Bins.firstBin(bins).age_x1 + 1;
        if (yy.length != width * ppy)
            throw new Exception("pclm width");

        Util.out("");
        Util.out("Bin      Input Rate   Simple Avg      Diff");
        Util.out("----------------------------------------------");

        for (Bin bin : bins)
        {
            double[] y = Util.splice(yy, bin.x1(ppy), bin.x2(ppy));

            double yyav = Util.average(y);
            double pct = 100.0 * (yyav - bin.avg) / bin.avg;

            Util.out(String.format("%d-%-2d   %10.5f   %10.5f   %+7.2f%%",
                                   bin.age_x1, bin.age_x2, bin.avg, yyav, pct));
        }
    }

    private static void verifyExposureWeighted(Bin[] bins, int ppy, double[] rates, double[] exposures) throws Exception
    {
        int width = Bins.lastBin(bins).age_x2 - Bins.firstBin(bins).age_x1 + 1;
        if (rates.length != width * ppy)
            throw new Exception("pclm width");

        Util.out("");
        Util.out("Bin      Input Rate   Exp-Wtd Avg     Diff");
        Util.out("----------------------------------------------");

        int firstAge = bins[0].age_x1;

        for (Bin bin : bins)
        {
            int start = (bin.age_x1 - firstAge) * ppy;
            int end = (bin.age_x2 - firstAge + 1) * ppy;

            // Calculate exposure-weighted average
            double weightedSum = 0.0;
            double exposureSum = 0.0;

            for (int j = start; j < end && j < rates.length; j++)
            {
                weightedSum += rates[j] * exposures[j];
                exposureSum += exposures[j];
            }

            double weightedAvg = weightedSum / exposureSum;
            double pct = 100.0 * (weightedAvg - bin.avg) / bin.avg;

            Util.out(String.format("%d-%-2d   %10.5f   %10.5f   %+7.2f%%",
                                   bin.age_x1, bin.age_x2, bin.avg, weightedAvg, pct));
        }
    }

    private static void analyzeExposureDistribution(Bin[] bins, double[] exposures, int ppy)
    {
        int firstAge = bins[0].age_x1;

        Util.out("");
        Util.out("Bin      Total Exp      Min Exp      Max Exp    Ratio");
        Util.out("----------------------------------------------------------");

        for (Bin bin : bins)
        {
            int start = (bin.age_x1 - firstAge) * ppy;
            int end = (bin.age_x2 - firstAge + 1) * ppy;

            double total = 0.0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            for (int j = start; j < end && j < exposures.length; j++)
            {
                total += exposures[j];
                min = Math.min(min, exposures[j]);
                max = Math.max(max, exposures[j]);
            }

            double ratio = max / min;

            Util.out(String.format("%d-%-2d   %,12.0f   %,10.0f   %,10.0f   %6.2f",
                                   bin.age_x1, bin.age_x2, total, min, max, ratio));
        }

        Util.out("");
        Util.out("Note: High exposure ratios (>>1) indicate non-uniform distribution.");
        Util.out("When ratio ≈ 1.0, simple and exposure-weighted averages are similar.");
        Util.out("When ratio >> 1.0, the two averages will differ significantly.");
    }
}
