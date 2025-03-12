package rtss.ww2losses.helpers.diag;

import rtss.ww2losses.helpers.PeacetimeMortalityTables;
import rtss.ww2losses.params.AreaParameters;
import rtss.ww2losses.struct.HalfYearEntries;
import rtss.ww2losses.struct.HalfYearEntries.HalfYearSelector;
import rtss.ww2losses.struct.HalfYearEntry;

import static rtss.data.population.projection.ForwardPopulation.years2days;

import rtss.data.curves.SculptDailyLX;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Clipboard;
import rtss.util.FieldValue;
import rtss.util.Util;
import rtss.util.plot.ChartXY;
import rtss.util.plot.ChartXYSPlineBasic;
import rtss.util.plot.PopulationChart;

public class DiagHelper
{
    private final AreaParameters ap;
    private final HalfYearEntries<HalfYearEntry> halves;

    public DiagHelper(AreaParameters ap, HalfYearEntries<HalfYearEntry> halves)
    {
        this.ap = ap;
        this.halves = halves;
    }

    public void showPopulationContext(String name) throws Exception
    {
        for (HalfYearEntry he : halves)
        {
            PopulationContext p = (PopulationContext) FieldValue.getObject(he, name);
            if (p != null)
            {
                PopulationChart.display(name + " в " + he.id(), p, name);
                double[] m = p.asArray(Locality.TOTAL, Gender.MALE);
                Util.unused(m);
                Util.noop();
            }
        }
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
        // put data to clipboard
        int n = 365 * 2;
        n = Math.min(n, p.length);
        n = Math.min(n, survival.length);
        StringBuilder sb = new StringBuilder();
        for (int nd = 0; nd < n; nd++)
        {
            sb.append(String.format("%d %.4f %.4f\n", nd, p[nd], survival[nd]));
        }
        Clipboard.put(sb.toString());
        Util.noop();

        // display data
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

        Util.noop();
    }

    public static void viewProjection(PopulationContext p, PeacetimeMortalityTables peacetimeMortalityTables, Gender gender, int ndays)
            throws Exception
    {
        double[] a = p.asArray(Locality.TOTAL, gender);

        CombinedMortalityTable cmt = peacetimeMortalityTables.getTable(1941, HalfYearSelector.FirstHalfYear);
        double[] lx = peacetimeMortalityTables.mt2lx(1941, HalfYearSelector.FirstHalfYear, cmt, Locality.TOTAL, gender);
        // viewLX(lx);
        Clipboard.put(lx);
        double[] survival = lx2survival(lx, ndays);
        viewProjection(a, survival, ndays);
    }

    public static void view_mid1941(PopulationContext p, Gender gender) throws Exception
    {
        double[] a = p.asArray(Locality.TOTAL, gender);
        StringBuilder sb = new StringBuilder();
        for (int nd = 0; nd < 365 * 2; nd++)
        {
            sb.append(String.format("%d %.4f\n", nd, a[nd]));
        }
        Clipboard.put(sb.toString());
        Util.noop();
    }
    
    @SuppressWarnings("unused")
    private static void viewLX(double[] lx) throws Exception
    {
        ChartXY chart = new ChartXY("lx", "days", "lx");
        chart.addSeries("original", lx);
        
        chart.addSeries("0", SculptDailyLX.scultDailyLX(lx, 0));
        chart.addSeries("1e-6", SculptDailyLX.scultDailyLX(lx, 1e-6));
        chart.addSeries("1.2e-6", SculptDailyLX.scultDailyLX(lx, 1.2e-6));

        chart.display();
        
        Util.noop();
    }
}
