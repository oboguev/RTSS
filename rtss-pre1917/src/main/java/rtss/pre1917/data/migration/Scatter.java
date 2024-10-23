package rtss.pre1917.data.migration;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Scatter
{
    public static enum PopulationSelector
    {
        HEBREW, NON_HEBREW, CATHOLIC, PROTESTANT, RUSSIAN, ALL
    }

    public static S2D s2d(Object... objects) throws Exception
    {
        return S2D.make(objects);
    }

    public static class S2D extends HashMap<String, Double>
    {
        private static final long serialVersionUID = 1L;

        public void add(String s, Double d) throws Exception
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

    public static Collection<String> tsBaltic()
    {
        return Set.of("Лифляндская", "Курляндская", "Эстляндская");
    }

    public static Collection<String> tsPolish(int year) throws Exception
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

    public static Collection<String> tsEuropeanRussian()
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

    public static Collection<String> union(Object... objects) throws Exception
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
