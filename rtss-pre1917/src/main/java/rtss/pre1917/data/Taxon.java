package rtss.pre1917.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rtss.pre1917.merge.MergeCities;
import rtss.pre1917.merge.MergeDescriptor;

/*
 * Таксон (или составной таксон) -- это территория состоящая из других областей,
 * в т.ч. рекурсивно других составных таксонов. 
 */
public class Taxon
{
    public final Map<String, TaxonTerritoryFraction> territories = new HashMap<>();
    private final String name;
    private final int year;
    public static final TaxonTerritoryFraction FractionONE = new TaxonTerritoryFraction(1.0);

    public static final String Астраханская_оседлое = "Астраханская (оседлое население)";
    public static final String Астраханская_кочевники = "Астраханская (кочевники)";

    public Taxon(String name, int year)
    {
        this.name = name;
        this.year = year;
    }

    private Taxon add(String name) throws Exception
    {
        return add(name, FractionONE);
    }

    private Taxon add(String name, TaxonTerritoryFraction fraction) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(name);
        territories.put(name, fraction);
        return this;
    }

    private Taxon add(String name, double fraction) throws Exception
    {
        return add(name, new TaxonTerritoryFraction(fraction));
    }

    private Taxon remove(String name) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(name);
        if (!territories.containsKey(name))
            throw new Exception("Таксон не содержит территории " + name);
        territories.remove(name);
        return this;
    }

    private Taxon set(String name, double pecrentage) throws Exception
    {
        return set(name, TaxonTerritoryFraction.percent(pecrentage));
    }

    private Taxon set(String name, TaxonTerritoryFraction fraction) throws Exception
    {
        TerritoryNames.checkValidTerritoryName(name);
        if (!territories.containsKey(name))
            throw new Exception("Таксон не содержит территории " + name);
        territories.put(name, fraction);
        return this;
    }

    public static Taxon of(String name, int year, TerritoryDataSet tds) throws Exception
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
                    .add("Варшавская с Варшавой")
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
            t.add("Люблинская с Седлецкой и Холмской");
            break;

        case "неземские губернии":
            t.add("Архангельская")
                    .add("Астраханская")
                    .add(Taxon.Астраханская_оседлое)
                    .add(Taxon.Астраханская_кочевники)
                    .add("Виленская")
                    .add("Витебская")
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
                    .add("Московская с Москвой")
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
                    .add("Санкт-Петербургская с Санкт-Петербургом")
                    .add("г. Санкт-Петербург")
                    .add("Саратовская")
                    .add("Симбирская")
                    .add("Смоленская")
                    .add("Таврическая")
                    .add("Таврическая с Севастополем")
                    .add("г. Севастополь")
                    .add("Тамбовская")
                    .add("Тверская")
                    .add("Тульская")
                    .add("Уфимская")
                    .add("Харьковская")
                    .add("Херсонская")
                    .add("Херсонская с Одессой")
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
                    .add("Бакинская с Баку")
                    .add("Батумская")
                    .add("Дагестанская обл.")
                    .add("Елисаветпольская")
                    .add("Карсская обл.")
                    .add("Кубанская обл.")
                    .add("Кутаисская")
                    .add("Кутаисская с Батумской")
                    .add("Ставропольская")
                    .add("Терская обл.")
                    .add("Тифлисская")
                    .add("Черноморская")
                    .add("Эриванская")
                    .add("Закатальский окр.")
                    .add("Сухумский окр.");
            break;

        case "Сибирь":
            t.add("Амурская обл.")
                    .add("Енисейская")
                    .add("Забайкальская обл.")
                    .add("Иркутская")
                    .add("Камчатская обл.")
                    .add("Иркутская")
                    .add("Приморская обл.")
                    .add("Приморская обл. с Камчатской обл.")
                    .add("Сахалин")
                    .add("Тобольская")
                    .add("Томская")
                    .add("Якутская обл.");
            break;

        case "Финляндия":
            t.add("Або-Бьерноборгская")
                    .add("Вазаская")
                    .add("Выборгская")
                    .add("Куопиоская")
                    .add("Нюландская")
                    .add("С.-Михельская")
                    .add("Тавастгусская")
                    .add("Улеаборгская");
            break;

        case "Империя с Финляндией":
            t.add("Империя")
                    .add("Финляндия");
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
            break;

        case "Белоруссия":
            t.add("Витебская")
                    .add("Гродненская")
                    .add("Смоленская")
                    .add("Могилевская")
                    .add("Минская");
            break;

        case "Белоруссия без Смоленской":
            t.add("Витебская")
                    .add("Гродненская")
                    .add("Могилевская")
                    .add("Минская");
            break;

        case "Литва":
            t.add("Виленская")
                    .add("Ковенская");
            break;

        case "Малороссия":
            t.add("Волынская")
                    .add("Киевская")
                    .add("Подольская")
                    .add("Полтавская")
                    .add("Черниговская");
            break;

        case "Новороссия":
            t.add("Бессарабская")
                    .add("Екатеринославская")
                    .add("Область войска Донского")
                    .add("Херсонская")
                    .add("Херсонская с Одессой")
                    .add("г. Одесса")
                    .add("г. Николаев")
                    .add("Таврическая")
                    .add("Таврическая с Севастополем")
                    .add("г. Севастополь");
            break;

        case "РСФСР-1991":
            t.add("Архангельская")
                    .add("Астраханская")
                    .add(Taxon.Астраханская_оседлое)
                    .add(Taxon.Астраханская_кочевники)
                    .add("Владимирская")
                    .add("Вологодская")
                    .add("Воронежская")
                    .add("Вятская")
                    .add("Казанская")
                    .add("Калужская")
                    .add("Костромская")
                    .add("Курская")
                    .add("Московская")
                    .add("Московская с Москвой")
                    .add("г. Москва")
                    .add("Нижегородская")
                    .add("Новгородская")
                    .add("Область войска Донского")
                    .add("Олонецкая")
                    .add("Оренбургская")
                    .add("Орловская")
                    .add("Пензенская")
                    .add("Пермская")
                    .add("Санкт-Петербургская")
                    .add("Санкт-Петербургская с Санкт-Петербургом")
                    .add("г. Санкт-Петербург")
                    .add("Псковская")
                    .add("Рязанская")
                    .add("Самарская")
                    .add("Саратовская")
                    .add("Симбирская")
                    .add("Смоленская")
                    .add("Тамбовская")
                    .add("Тверская")
                    .add("Тульская")
                    .add("Уфимская")
                    .add("Ярославская")
                    .add("Выборгская", TaxonTerritoryFraction.percent(70))
                    .add("Сибирь")
                    .add("Акмолинская обл.", TaxonTerritoryFraction.percent(1897, 11.5, 1913, 17))
                    .add("Тургайская обл.", TaxonTerritoryFraction.percent(1897, 0.5, 1913, 3.5))
                    .add("Ставропольская")
                    .add("Черноморская")
                    .add("Кубанская обл.")
                    .add("Дагестанская обл.")
                    .add("Терская обл.");
            if (year >= 1914)
                t.add("Ростовское и./Д град.");
            break;

        case "Европейская часть РСФСР-1991":
            t.add("Архангельская")
                    .add("Астраханская")
                    .add(Taxon.Астраханская_оседлое)
                    .add(Taxon.Астраханская_кочевники)
                    .add("Владимирская")
                    .add("Вологодская")
                    .add("Воронежская")
                    .add("Вятская")
                    .add("Казанская")
                    .add("Калужская")
                    .add("Костромская")
                    .add("Курская")
                    .add("Московская")
                    .add("Московская с Москвой")
                    .add("г. Москва")
                    .add("Нижегородская")
                    .add("Новгородская")
                    .add("Область войска Донского")
                    .add("Олонецкая")
                    .add("Оренбургская")
                    .add("Орловская")
                    .add("Пензенская")
                    .add("Пермская")
                    .add("Санкт-Петербургская")
                    .add("Санкт-Петербургская с Санкт-Петербургом")
                    .add("г. Санкт-Петербург")
                    .add("Псковская")
                    .add("Рязанская")
                    .add("Самарская")
                    .add("Саратовская")
                    .add("Симбирская")
                    .add("Смоленская")
                    .add("Тамбовская")
                    .add("Тверская")
                    .add("Тульская")
                    .add("Уфимская")
                    .add("Ярославская")
                    .add("Выборгская", 0.7)
                    .add("Ставропольская")
                    .add("Черноморская")
                    .add("Кубанская обл.")
                    .add("Дагестанская обл.")
                    .add("Терская обл.");
            if (year >= 1914)
                t.add("Ростовское и./Д град.");
            break;

        case "русские губернии Европейской России и Кавказа, кроме Черноморской":
            t.add_rus_evro_kavkaz(year);
            break;

        case "СССР-1991":
            t.add_ussr_1991(year);
            break;

        case "СССР-1926":
            // ### в СССР 1991 -- set("Батумская обл.", 0.5) ### кутаисская с батумской
            t.add_ussr_1991(year);
            // ### flatten
            t.remove("Курляндская")
                    .remove("Лифляндская")
                    .remove("Эстляндская")
                    .remove("Холмская") // ### if contains
                    .remove("Виленская")
                    .remove("Ковенская")
                    .remove("Гродненская")
                    .remove("Бессарабская")
                    .remove("Карсская обл.") // ### if contains
                    .remove("Выборгская")
                    .set("Волынская", 49.3)
                    .set("Минская", 66.6)
                    .set("Витебская", 66.9)
                    .set("Псковская", 81.5)
                    // ### кутаисская с батумской
                    .set("Батумская обл.", 0.5);
            break;
        }

        if (t.territories.size() == 0)
            t = null;

        return t;
    }

    private void add50(int year) throws Exception
    {
        add("Архангельская");
        add("Астраханская");
        add(Taxon.Астраханская_оседлое);
        add(Taxon.Астраханская_кочевники);
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
        add("Московская с Москвой");
        add("г. Москва");
        add("Нижегородская");
        add("Новгородская");
        add("Олонецкая");
        add("Оренбургская");
        add("Орловская");
        add("Пензенская");
        add("Пермская");
        add("Санкт-Петербургская");
        add("Санкт-Петербургская с Санкт-Петербургом");
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
        add("Таврическая с Севастополем");
        add("г. Севастополь");
        add("Тамбовская");
        add("Тверская");
        add("Тульская");
        add("Уфимская");
        add("Харьковская");
        add("Херсонская");
        add("Херсонская с Одессой");
        add("г. Николаев");
        add("г. Одесса");
        add("Черниговская");
        add("Эстляндская");
        add("Ярославская");
        if (year >= 1914)
            add("Ростовское и./Д град.");
    }

    private void add_rus_evro_kavkaz(int year) throws Exception
    {
        add("Архангельская");
        add("Астраханская");
        add(Taxon.Астраханская_оседлое);
        add(Taxon.Астраханская_кочевники);
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
        add("Костромская");
        add("Курская");
        add("Минская");
        add("Могилевская");
        add("Московская");
        add("Московская с Москвой");
        add("г. Москва");
        add("Нижегородская");
        add("Новгородская");
        add("Олонецкая");
        add("Оренбургская");
        add("Орловская");
        add("Пензенская");
        add("Пермская");
        add("Санкт-Петербургская");
        add("Санкт-Петербургская с Санкт-Петербургом");
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
        add("Таврическая с Севастополем");
        add("г. Севастополь");
        add("Тамбовская");
        add("Тверская");
        add("Тульская");
        add("Уфимская");
        add("Харьковская");
        add("Херсонская");
        add("Херсонская с Одессой");
        add("г. Николаев");
        add("г. Одесса");
        add("Черниговская");
        add("Ярославская");

        add("Кубанская обл.");
        add("Ставропольская");

        if (year >= 1913)
            add("Холмская");

        if (year >= 1914)
            add("Ростовское и./Д град.");
    }

    private void add_ussr_1991(int year) throws Exception
    {
        add("50 губерний Европейской России");
        add("Выборгская", 0.7);

        add("Сибирь");
        add("Средняя Азия");

        // Кавказ кроме Карсской области
        add("г. Баку");
        add("Бакинская");
        add("Бакинская с Баку");
        add("Батумская");
        add("Дагестанская обл.");
        add("Елисаветпольская");
        add("Кубанская обл.");
        add("Кутаисская");
        add("Кутаисская с Батумской");
        add("Ставропольская");
        add("Терская обл.");
        add("Тифлисская");
        add("Черноморская");
        add("Эриванская");
        add("Закатальский окр.");
        add("Сухумский окр.");
    }

    /* =================================================================================================== */

    /*
     * Имена всех составных таксонов входящих в данный таксон, рекурсивно
     */
    public Set<String> allCompositeSubTaxons(boolean includeSelf, TerritoryDataSet tds) throws Exception
    {
        Set<String> xs = new HashSet<String>();
        allCompositeSubTaxons(xs, tds);
        if (includeSelf)
            xs.add(name);
        return xs;
    }

    private void allCompositeSubTaxons(Set<String> xs, TerritoryDataSet tds) throws Exception
    {
        for (String xname : territories.keySet())
        {
            Taxon tx = of(xname, year, tds);
            if (tx != null)
                xs.addAll(tx.allCompositeSubTaxons(true, tds));
        }
    }

    public static final Set<String> CompositeTaxons = Set.of("Империя",
                                                             "Европейская Россия",
                                                             "51 губерния Европейской России",
                                                             "50 губерний Европейской России",
                                                             "Остзейские губернии",
                                                             "привислинские губернии",
                                                             "земские губернии",
                                                             "неземские губернии",
                                                             "Азиатская Россия",
                                                             "Кавказ",
                                                             "Сибирь",
                                                             "Средняя Азия",
                                                             "Финляндия",
                                                             "Империя с Финляндией",
                                                             "2 Среднеазиатских областей",
                                                             "34 старо-земских губерний",
                                                             "45 губерний Европейской России",
                                                             "Московский промышленный район",
                                                             "Нижневолжский район",
                                                             "Новороссийский район",
                                                             "Средне-волжский район",
                                                             "Центральный земледельческий район",
                                                             "Юго-западный район",
                                                             "белорусский район",
                                                             "литовский район",
                                                             "малороссийский район",
                                                             "приозерный район",
                                                             "приуральский район",
                                                             "северный район",
                                                             "РСФСР-1991");

    /*
     * Проверить, является ли поименованная территория составным таксоном
     */
    public static boolean isComposite(String name)
    {
        return CompositeTaxons.contains(name);
    }

    static public Set<String> eliminateComposite(Collection<String> names)
    {
        Set<String> xs = new HashSet<String>();
        for (String name : names)
        {
            if (!isComposite(name))
                xs.add(name);
        }
        return xs;
    }

    /*
     * Редуцировать таксон до базовых областей
     */
    public Taxon flatten(TerritoryDataSet tds, int year) throws Exception
    {
        Taxon tx = new Taxon(name, year);
        flatten(tx.territories, FractionONE, tds, year);
        tx.weedOutCities(tds, year);
        return tx;
    }

    private void flatten(Map<String, TaxonTerritoryFraction> out, TaxonTerritoryFraction pweight, TerritoryDataSet tds, int year) throws Exception
    {
        for (String tname : territories.keySet())
        {
            TaxonTerritoryFraction weight = territories.get(tname);

            if (weight == FractionONE && pweight == FractionONE)
            {
                weight = FractionONE;
            }
            else if (pweight == FractionONE)
            {
                // just use weight
            }
            else if (weight == FractionONE)
            {
                // use pweight
                weight = pweight;
            }
            else
            {
                weight = new TaxonTerritoryFraction(weight, pweight);
            }

            if (!isComposite(tname))
            {
                if (out.containsKey(tname))
                    throw new Exception("Overlapping taxons");
                out.put(tname, weight);
            }
            else
            {
                Taxon t2 = Taxon.of(tname, year, tds).flatten(tds, year);
                t2.flatten(out, weight, tds, year);
            }
        }
    }

    private void weedOutCities(TerritoryDataSet tds, int year)
    {
        for (MergeDescriptor md : MergeCities.MergeCitiesDescriptors)
        {
            if (md.parent != null)
                weed(tds, year, md.combined, md.parent, md.childrenAsArray());
        }
    }

    private void weed(TerritoryDataSet tds, int year, String dstname, String srcname, String... cities)
    {
        if (territories.containsKey(dstname) && tds.containsKey(dstname) && tds.get(dstname).hasYear(year))
        {
            if (srcname != null)
                territories.remove(srcname);
            for (String city : cities)
                territories.remove(city);
        }
    }

    private static Taxon TaxonFinland = null;

    private static synchronized Taxon taxonFinland() throws Exception
    {
        if (TaxonFinland == null)
            TaxonFinland = Taxon.of("Финляндия", 1913, null);
        return TaxonFinland;
    }

    public static boolean isFinland(String tname) throws Exception
    {
        return taxonFinland().territories.containsKey(tname);
    }
}
