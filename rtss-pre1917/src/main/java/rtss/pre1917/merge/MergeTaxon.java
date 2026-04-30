package rtss.pre1917.merge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.FieldValue;
import rtss.util.Util;
import rtss.pre1917.util.WeightedAverage;

/*
 * Вычислить сумму по демографических данных по географическому таксону.
 * 
 * В отличие от MergeTerritories и MergeCities, MergeTaxon суммирует территории с весами 
 * не обязятельно равными единице для каждой из суммируемых территорий, но может включать
 * в сумму также и часть теорритории с весом менее 1.0. 
 */
public class MergeTaxon
{
    public static enum WhichYears
    {
        TaxonExistingYears, AllSetYears
    }

    public static class MissingDataAllowance
    {
        private Set<String> selectors;
        private Set<String> territoryNames;
        private Set<Integer> years;

        public MissingDataAllowance(Set<String> territoryNames)
        {
            this.territoryNames = territoryNames;
        }

        public MissingDataAllowance(Set<String> selectors, Set<String> territoryNames, Set<Integer> years)
        {
            this.selectors = selectors;
            this.territoryNames = territoryNames;
            this.years = years;
        }

        public boolean allowMissing(String selector, String tname, int year)
        {
            if (selectors != null && !selectors.contains(selector))
                return false;

            if (territoryNames != null && !territoryNames.contains(tname))
                return false;

            if (years != null && !years.contains(year))
                return false;

            return true;
        }
    }

    public static class MissingDataCheck
    {
        private String selector;
        private Integer enforceYearMin;
        private Integer enforceYearMax;

        public MissingDataCheck(String selector, Integer enforceYearMin, Integer enforceYearMax)
        {
            this.selector = selector;
            this.enforceYearMin = enforceYearMin;
            this.enforceYearMax = enforceYearMax;
        }

        public boolean isFlagMissing(String selector, int year)
        {
            if (!this.selector.equals(selector))
                return false;
            if (enforceYearMin != null && year < enforceYearMin)
                return false;
            if (enforceYearMax != null && year > enforceYearMax)
                return false;
            return true;
        }
    }

    public static class MergeTaxonOptions
    {
        private List<MissingDataAllowance> missingDataAllowances = new ArrayList<>();
        private List<MissingDataCheck> missingDataChecks = new ArrayList<>();

        public MergeTaxonOptions flagMissing(String selector, Integer enforceYearMin, Integer enforceYearMax)
        {
            missingDataChecks.add(new MissingDataCheck(selector, enforceYearMin, enforceYearMax));
            return this;
        }

        public MergeTaxonOptions allowMissingTeritory(String tname)
        {
            Set<String> xs = new HashSet<>();
            xs.add(tname);
            return allowMissingTeritories(xs);
        }

        public MergeTaxonOptions allowMissingTeritories(Set<String> tnames)
        {
            missingDataAllowances.add(new MissingDataAllowance(tnames));
            return this;
        }

        public MergeTaxonOptions allowMissingSelectorsTeritoriesYears(Set<String> selectors, Set<String> tnames, Set<Integer> years)
        {
            missingDataAllowances.add(new MissingDataAllowance(selectors, tnames, years));
            return this;
        }

        /* -------------------------------------------- */

        public boolean isFlagMissing(String selector, String tname, int year)
        {
            for (MissingDataAllowance mda : missingDataAllowances)
            {
                if (mda.allowMissing(selector, tname, year))
                    return false;
            }

            for (MissingDataCheck mdc : missingDataChecks)
            {
                if (mdc.isFlagMissing(selector, year))
                    return true;
            }

            return false;
        }
    }

    private TerritoryDataSet territories;

    public static Territory mergeTaxon(TerritoryDataSet territories, String txname, WhichYears whichYears) throws Exception
    {
        return mergeTaxon(territories, txname, whichYears, new MergeTaxonOptions());
    }

    public static Territory mergeTaxon(TerritoryDataSet territories, String txname, WhichYears whichYears,
            MergeTaxonOptions options) throws Exception
    {
        return new MergeTaxon(territories).mergeTaxon(txname, whichYears, options);
    }

    public MergeTaxon(TerritoryDataSet territories)
    {
        this.territories = territories;
    }

    public Territory mergeTaxon(String txname, WhichYears whichYears, MergeTaxonOptions options) throws Exception
    {
        VerifyNoTerritoryDuplication.verify(territories);
        
        Territory src = territories.get(txname);
        Territory res = new Territory(txname);

        List<Integer> years;
        if (whichYears == WhichYears.TaxonExistingYears)
        {
            years = src.years();
        }
        else
        {
            years = new ArrayList<>();
            for (int y = territories.minYear(); y <= territories.maxYear(); y++)
                years.add(y);
        }

        for (int year : years)
        {
            Taxon tx = Taxon.of(txname, year, territories);
            tx = tx.flatten(territories, year);

            TerritoryYear ty = res.territoryYear(year);

            sum_ur(ty, "population", tx, options);
            sum_ur(ty, "progressive_population", tx, options);
            sum_ur(ty, "midyear_population", tx, options);
            sum_ur(ty, "births", tx, options);
            sum_ur(ty, "deaths", tx, options);

            rate(ty, "cbr", tx);
            rate(ty, "cdr", tx);

            if (ty.cbr != null && ty.cdr != null)
            {
                ty.ngr = ty.cbr - ty.cdr;
            }
            else
            {
                ty.ngr = null;
            }
        }

        return res;
    }

    private void sum_ur(TerritoryYear ty, String selector, Taxon tx, MergeTaxonOptions options) throws Exception
    {
        sum_vg(ty, selector + ".total", tx, options);
        sum_vg(ty, selector + ".rural", tx, options);
        sum_vg(ty, selector + ".urban", tx, options);
    }

    private void sum_vg(TerritoryYear ty, String selector, Taxon tx, MergeTaxonOptions options) throws Exception
    {
        sum_long(ty, selector + ".male", tx, options);
        sum_long(ty, selector + ".female", tx, options);
        sum_long(ty, selector + ".both", tx, options);
    }

    private void sum_long(TerritoryYear ty, String selector, Taxon tx, MergeTaxonOptions options) throws Exception
    {
        double res = 0;
        int count = 0;

        for (String tname : tx.territories.keySet())
        {
            boolean flag = options.isFlagMissing(selector, tname, ty.year);

            Territory ter2 = territories.get(tname);

            if (ter2 == null)
            {
                String combined = MergePost1897Regions.contained2combined(tname);
                if (combined != null)
                {
                    if (territories.containsKey(combined) || !options.isFlagMissing(selector, combined, ty.year))
                        continue;
                }

                combined = MergeCities.contained2combined(tname);
                if (combined != null)
                {
                    if (territories.containsKey(combined) || !options.isFlagMissing(selector, combined, ty.year))
                        continue;
                }

                if (flag)
                    Util.err(String.format("MergeTaxon: missing territory [%s]", tname));
                continue;
            }

            if (!ter2.hasYear(ty.year))
            {
                if (flag)
                    Util.err(String.format("MergeTaxon: missing territory [%s] year [%d]", tname, ty.year));
                continue;
            }

            TerritoryYear ty2 = ter2.territoryYearOrNull(ty.year);
            if (ty2 == null)
            {
                if (flag)
                    Util.err(String.format("MergeTaxon: missing territory [%s] year [%d]", tname, ty.year));
                continue;
            }

            Long lv = FieldValue.getLong(ty2, selector);
            if (lv == null)
            {
                if (flag)
                    Util.err(String.format("MergeTaxon: missing territory [%s] year [%d] field [%s]", tname, ty.year, selector));
                continue;
            }

            double weight = tx.territories.get(tname);
            res += lv * weight;
            count++;
        }

        if (count != 0)
            FieldValue.setLong(ty, selector, Math.round(res));
    }

    private void rate(TerritoryYear ty, String selector, Taxon tx) throws Exception
    {
        WeightedAverage wa = new WeightedAverage();

        for (String tname : tx.territories.keySet())
        {
            Territory ter2 = territories.get(tname);
            if (ter2 != null && ter2.hasYear(ty.year))
            {
                TerritoryYear ty2 = ter2.territoryYear(ty.year);

                Long pop = ty2.population.total.both;
                if (pop == null)
                    pop = ty2.midyear_population.total.both;

                Double rate = FieldValue.getDouble(ty2, selector);

                double weight = tx.territories.get(tname);

                if (pop != null && rate != null)
                    wa.add(rate, pop * weight);
            }
        }

        if (wa.count() != 0)
            FieldValue.setDouble(ty, selector, wa.doubleResult());
    }
}
