package rtss.pre1917.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
 * Таксон (или составной таксон) -- это территория состоящая из других областей,
 * в т.ч. рекурсивно других составных таксонов. 
 */
public class Taxon
{
    public final Map<String, Double> territories = new HashMap<>();
    private final String name;
    private final int year;
    public static Double DoubleONE = Double.valueOf(1.0);

    public Taxon(String name, int year)
    {
        this.name = name;
        this.year = year;
    }

    private Taxon add(String name) throws Exception
    {
        return add(name, DoubleONE);
    }

    private Taxon add(String name, double fraction) throws Exception
    {
        territories.put(name, fraction);
        return this;
    }

    public static Taxon of(String name, int year, TerritoryDataSet territories) throws Exception
    {
        return of(name, year, territories.dataSetType);
    }
    
    public static Taxon of(String name, int year, DataSetType dataSetType) throws Exception
    {
        Taxon t = new Taxon(name, year);

        switch (name)
        {
        case "Империя":
            t.add("Азиатская Россия").add("Европейская Россия");
            break;

        case "Европейская Россия":
            if (year >= 1913)
                t.add("51 губерния Европейской России");
            else
                t.add("50 губерний Европейской России");
            t.add("привислинские губернии");
            break;

        case "51 губерния Европейской России":
            if (year >= 1913)
            {
                t.add50(year);
                t.add("Холмская");
            }
            break;

        case "50 губерний Европейской России":
            t.add50(year);
            break;

        case "Остзейские губернии":
            t.add("Курляндская").add("Лифляндская").add("Эстляндская");
            break;

        case "привислинские губернии":
            t.add("г. Варшава")
                    .add("Варшавская")
                    .add("Калишская")
                    .add("Келецкая")
                    .add("Ломжинская")
                    .add("Люблинская")
                    .add("Петроковская")
                    .add("Плоцкая")
                    .add("Радомская")
                    .add("Сувалкская");
            if (year <= 1913)
                t.add("Седлецкая");
            break;

        case "неземские губернии":
            t.add("Архангельская ")
                    .add("Астраханская")
                    .add("Виленская")
                    .add("Витебская ")
                    .add("Волынская")
                    .add("Гродненская")
                    .add("Киевская")
                    .add("Ковенская")
                    .add("Минская")
                    .add("Могилевская")
                    .add("Оренбургская")
                    .add("Подольская");
            break;

        case "земские губернии":
            t.add("Бессарабская")
                    .add("Владимирская")
                    .add("Вологодская")
                    .add("Воронежская")
                    .add("Вятская")
                    .add("Екатеринославская")
                    .add("Казанская")
                    .add("Калужская")
                    .add("Костромская")
                    .add("Курская")
                    .add("Московская")
                    .add("г. Москва")
                    .add("Нижегородская")
                    .add("Новгородская")
                    .add("Олонецкая")
                    .add("Орловская")
                    .add("Пензенская")
                    .add("Пермская")
                    .add("Полтавская")
                    .add("Псковская")
                    .add("Рязанская")
                    .add("Самарская")
                    .add("Санкт-Петербургская")
                    .add("г. Санкт-Петербург")
                    .add("Саратовская")
                    .add("Симбирская")
                    .add("Смоленская")
                    .add("Таврическая")
                    .add("г. Севастополь")
                    .add("Тамбовская")
                    .add("Тверская")
                    .add("Тульская")
                    .add("Уфимская")
                    .add("Харьковская")
                    .add("Херсонская")
                    .add("г. Николаев")
                    .add("г. Одесса")
                    .add("Черниговская")
                    .add("Ярославская")
                    .add("Ростовское и./Д град.");
            break;

        case "Азиатская Россия":
            t.add("Кавказ").add("Сибирь").add("Средняя Азия");
            break;

        case "Кавказ":
            t.add("г. Баку")
                    .add("Бакинская")
                    .add("Батумская")
                    .add("Дагестанская обл.")
                    .add("Елисаветпольская")
                    .add("Карсская обл.")
                    .add("Кубанская обл.")
                    .add("Кутаисская")
                    .add("Ставропольская")
                    .add("Терская обл.")
                    .add("Тифлисская")
                    .add("Черноморская")
                    .add("Эриванская");
            break;

        case "Сибирь":
            t.add("Амурская обл.")
                    .add("Енисейская")
                    .add("Забайкальская обл.")
                    .add("Иркутская")
                    .add("Камчатская")
                    .add("Приморская обл.")
                    .add("Сахалин")
                    .add("Тобольская")
                    .add("Томская")
                    .add("Якутская обл.");
            break;

        case "Средняя Азия":
            t.add("Акмолинская обл.")
                    .add("Закаспийская обл.")
                    .add("Самаркандская обл.")
                    .add("Семипалатинская обл.")
                    .add("Семиреченская обл.")
                    .add("Сыр-Дарьинская обл.")
                    .add("Тургайская обл.")
                    .add("Уральская обл.")
                    .add("Ферганская обл.");
        }

        if (t.territories.size() == 0)
            t = null;

        return t;
    }

    private void add50(int year) throws Exception
    {
        add("Архангельская");
        add("Астраханская");
        add("Бессарабская");
        add("Виленская");
        add("Витебская");
        add("Владимирская");
        add("Волынская");
        add("Вологодская");
        add("Воронежская");
        add("Вятская");
        add("Гродненская");
        add("Область войска Донского");
        add("Екатеринославская");
        add("Казанская");
        add("Калужская");
        add("Киевская");
        add("Ковенская");
        add("Костромская");
        add("Курляндская");
        add("Курская");
        add("Лифляндская");
        add("Минская");
        add("Могилевская");
        add("Московская");
        add("г. Москва");
        add("Нижегородская");
        add("Новгородская");
        add("Олонецкая");
        add("Оренбургская");
        add("Орловская");
        add("Пензенская");
        add("Пермская");
        add("Санкт-Петербургская");
        add("г. Санкт-Петербург");
        add("Подольская");
        add("Полтавская");
        add("Псковская");
        add("Рязанская");
        add("Самарская");
        add("Саратовская");
        add("Симбирская");
        add("Смоленская");
        add("Таврическая");
        add("г. Севастополь");
        add("Тамбовская");
        add("Тверская");
        add("Тульская");
        add("Уфимская");
        add("Харьковская");
        add("Херсонская");
        add("г. Николаев");
        add("г. Одесса");
        add("Черниговская");
        add("Эстляндская");
        add("Ярославская");
        if (year >= 1914)
            add("Ростовское и./Д град.");
    }

    /*
     * Имена всех составных таксонов входящих в данный таксон, рекурсивно
     */
    public Set<String> allCompositeSubTaxons(boolean includeSelf, DataSetType dataSetType) throws Exception
    {
        Set<String> xs = new HashSet<String>();
        allCompositeSubTaxons(xs, dataSetType);
        if (includeSelf)
            xs.add(name);
        return xs;
    }

    private void allCompositeSubTaxons(Set<String> xs, DataSetType dataSetType) throws Exception
    {
        for (String xname : territories.keySet())
        {
            Taxon tx = of(xname, year, dataSetType);
            if (tx != null)
                xs.addAll(tx.allCompositeSubTaxons(true, dataSetType));
        }
    }

    /*
     * Проверить, является ли поименованная территория составным таксоном
     */
    public static boolean isComposite(String name)
    {
        switch (name)
        {
        case "Империя":
        case "Европейская Россия":
        case "51 губерния Европейской России":
        case "50 губерний Европейской России":
        case "Остзейские губернии":
        case "привислинские губернии":
        case "земские губернии":
        case "неземские губернии":
        case "Азиатская Россия":
        case "Кавказ":
        case "Сибирь":
        case "Средняя Азия":
            return true;

        default:
            break;
        }

        return false;
    }

    /*
     * Редуцировать таксон до базовых областей
     */
    public Taxon flatten(DataSetType dataSetType) throws Exception
    {
        Taxon tx = new Taxon(name, year);
        flatten(tx.territories, DoubleONE, dataSetType);
        return tx;
    }

    private void flatten(Map<String, Double> out, Double pweight, DataSetType dataSetType) throws Exception
    {
        for (String tname : territories.keySet())
        {
            Double weight = territories.get(tname);

            if (weight == DoubleONE && pweight == DoubleONE)
            {
                weight = DoubleONE;
            }
            else
            {
                weight *= pweight;
            }

            if (!isComposite(tname))
            {
                if (out.containsKey(tname))
                    throw new Exception("Overlapping taxons");
                out.put(tname, weight);
            }
            else
            {
                Taxon t2 = Taxon.of(tname, year, dataSetType).flatten(dataSetType);
                t2.flatten(out, weight, dataSetType);
            }
        }
    }
}
