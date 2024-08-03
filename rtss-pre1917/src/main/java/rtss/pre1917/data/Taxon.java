package rtss.pre1917.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Taxon
{
    public final Map<String, Double> territories = new HashMap<>();
    private String name;
    private int year;

    private Taxon add(String name)
    {
        return add(name, 1.0);
    }

    private Taxon add(String name, double fraction)
    {
        territories.put(name, fraction);
        return this;
    }

    public static Taxon of(String name, int year)
    {
        Taxon t = new Taxon();
        t.name = name;
        t.year = year;

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

    private void add50(int year)
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
    
    public Set<String> allCompositeSubTaxons(boolean self)
    {
        Set<String> xs = new HashSet<String>();
        allCompositeSubTaxons(xs);
        if (self)
            xs.add(name);
        return xs;
    }
    
    private void allCompositeSubTaxons(Set<String> xs)
    {
        for (String xname : territories.keySet())
        {
            Taxon tx = of(xname, year);
            if (tx != null)
                xs.addAll(tx.allCompositeSubTaxons(true));
        }
    }
}
