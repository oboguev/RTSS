package rtss.pre1917.data.migration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.CensusCategories;
import rtss.pre1917.data.CensusCategoryValues;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryNames;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.Scatter.PopulationSelector;
import rtss.pre1917.data.migration.Scatter.S2D;
import rtss.pre1917.merge.MergeCities;
import rtss.pre1917.merge.MergeDescriptor;
import rtss.pre1917.merge.MergePost1897Regions;
import rtss.util.Util;

import static rtss.pre1917.data.migration.Scatter.s2d;
import static rtss.pre1917.data.migration.Scatter.union;
import static rtss.pre1917.data.migration.Scatter.tsBaltic;
import static rtss.pre1917.data.migration.Scatter.tsEuropeanRussian;
import static rtss.pre1917.data.migration.Scatter.tsPolish;

/*
 * Эмиграция из России в иностранные государства
 */
public class Emigration
{
    /* ================================== FETCH DATA ================================== */

    /*
     * Число эиигрантов уехавших за границу из губернии или области @tname в год @year 
     */
    public long emigrants(String tname, int year) throws Exception
    {
        String key = key(tname, year);

        Double v = tname2amount.get(key);

        if (v == null)
        {
            String pname = MergeCities.parent2combined(tname);
            if (pname != null)
            {
                key = key(pname, year);
                v = tname2amount.get(key);
            }
        }

        if (v == null)
        {
            v = 0.0;

            MergeDescriptor md = MergePost1897Regions.find(tname);

            if (md != null)
            {
                for (String xtn : md.parentWithChildren())
                    v += emigrants(xtn, year);
            }
            else if (MergeCities.isMergedCity(tname))
            {
                // leave zero
            }
            else if (union("Холмская", "Сахалин", "Камчатская", "Батумская").contains(tname))
            {
                // leave zero
            }
            else
            {
                throw new Exception(String.format("Нет данных об эмиграции из %s в %d году", tname, year));
            }
        }

        return Math.round(v);
    }

    /* ================================== INNER DATA ================================== */

    /* количество эмигрантов для губернии и года */
    private Map<String, Double> tname2amount = new HashMap<>();

    private boolean sealed = false;

    private String key(String tname, int year)
    {
        return year + " @ " + tname;
    }

    private void addAmount(String tname, int year, double value)
    {
        String key = key(tname, year);
        Double v = tname2amount.get(key);
        if (v == null)
            v = 0.0;
        tname2amount.put(key, v + value);
    }

    private void checkWritable() throws Exception
    {
        if (sealed)
            throw new Exception("Emigration instance is readonly");
    }

    /* ================================== CONSTRUCTION ================================== */

    private Map<Integer, EmigrationYear> y2yd = new HashMap<>();

    public void setYearData(EmigrationYear yd) throws Exception
    {
        checkWritable();

        if (y2yd.containsKey(yd.year))
            throw new Exception("Duplicate year");
        y2yd.put(yd.year, yd);

    }

    private TerritoryDataSet tdsCensus;
    private CensusCategories censusCategories;

    public void build() throws Exception
    {
        checkWritable();

        tdsCensus = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        censusCategories = new LoadData().loadCensusCategories();

        for (int year : Util.sort(y2yd.keySet()))
        {
            EmigrationYear yd = y2yd.get(year);
            build(yd);
            validate(yd);
        }

        sealed = true;
    }

    private void build(EmigrationYear yd) throws Exception
    {
        scatter(yd.armenians, s2d("Эриванская", "Карсская обл."), PopulationSelector.ALL, yd.year);
        scatter(yd.finns * yd.vyborg / 100, s2d("Выборгская"), PopulationSelector.ALL, yd.year);

        // следует ли взвешивать губернии по численности населения или просто использовать соотношение 3-2-1-1-1-1 ?
        scatter(yd.germans,
                s2d("Волынская", 3, "Херсонская", 2, "Бессарабская", "Таврическая", "Саратовская", "Самарская"),
                PopulationSelector.NON_HEBREW, yd.year);

        Set<String> xs = censusCategories.keySet();
        xs = Taxon.eliminateComposite(xs);
        xs = MergeCities.leaveOnlyCombined(xs);
        scatter(yd.hebrews, s2d(xs), PopulationSelector.HEBREW, yd.year);

        scatter(yd.lithuanians * 0.4, s2d("Сувалкская"), PopulationSelector.ALL, yd.year);
        scatter(yd.lithuanians * 0.55, s2d("Виленская", "Ковенская"), PopulationSelector.CATHOLIC, yd.year);
        scatter(yd.lithuanians * 0.05, s2d("Курляндская", "Лифляндская"), PopulationSelector.PROTESTANT, yd.year);

        // --------------------------------------------------------------------------------------------

        S2D sd = s2d("Варшавская с Варшавой", 6.4,
                     "Калишская", 11.5,
                     "Келецкая", 0.6,
                     "Ломжинская", 27.4,
                     "Люблинская", 2.4,
                     "Петроковская", 3.0,
                     "Плоцкая", 42.5,
                     "Радомская", 1.9,
                     "Сувалкская", 0.6 * 57.9);

        if (yd.year <= 1913)
            sd.add("Седлецкая", 2.1);

        scatter(yd.poles, sd, PopulationSelector.CATHOLIC, yd.year);

        // --------------------------------------------------------------------------------------------

        if (yd.year <= 1902)
        {
            scatter(yd.russians, s2d("Виленская", "Минская"), PopulationSelector.RUSSIAN, yd.year);
        }
        else
        {
            scatter(yd.russians,
                    s2d("Виленская", "Могилевская", "Минская", "Волынская", "Киевская", "Подольская", "Полтавская", "Воронежская", "Саратовская",
                        "Ставропольская", "Терская обл.", "Кубанская обл.", "Область войска Донского"),
                    PopulationSelector.RUSSIAN, yd.year);
        }

        scatter(yd.ruthenians, s2d("Волынская", "Подольская"), PopulationSelector.NON_HEBREW, yd.year);

        scatter(yd.others + yd.greeks + yd.scandinavians,
                s2d(tsEuropeanRussian(), "Виленская", "Ковенская", tsBaltic(), tsPolish(yd.year)),
                PopulationSelector.NON_HEBREW, yd.year);
    }

    private void scatter(double amount, S2D tnames, PopulationSelector selector, int year) throws Exception
    {
        double all_pop = pop_1897(tnames, selector);

        for (String tname : tnames.keySet())
        {
            double pop = pop_1897(tname, selector) * tnames.weight(tname);
            addAmount(tname, year, amount * pop / all_pop);
        }
    }

    private double pop_1897(S2D tnames, PopulationSelector selector) throws Exception
    {
        double v = 0;
        for (String tname : tnames.keySet())
            v += pop_1897(tname, selector) * tnames.weight(tname);
        return v;
    }

    private double pop_1897(String tname, PopulationSelector selector) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(tname);

        if (tname.equals("Выборгская"))
            return 386_440;

        Territory t = tdsCensus.get(tname);
        if (t == null)
        {
            MergeDescriptor md = MergeCities.findContaining(tname);
            if (md != null)
                t = tdsCensus.get(md.combined);
        }

        if (t == null && tname.equals("Батумская"))
            return 0;

        if (t == null)
            throw new Exception("no pop_1897 data for " + tname);

        TerritoryYear ty = t.territoryYearOrNull(1897);

        double pop = ty.population.total.both;

        CensusCategoryValues ccv = censusCategories.get(tname);
        if (ccv == null)
        {
            MergeDescriptor md = MergeCities.findContaining(tname);
            if (md != null)
                ccv = censusCategories.get(md.combined);
        }

        if (ccv == null)
            throw new Exception("No 1897 census category data for " + tname);

        switch (selector)
        {
        case HEBREW:
            pop *= ccv.pct_juifs / 100;
            break;

        case NON_HEBREW:
            pop *= 1 - ccv.pct_juifs / 100;
            break;

        case CATHOLIC:
            pop *= ccv.pct_catholic / 100;
            break;

        case PROTESTANT:
            pop *= ccv.pct_protestants / 100;
            break;

        case RUSSIAN:
            pop *= ccv.pct_russian / 100;
            break;

        case ALL:
            break;
        }

        return pop;
    }

    private void validate(EmigrationYear yd) throws Exception
    {
        double v = 0;

        for (String key : tname2amount.keySet())
        {
            if (key.startsWith("" + yd.year + " @ "))
                v += tname2amount.get(key);
        }

        if (Math.abs(yd.total - yd.finns * (1 - yd.vyborg / 100) - v) > 5)
            throw new Exception("Emigration builder self-check failed for year " + yd.year);
    }
}
