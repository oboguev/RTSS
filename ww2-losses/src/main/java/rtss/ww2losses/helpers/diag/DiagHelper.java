package rtss.ww2losses.helpers.diag;

import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.struct.HalfYearEntries;
import rtss.ww2losses.struct.HalfYearEntry;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.ChartXY;
import rtss.util.plot.ChartXYSPlineBasic;

public class DiagHelper
{
    private final AreaParameters ap;
    private final HalfYearEntries<HalfYearEntry> halves;

    public DiagHelper(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves)
    {
        this.ap = ap;
        this.halves = halves;
    }

    public void showEarlyAges() throws Exception
    {
        for (HalfYearEntry he : halves)
        {
            double offset = he.offset_start1941();
            if (offset >= 0.5)
            {
                int nd1 = years2days(offset - 0.5);
                int nd2 = years2days(offset + 1);
                double[] v = he.p_nonwar_without_births.asArray(Locality.TOTAL, Gender.MALE);
                v = Util.splice(v, nd1, nd2);
                ChartXYSPlineBasic.display("Муж. население " + ap.area + " в возрасте 4.5 - 6 @ 1946 на " + he.id(), v);
            }
        }
    }

    public static double[] lx2survival(double[] lx, int ndays)
    {
        double[] survival = new double[lx.length - ndays];

        for (int k = 0; k < survival.length; k++)
            survival[k] = lx[k + ndays] / lx[k];

        return survival;
    }

    public static void viewProjection(double[] p, double[] survival, int hydays) throws Exception
    {
        p = Util.splice(p, 0, survival.length - 1);
        double[] p2 = Util.multiply(p, survival);
        
        survival = Util.splice(survival, hydays, survival.length - hydays);
        double[] p3 = Util.splice(p2, 0, survival.length - 1);
        p3 = Util.multiply(p3, survival);
        
        p = Util.splice(p, 0, p3.length - 1);
        p2 = Util.splice(p2, 0, p3.length - 1);

        new ChartXY("Кривая населения до и после", "x", "y")
                .addSeries("1", p)
                .addSeries("2", p2)
                .addSeries("3", p3)
                .display();

    }
}
