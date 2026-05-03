package rtss.pre1917.calc.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rtss.math.algorithms.MathUtil;
import rtss.pre1917.ExportData;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.MissingMigrationDataException;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.pre1917.eval.EvalGrowthRate;
import rtss.pre1917.util.WeightedAverage;
import rtss.util.Util;

public class TaxonYearlyPopulationData extends HashMap<Integer, TaxonYearData>
{
    private static final long serialVersionUID = 1L;
    private final String taxonName;

    public final TerritoryDataSet tdsPopulation;
    public final TerritoryDataSet tdsVitalRates;
    public final TerritoryDataSet tdsCSK;
    public final TerritoryDataSet tdsExportPopulation;
    public final int toYear;

    private final double PROMILLE = 1000.0;
    private final String nl = "\n";
    private final char NBSP = 0xA0;
    private final String NBSP_S = "" + NBSP;

    public static class Summary
    {
        private int nyears;

        public double cbr;
        public double cdr;
        public double ngr;
        public long population_increase;
        public long migration;

        public void add(double cbr, double cdr, double ngr, long population_increase, long migration)
        {
            this.cbr += cbr;
            this.cdr += cdr;
            this.ngr += ngr;
            this.population_increase = population_increase;
            this.migration += migration;
            nyears++;
        }

        public Summary getAverages()
        {
            Summary s = new Summary();

            s.cbr = this.cbr / nyears;
            s.cdr = this.cdr / nyears;
            s.ngr = this.ngr / nyears;
            s.population_increase = Math.round((double) this.population_increase / nyears);
            s.migration = Math.round((double) this.migration / nyears);

            return s;
        }
    }

    public TaxonYearlyPopulationData(String taxonName,
            TerritoryDataSet tdsPopulation,
            TerritoryDataSet tdsVitalRates,
            TerritoryDataSet tdsCSK,
            TerritoryDataSet tdsExportPopulation,
            int toYear)
    {
        this.taxonName = taxonName;
        this.tdsPopulation = tdsPopulation;
        this.tdsVitalRates = tdsVitalRates;
        this.tdsCSK = tdsCSK;
        this.tdsExportPopulation = tdsExportPopulation;
        this.toYear = toYear;
    }

    public TaxonYearlyPopulationData print()
    {
        Util.out("Численность населения в границах " + taxonName);
        Util.out("рождаемость, смертность, естественный прирост, ест. + мех. изменение численности, миграция");
        Util.out("в нормировке на население на начало года");
        Util.out("");

        List<Integer> years = Util.sort(keySet());
        int lastPartialYear = years.get(years.size() - 1);
        Summary summary = new Summary();

        for (int year : years)
        {
            TaxonYearData yd = get(year);

            if (year != lastPartialYear)
            {
                double ngr = yd.cbr - yd.cdr;
                Util.out(String.format("%d %,d %.1f %.1f %.1f %,d %,d",
                                       year, yd.population, yd.cbr, yd.cdr, ngr,
                                       yd.population_increase,
                                       yd.migration));
                summary.add(yd.cbr, yd.cdr, ngr, yd.population_increase, yd.migration);
            }
            else
            {
                Util.out(String.format("%d %,d", year, yd.population));
            }
        }

        Summary av = summary.getAverages();
        Util.out(String.format("%d-%d %s %.1f %.1f %.1f %,d %,d",
                               years.get(0), lastPartialYear - 1, 
                               NBSP_S, av.cbr, av.cdr, av.ngr,
                               av.population_increase,
                               av.migration));

        Util.out("");
        printRateChange("CBR");
        printRateChange("CDR");
        printRateChange("NGR");

        Util.out("");
        Util.out("То же в нормировке на среднегодовое население, численность населения указана среднегодовая");
        Util.out("");
        summary = new Summary();

        for (int year : years)
        {
            TaxonYearData yd = get(year);

            if (year != lastPartialYear)
            {
                double ngr_middle = yd.cbr_middle - yd.cdr_middle;
                Util.out(String.format("%d %,d %.1f %.1f %.1f",
                                       year, yd.population_middle, yd.cbr_middle, yd.cdr_middle, ngr_middle));
                summary.add(yd.cbr_middle, yd.cdr_middle, ngr_middle, 0, 0);
            }
            else
            {
                // Util.out(String.format("%d %,d", year, yd.population));
            }
        }

        return this;
    }

    private double rate(String which, int year)
    {
        TaxonYearData yd = get(year);
        switch (which)
        {
        case "CBR":
            return yd.cbr;
        case "CDR":
            return yd.cdr;
        case "NGR":
            return yd.cbr - yd.cdr;
        default:
            throw new RuntimeException("Invalid selector");
        }
    }

    private void printRateChange(String which)
    {
        WeightedAverage wa1 = new WeightedAverage();
        wa1.add(rate(which, 1896), 1.0);
        wa1.add(rate(which, 1897), 1.0);
        wa1.add(rate(which, 1899), 1.0);
        wa1.add(rate(which, 1900), 1.0);

        WeightedAverage wa2 = new WeightedAverage();
        wa2.add(rate(which, 1911), 1.0);
        wa2.add(rate(which, 1912), 1.0);
        wa2.add(rate(which, 1913), 1.0);

        double r1 = wa1.doubleResult();
        double r2 = wa2.doubleResult();
        double dr = r2 - r1;
        double pct = 100.0 * dr / r1;

        Util.out(String.format("Изменение в %s на %.1f (%.1f%%)", which, dr, pct));
    }

    /* ======================================================================================= */

    public static class PopulationDifference implements Comparable<PopulationDifference>
    {
        public final String tname;
        public final long diff;
        public final double pct;

        public PopulationDifference(String tname, long diff, double pct)
        {
            this.tname = tname;
            this.diff = diff;
            this.pct = pct;
        }

        public PopulationDifference(String tname, long v1, long v2)
        {
            this(tname, v1 - v2, (100.0 * (v1 - v2)) / v2);
        }

        @Override
        public int compareTo(PopulationDifference o)
        {
            long d = o.diff - this.diff;
            if (d > 0)
                return 1;
            else if (d < 0)
                return -1;
            else
                return 0;
        }
    }

    public TaxonYearlyPopulationData printDifferenceWithUGVI()
    {
        List<PopulationDifference> list = new ArrayList<>();

        for (String tname : tdsPopulation.keySet())

        {
            if (!Taxon.isComposite(tname))
            {
                Territory t = tdsPopulation.get(tname);
                TerritoryYear ty = t.territoryYearOrNull(toYear);
                list.add(new PopulationDifference(tname, ty.population.total.both, ty.progressive_population.total.both));
            }
        }

        print(list, "Превышение по УГВИ");

        return this;
    }

    public TaxonYearlyPopulationData printDifferenceWithCSK()
    {
        List<PopulationDifference> list = new ArrayList<>();

        for (String tname : tdsPopulation.keySet())

        {
            if (!Taxon.isComposite(tname))
            {
                TerritoryYear ty = tdsPopulation.get(tname).territoryYearOrNull(toYear);
                TerritoryYear tyCSK = tdsCSK.get(tname).territoryYearOrNull(toYear);
                list.add(new PopulationDifference(tname, tyCSK.population.total.both, ty.progressive_population.total.both));
            }
        }

        print(list, "Превышение по ЦСК");

        return this;
    }

    private void print(List<PopulationDifference> list, String title)
    {
        Util.out("");
        Util.out(title + ":");
        Util.out("");

        Collections.sort(list);

        for (PopulationDifference pd : list)
            Util.out(String.format("    \"%s\" %,d %.1f", pd.tname, pd.diff, pd.pct));
    }

    public TaxonYearlyPopulationData exportData(String csvPath, String txtPath) throws Exception
    {
        if (tdsExportPopulation == null)
            return this;

        if (csvPath == null && txtPath == null)
            return this;

        ExportData ed = ExportData.forFinal();
        StringBuilder sb = new StringBuilder();

        for (String tname : Util.sort(tdsExportPopulation.keySet()))
        {
            if (!Taxon.isComposite(tname))
                exportValues(ed, sb, tname);
        }

        if (csvPath != null)
            ed.export(csvPath);

        if (txtPath != null)
            Util.writeAsFile(txtPath, sb.toString().replace("\n", "\r\n"));

        return this;
    }

    private void exportValues(ExportData ed, StringBuilder sb, String tname) throws Exception
    {
        if (sb.length() != 0)
            sb.append(nl + "*************************************************************************" + nl);

        sb.append(nl);
        sb.append(tname + nl);
        sb.append(nl);
        sb.append("год      чн       чр     чс      мигр   р    с    еп   р2   с2   еп2 v s" + nl);
        sb.append("==== ========= ======= ======= ======= ==== ==== ==== ==== ==== ==== = =" + nl);

        final TotalMigration totalMigration = TotalMigration.getTotalMigration();
        final EvalGrowthRate evalGrowthRate = new EvalGrowthRate(null);
        final Territory t = tdsExportPopulation.get(tname).dup();

        addLastYear(t, totalMigration);

        for (int year : t.years())
        {
            if (year >= 1896)
            {
                TerritoryYear ty = t.territoryYear(year);

                Double cbr = rate(ty.births.total.both, ty.progressive_population.total.both);
                Double cdr = rate(ty.deaths.total.both, ty.progressive_population.total.both);
                Double ngr = (cbr != null && cdr != null) ? cbr - cdr : null;

                Double cbr2 = null;
                Double cdr2 = null;
                Double ngr2 = null;

                TerritoryYear ty2 = t.territoryYearOrNull(year + 1);
                if (ty2 != null &&
                    ty2.progressive_population != null &&
                    ty2.progressive_population.total != null &&
                    ty2.progressive_population.total.both != null)
                {
                    long pop_middle = MathUtil.log_average(ty.progressive_population.total.both, ty2.progressive_population.total.both);
                    cbr2 = rate(ty.births.total.both, pop_middle);
                    cdr2 = rate(ty.deaths.total.both, pop_middle);
                    ngr2 = (cbr2 != null && cdr2 != null) ? cbr2 - cdr2 : null;
                }

                Long saldo = null;

                try
                {
                    saldo = totalMigration.saldo(t.name, year);
                }
                catch (MissingMigrationDataException ex)
                {
                    if (year < 1916)
                        throw ex;
                }

                boolean vrok = this.tdsVitalRates.containsKey(t.name) || t.name.equals("Выборгская");
                if (cbr == null && cdr == null)
                    vrok = false;

                boolean stable = evalGrowthRate.is_stable_year(t.name, year);

                ed.add(t.name, year,
                       ty.progressive_population.total.both,
                       ty.births.total.both,
                       ty.deaths.total.both,
                       saldo,
                       stable,
                       cbr, cdr, ngr,
                       cbr2, cdr2, ngr2,
                       vrok);

                line(sb, year,
                     ty.progressive_population.total.both,
                     ty.births.total.both,
                     ty.deaths.total.both,
                     saldo,
                     stable,
                     cbr, cdr, ngr,
                     cbr2, cdr2, ngr2,
                     vrok);
            }
        }
    }

    private void line(StringBuilder sb, int year, Long population, Long births, Long deaths, Long saldo, boolean stable,
            Double cbr, Double cdr, Double ngr,
            Double cbr2, Double cdr2, Double ngr2,
            boolean vrok)
    {
        String line = String.format("%4d %s %s %s %s %s %s %s %s %s %s %s %s",
                                    year,
                                    s_count(population, 9),
                                    s_count(births, 7),
                                    s_count(deaths, 7),
                                    s_count(saldo, 7),
                                    s_rate(cbr), s_rate(cdr), s_rate(ngr),
                                    s_rate(cbr2), s_rate(cdr2), s_rate(ngr2),
                                    vrok ? "1" : "0",
                                    stable ? "*" : NBSP_S);

        sb.append(line + nl);
    }

    private String s_count(Long value, int width)
    {
        return s_wide(value == null ? "" : String.format("%,d", value), width);
    }

    private String s_rate(Double value)
    {
        return s_wide(value == null ? "" : String.format("%.1f", value), 4);
    }

    private String s_wide(String s, int width)
    {
        while (s.length() < width)
            s = " " + s;
        return s;
    }

    private Double rate(Long v, Long pop)
    {
        if (v == null || pop == null || pop == 0)
            return null;
        else
            return (v * PROMILLE) / pop;
    }

    private void addLastYear(Territory t, TotalMigration totalMigration) throws Exception
    {
        List<Integer> years = t.years();
        if (years.size() == 0)
            return;
        int year = years.get(years.size() - 1);
        TerritoryYear ty = t.territoryYear(year);

        if (ty.progressive_population == null ||
            ty.progressive_population.total == null ||
            ty.progressive_population.total.both == null)
        {
            return;
        }

        if (ty.births == null ||
            ty.births.total == null ||
            ty.births.total.both == null)
        {
            return;
        }

        if (ty.deaths == null ||
            ty.deaths.total == null ||
            ty.deaths.total.both == null)
        {
            return;
        }

        Long saldo = null;

        try
        {
            saldo = totalMigration.saldo(t.name, year);
        }
        catch (MissingMigrationDataException ex)
        {
            if (year < 1916)
                throw ex;
        }

        if (t.hasYear(year + 1))
            throw new Exception("Must not have year " + (year + 1));

        TerritoryYear ty2 = t.territoryYear(year + 1);
        ty2.progressive_population.total.both = ty.progressive_population.total.both + ty.births.total.both - ty.deaths.total.both;
        if (saldo != null)
            ty2.progressive_population.total.both += saldo;
    }
}