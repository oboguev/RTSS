package rtss.pre1917.data.migration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
     * Число эмигрантов уехавших за границу из губернии или области @tname в год @year 
     */
    public long emigrants(String tname, int year) throws Exception
    {
        if (tname.equals(Taxon.Астраханская_кочевники))
            return 0;

        if (tname.equals(Taxon.Астраханская_оседлое))
            tname = "Астраханская";

        String key = key(tname, year);

        Double v = tname2amount.get(key);

        if (v != null)
            return Math.round(v);

        MergeDescriptor md = MergePost1897Regions.find(tname);
        if (md != null)
        {
            v = 0.0;
            for (String xtn : md.parentWithChildren())
                v += emigrants(xtn, year);
            return Math.round(v);
        }

        md = MergeCities.find(tname);
        if (md != null)
        {
            v = 0.0;
            for (String xtn : md.parentWithChildren())
                v += emigrants(xtn, year);
            return Math.round(v);
        }

        if (union("Холмская", "Сахалин", "Камчатская обл.", "Батумская").contains(tname))
            return 0;

        if (union("г. Баку", "г. Севастополь", "г. Николаев").contains(tname))
            return 0;

        throw new MissingMigrationDataException(String.format("Нет данных об эмиграции из %s в %d году", tname, year));
    }

    public long emigrants(int year) throws Exception
    {
        Double v = 0.0;

        for (String key : tname2amount.keySet())
        {
            if (isYearKey(key, year))
                v += tname2amount.get(key);
        }

        return Math.round(v);
    }

    /* ================================== INNER DATA ================================== */

    /* количество эмигрантов для губернии и года */
    private Map<String, Double> tname2amount = new TreeMap<>();

    private boolean sealed = false;

    private String key(String tname, int year)
    {
        return tname + " @ " + year;
    }

    private boolean isYearKey(String key, int year)
    {
        return key.endsWith(" @ " + year);
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

    private Map<Integer, EmigrationYear> y2yd = new TreeMap<>();

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

        // tdsCensus = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        tdsCensus = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY);
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
        Set<String> xs = new HashSet<>(censusCategories.keySet());
        xs = leaveOnlyElementary(xs);

        scatter(yd.armenians, s2d("Эриванская", "Карсская обл."), PopulationSelector.ALL, yd.year);
        scatter(yd.finns * yd.vyborg / 100, s2d("Выборгская"), PopulationSelector.ALL, yd.year);

        // следует ли взвешивать губернии по численности населения или просто использовать соотношение 3-2-1-1-1-1 ?
        scatter(yd.germans,
                s2d("Волынская", 3, "Херсонская", 2, "Бессарабская", "Таврическая", "Саратовская", "Самарская"),
                PopulationSelector.UNITARY, yd.year);

        scatter(yd.hebrews, s2d(xs), PopulationSelector.HEBREW, yd.year);

        // --------------------------------------------------------------------------------------------

        scatter(yd.lithuanians * 0.4, s2d("Сувалкская"), PopulationSelector.ALL, yd.year);
        scatter(yd.lithuanians * 0.55, s2d("Виленская", "Ковенская"), PopulationSelector.CATHOLIC, yd.year);
        scatter(yd.lithuanians * 0.05, s2d("Курляндская", "Лифляндская"), PopulationSelector.PROTESTANT, yd.year);

        // --------------------------------------------------------------------------------------------

        S2D sd = s2d("Варшавская", 6.4,
                     "г. Варшава", 6.4,
                     "Калишская", 11.5,
                     "Келецкая", 0.6,
                     "Ломжинская", 27.4,
                     "Люблинская", 2.4,
                     "Петроковская", 3.0,
                     "Плоцкая", 42.5,
                     "Радомская", 1.9,
                     "Сувалкская", 57.9);

        if (yd.year <= 1913)
            sd.add("Седлецкая", 2.1);

        double poles_non_kingdom_emigration_intensity = 2.1;

        sd.add("Гродненская", poles_non_kingdom_emigration_intensity);
        sd.add("Ковенская", poles_non_kingdom_emigration_intensity);
        sd.add("Виленская", poles_non_kingdom_emigration_intensity);
        sd.add("Волынская", poles_non_kingdom_emigration_intensity);
        sd.add("г. Одесса", poles_non_kingdom_emigration_intensity);
        sd.add("Витебская", poles_non_kingdom_emigration_intensity);
        sd.add("Минская", poles_non_kingdom_emigration_intensity);
        sd.add("Курляндская", poles_non_kingdom_emigration_intensity);
        sd.add("Подольская", poles_non_kingdom_emigration_intensity);
        sd.add("Киевская", poles_non_kingdom_emigration_intensity);
        sd.add("Лифляндская", poles_non_kingdom_emigration_intensity);
        sd.add("Могилевская", poles_non_kingdom_emigration_intensity);

        scatter(yd.poles, sd, PopulationSelector.POLES, yd.year);

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

        scatter(yd.ruthenians, s2d("Волынская", "Подольская"), PopulationSelector.NON_HEBREW_NON_POLISH, yd.year);

        scatter(yd.others + yd.greeks + yd.scandinavians,
                s2d(tsEuropeanRussian(), "Виленская", "Ковенская", tsBaltic(), tsPolish(yd.year)),
                PopulationSelector.NON_HEBREW_NON_POLISH, yd.year);
    }

    private Set<String> leaveOnlyElementary(Set<String> xs)
    {
        xs = Taxon.eliminateComposite(xs);

        for (MergeDescriptor md : MergeCities.MergeCitiesDescriptors)
        {
            for (String s : md.parentWithChildren())
            {
                if (s == null)
                    continue;

                switch (s)
                {
                case "г. Баку":
                case "г. Николаев":
                case "г. Севастополь":
                case "Ростовское и./Д град.":
                    continue;
                }

                if (!xs.contains(s))
                    Util.err("leaveOnlyElementary: missing " + s);
            }

            xs.remove(md.combined);
        }

        for (MergeDescriptor md : MergePost1897Regions.MergePost1897Descriptors)
        {
            for (String s : md.parentWithChildren())
            {
                switch (s)
                {
                case "Холмская":
                    continue;
                }

                if (!xs.contains(s))
                    Util.err("leaveOnlyElementary: missing " + s);
            }

            xs.remove(md.combined);
        }

        for (String tname : xs)
        {
            if (censusCategories.get(tname) == null)
                Util.err("leaveOnlyElementary: census missing " + tname);
        }

        return xs;
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
        
        CensusCategoryValues ccv;
        double pop;

        if (tname.equals("Выборгская"))
        {
            pop = 386_440;
            ccv = new CensusCategoryValues();
            ccv.pct_protestants = 100;
            ccv.pct_catholic = 0;
            ccv.pct_poles = 0;
            ccv.pct_juifs = 0;
            ccv.pct_russian = 0;
        }
        else
        {
            Territory t = tdsCensus.get(tname);
            if (t == null && Util.False)
            {
                MergeDescriptor md = MergeCities.findContaining(tname);
                if (md != null)
                    t = tdsCensus.get(md.combined);
            }

            if (t == null && Util.False)
            {
                MergeDescriptor md = MergePost1897Regions.findContaining(tname);
                if (md != null)
                    t = tdsCensus.get(md.combined);
            }

            if (t == null && tname.equals("Батумская"))
                return 0;

            if (t == null && tname.equals("Камчатская обл."))
                return 0;

            if (t == null)
                throw new Exception("no pop_1897 data for " + tname);

            TerritoryYear ty = t.territoryYearOrNull(1897);

            pop = ty.population.total.both;

            ccv = censusCategories.get(tname);
            if (ccv == null)
            {
                MergeDescriptor md = MergeCities.findContaining(tname);
                if (md != null)
                    ccv = censusCategories.get(md.combined);
            }
        }
        
        if (ccv == null)
            throw new Exception("No 1897 census category data for " + tname);

        switch (selector)
        {
        case HEBREW:
            pop *= ccv.pct_juifs / 100;
            break;

        case POLES:
            pop *= ccv.pct_poles / 100;
            break;

        case NON_HEBREW:
            pop *= 1 - ccv.pct_juifs / 100;
            break;

        case NON_HEBREW_NON_POLISH:
            pop *= 1 - (ccv.pct_juifs + ccv.pct_poles) / 100;
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

        case UNITARY:
            pop = 1.0;
            break;

        default:
            throw new IllegalArgumentException("selector = " + selector);
        }

        if (pop <= 0)
            throw new Exception("population is non-positive: " + pop);

        return pop;
    }

    private void validate(EmigrationYear yd) throws Exception
    {
        double v = 0;

        for (String key : tname2amount.keySet())
        {
            if (key.endsWith(" @ " + yd.year))
                v += tname2amount.get(key);
        }

        if (Math.abs(yd.total - yd.finns * (1 - yd.vyborg / 100) - v) > 5)
            throw new Exception("Emigration builder self-check failed for year " + yd.year);
    }
}
