package rtss.pre1917.data.migration;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.CensusCategories;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.merge.MergeCities;
import rtss.pre1917.merge.MergeDescriptor;
import rtss.pre1917.merge.MergePost1897Regions;
import rtss.util.Util;

public class Emigration
{
    /* ================================== FETCH DATA ================================== */

    public long emigrants(String tname, int year) throws Exception
    {
        String key = key(tname, year);

        Double v = tname2amount.get(key);

        if (v == null)
        {
            v = 0.0;

            MergeDescriptor md = MergePost1897Regions.find(tname);

            if (md != null)
            {
                for (String xtn : md.parentWithChildren())
                    v += emigrants(xtn, year);
            }
            else if (union("Холмская", "Сахалин", "Камчатская").contains(tname))
            {
                // leave zero
            }
            else
            {
                throw new Exception(String.format("Нет данных об эмиграции из %s в %в году", tname, year));
            }
        }

        return Math.round(v * 1.05);
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

    @SuppressWarnings("unused")
    private static final long serialVersionUID = 1L;

    private Map<Integer, EmigrationYear> y2yd = new HashMap<>();

    public void setYearData(EmigrationYear yd) throws Exception
    {
        checkWritable();

        if (y2yd.containsKey(yd.year))
            throw new Exception("Duplicate year");
        y2yd.put(yd.year, yd);

    }

    private TerritoryDataSet tdsCensus;
    private Map<String, Double> jews;
    private CensusCategories censusCategories;

    public void build() throws Exception
    {
        checkWritable();

        jews = new LoadData().loadJews();
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
        scatter(yd.armenians, s2d("Эриванская"), PopulationSelector.ALL, yd.year);
        scatter(yd.finns * yd.vyborg / 100, s2d("Выборгская"), PopulationSelector.ALL, yd.year);

        // следует ли взвешивать губернии по численности населения или просто использовать соотношение 3-2-1-1-1-1 ?
        scatter(yd.germans,
                s2d("Волынская", 3, "Херсонская", 2, "Бессарабская", "Таврическая", "Саратовская", "Самарская"),
                PopulationSelector.NON_HEBREW, yd.year);

        Set<String> xs = MergeCities.leaveOnlyCombined(jews.keySet());
        scatter(yd.hebrews, s2d(xs), PopulationSelector.HEBREW, yd.year);

        scatter(yd.lithuanians * 0.4, s2d("Сувалкская"), PopulationSelector.NON_HEBREW, yd.year);
        scatter(yd.lithuanians * 0.55, s2d("Виленская", "Ковенская"), PopulationSelector.NON_HEBREW, yd.year);
        scatter(yd.lithuanians * 0.05, s2d("Курляндская", "Лифляндская"), PopulationSelector.NON_HEBREW, yd.year);
        
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

        scatter(yd.poles, sd, PopulationSelector.NON_HEBREW, yd.year);
        
        // --------------------------------------------------------------------------------------------

        if (yd.year <= 1902)
        {
            // ### use PopulationSelector.RUSSIAN
            scatter(yd.russians, s2d("Виленская", "Минская"), PopulationSelector.NON_HEBREW, yd.year);
        }
        else
        {
            // ### use PopulationSelector.RUSSIAN
            scatter(yd.russians,
                    s2d("Виленская", "Могилевская", "Минская", "Волынская", "Киевская", "Подольская", "Полтавская", "Воронежская", "Саратовская",
                        "Ставропольская", "Терская обл.", "Кубанская обл.", "Область войска Донского"),
                    PopulationSelector.NON_HEBREW, yd.year);
        }

        scatter(yd.ruthenians, s2d("Волынская", "Подольская"), PopulationSelector.NON_HEBREW, yd.year);

        scatter(yd.others + yd.greeks + yd.scandinavians,
                s2d(tsEuropeanRussian(), "Виленская", "Ковенская", tsBaltic(), tsPolish(yd.year)),
                PopulationSelector.NON_HEBREW, yd.year);
    }

    private static enum PopulationSelector
    {
        HEBREW, NON_HEBREW, ALL
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

        Double jp = jews.get(tname);

        if (jp == null)
        {
            MergeDescriptor md = MergeCities.findContaining(tname);
            if (md != null)
                jp = jews.get(md.combined);
        }

        if (jp == null)
            jp = 0.0;
        jp /= 100;

        switch (selector)
        {
        case HEBREW:
            pop *= jp;
            break;

        case NON_HEBREW:
            pop *= 1 - jp;
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

    /* =================================================================================================== */

    private S2D s2d(Object... objects) throws Exception
    {
        return S2D.make(objects);
    }

    private static class S2D extends HashMap<String, Double>
    {
        private static final long serialVersionUID = 1L;

        private void add(String s, Double d) throws Exception
        {
            if (containsKey(s))
                throw new Exception("already contains " + s);
            put(s, d);
        }

        public double weight(String s) throws Exception
        {
            if (!containsKey(s))
                throw new Exception("no value for " + s);
            return get(s);
        }

        public static S2D make(Object... objects) throws Exception
        {
            S2D sd = new S2D();

            String tname = null;

            for (Object o : objects)
            {
                if (o == null)
                {
                    throw new IllegalArgumentException("null argument");
                }
                else if (o instanceof String)
                {
                    String s = (String) o;
                    if (tname == null)
                    {
                        tname = s;
                    }
                    else
                    {
                        sd.add(tname, 1.0);
                        tname = s;
                    }
                }
                else if (o instanceof Number)
                {
                    if (tname == null)
                        throw new IllegalArgumentException("number not preceded by string");
                    Number n = (Number) o;
                    sd.add(tname, n.doubleValue());
                    tname = null;
                }
                else if (o instanceof S2D)
                {
                    if (tname != null)
                    {
                        sd.add(tname, 1.0);
                        tname = null;
                    }

                    S2D xs = (S2D) o;
                    for (String tn : xs.keySet())
                        sd.add(tn, xs.get(tn));
                }
                else if (o instanceof Set)
                {
                    if (tname != null)
                    {
                        sd.add(tname, 1.0);
                        tname = null;
                    }

                    Set<?> xs = (Set<?>) o;
                    for (Object o2 : xs)
                        sd.add((String) o2, 1.0);
                }
                else
                {
                    throw new IllegalArgumentException("unexpected type");
                }
            }

            if (tname != null)
                sd.add(tname, 1.0);

            return sd;
        }
    }

    private Collection<String> tsBaltic()
    {
        return Set.of("Лифляндская", "Курляндская", "Эстляндская");
    }

    private Collection<String> tsPolish(int year) throws Exception
    {
        Set<String> v = Set.of("Варшавская с Варшавой",
                               "Калишская",
                               "Келецкая",
                               "Ломжинская",
                               "Люблинская",
                               "Петроковская",
                               "Плоцкая",
                               "Радомская",
                               "Сувалкская");

        if (year <= 1913)
        {
            return union("Седлецкая", v);
        }
        else
        {
            return v;
        }
    }

    private Collection<String> tsEuropeanRussian()
    {
        // без Сибири, Кавказа и Средней Азии, 
        // в т.ч. без Кубанской области, Ставропольской и Черноморской губерний, Тобольской губ. и Якутской обл.,
        // также без Холмской губ.
        return Set.of("Архангельская",
                      "Астраханская",
                      "Витебская",
                      "Владимирская",
                      "Вологодская",
                      "Волынская",
                      "Воронежская",
                      "Вятская",
                      "Гродненская",
                      "Екатеринославская",
                      "Казанская",
                      "Калужская",
                      "Киевская",
                      "Костромская",
                      "Курская",
                      "Минская",
                      "Могилевская",
                      "Московская с Москвой",
                      "Нижегородская",
                      "Новгородская",
                      "Область войска Донского",
                      "Олонецкая",
                      "Оренбургская",
                      "Орловская",
                      "Пензенская",
                      "Полтавская",
                      "Псковская",
                      "Рязанская",
                      "Самарская",
                      "Санкт-Петербургская с Санкт-Петербургом",
                      "Саратовская",
                      "Симбирская",
                      "Смоленская",
                      "Таврическая с Севастополем",
                      "Тамбовская",
                      "Тверская",
                      "Тульская",
                      "Уфимская",
                      "Харьковская",
                      "Херсонская с Одессой",
                      "Черниговская",
                      "Ярославская");
    }

    private Collection<String> union(Object... objects) throws Exception
    {
        Set<String> xs = new HashSet<>();

        for (Object o : objects)
        {
            if (o instanceof String)
            {
                xs.add((String) o);
            }
            else if (o instanceof Set)
            {
                for (Object o2 : (Set<?>) o)
                    xs.add((String) o2);
            }
            else
            {
                throw new IllegalArgumentException("Neither a string nor a set of strings");
            }
        }

        return xs;
    }
}
