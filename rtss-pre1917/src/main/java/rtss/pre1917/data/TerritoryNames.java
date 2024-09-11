package rtss.pre1917.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rtss.util.Util;

/**
 * Губернии, области и территории России
 */
public class TerritoryNames
{
    public static Set<String> seen = new HashSet<>();
    public static Map<String, String> aliases = new HashMap<>();

    static
    {
        defineAliases();
    }

    public static String canonic(String ter) throws Exception
    {
        ter = ter.replace("(", " ");
        ter = ter.replace(")", " ");
        
        ter = Util.despace(ter).trim();
        if (ter.equals(""))
            return "";

        ter = ter.replace("c", "с"); // latin c with Cyrillic с
        ter = ter.replace("o", "о"); // latin o with Cyrillic о
        ter = ter.replace("P", "Р"); // latin P with Cyrillic р
        ter = ter.replace("p", "р"); // latin p with Cyrillic р
        ter = ter.replace("і", "и");
        ter = ter.replace("i", "и");
        ter = ter.replace("ѣ", "е");
        ter = ter.replace("ъ ", " ");
        ter = ter.replace("ъ-", "-");
        if (ter.endsWith("ъ"))
            ter = Util.stripTail(ter, "ъ");
        ter = ter.replace("въ т.ч. ", "");
        ter = ter.replace("в т.ч. ", "");
        ter = ter.replace(" вь ", " в ");
        ter = ter.replace("Итога ", "Итого ");
        ter = ter.replace("Итого в ", "");
        ter = ter.replace("Итого ва ", "");
        ter = ter.replace("Итого на ", "");
        ter = ter.replace("Всего в ", "");
        ter = ter.replace("Среднее для ", "");
        ter = ter.replace("Среднее дли ", "");

        ter = ter.replace("С-Петербург", "Санкт-Петербург");
        ter = ter.replace("С-Петербург", "Санкт-Петербург");
        ter = ter.replace("Петроград", "Санкт-Петербург");
        ter = ter.replace("Петроградская", "Санкт-Петербургская");
        ter = ter.replace("Петроградская", "Санкт-Петербургская");
        ter = ter.replace("С.-Петербургcкая", "Санкт-Петербургская");
        ter = ter.replace("С.-Петербургская", "Санкт-Петербургская");
        ter = ter.replace("с Петербургом", "с Санкт-Петербургом");

        if (ter.startsWith("В "))
            ter = Util.stripStart(ter, "В ");
        if (ter.startsWith("Во "))
            ter = Util.stripStart(ter, "Во ");
        if (ter.startsWith("На "))
            ter = Util.stripStart(ter, "На ");

        if (ter.startsWith("Г "))
            ter = "г. " + Util.stripStart(ter, "Г ");
        if (ter.startsWith("Г. "))
            ter = "г. " + Util.stripStart(ter, "Г. ");
        if (ter.startsWith("г "))
            ter = "г. " + Util.stripStart(ter, "г ");

        if (ter.endsWith(" г"))
            ter = Util.stripTail(ter, " г") + " губ.";
        if (ter.endsWith(" г."))
            ter = Util.stripTail(ter, " г.") + " губ.";
        if (ter.endsWith(" губ"))
            ter = Util.stripTail(ter, " губ") + " губ.";
        if (ter.endsWith(" -"))
            ter = Util.stripTail(ter, " -");

        if (ter.endsWith(" обл"))
            ter = Util.stripTail(ter, " обл") + " обл.";

        if (aliases.containsKey(ter))
            ter = aliases.get(ter);

        if (ter.endsWith(" губ."))
            ter = Util.stripTail(ter, " губ.");
        if (ter.endsWith(" губ"))
            ter = Util.stripTail(ter, " губ");

        seen.add(ter);
        
        if (!getValidTerritoryNames().contains(ter))
            throw new Exception("Invalid territory name [" + ter + "]");

        return ter;
    }

    public static List<String> getSeen()
    {
        List<String> list = new ArrayList<>(seen);
        Collections.sort(list);
        return list;
    }

    public static void printSeen()
    {
        for (String s : getSeen())
            Util.out(s);
    }

    public static void aliases(String name, String... xaliases)
    {
        for (String x : xaliases)
            aliases.put(x, name);
    }

    private static void defineAliases()
    {
        aliases("Империя", "Империи", "всей Империи");
        aliases("Азиатская Россия", "азиатская Россия", "Азиатской России");
        aliases("Средняя Азия", "Средней Азии", "Среднеазиатские области", "Ср.-Азиатских областей");
        aliases("Сибирь", "Сибири");
        aliases("Кавказ", "Кавказе", "Кавказа");
        aliases("земские губернии", "земск губ.", "земских губ.", "земских губерний", "земских губерниях");
        aliases("неземские губернии", "неземск губ.", "неземских губ.",
                "неземских губерний", "неземских губериий", "неземских губерниях",
                "12 прежних неземских губерний", "12 прежних неземских губ.",
                "В 12 прежних неземских губ.", "В 12 прежних меземских губ.",
                "неземcких губ.", "неземcких", "неземских");
        aliases("34 старо-земских губерний", "34 старо-земских губ.", "т ч в 34 старо-земских губ.");
        aliases("50 губерний Европейской России", "50 губерний Европейской России");
        aliases("45 губерний Европейской России", "45 губ Евр России");
        aliases("51 губерния Европейской России", "51 губернии Европейской России");
        aliases("50 губерний Европейской России", "50 губерниях Европейской России");
        aliases("Европейская Россия", "европейская Россия", "Европейской России",
                "в Европейской России");
        aliases("привислинские губернии", "привислинские губ.", "привисленских губ.",
                "привислинск губ.", "привислинских губерний", "привислинcк губ.", "привислинских губ.",
                "привнслинск губ.", "привислинских губерниях", "Привислинские губ.");
        aliases("Остзейские губернии", "остзейские губернии", "остзейских губ.", "остзейских губерний",
                "остзейских", "остзейских губ.", "Остзейские", "прибалтийские губернии", "прибалтийск губ.",
                "остзейских губерниях");
        aliases("Средне-волжский район", "средне-волжском районе", "средне-волжском раойне");
        aliases("Центральный земледельческий район", "центральн земледел районе", "центральном земледел районе");
        aliases("Юго-западный район", "юго-западный район", "юго-западном районе", "юго-западном район");
        aliases("Новороссийский район", "новороссийский район", "новороссийском районе", "новороссийком район");
        aliases("Московский промышленный район", "Московском промышленном", "Московском промышленном р");
        aliases("малороссийский район", "малороссийском районе", "маоороссийском районе", "Малороссииском район");
        aliases("литовский район", "литовском", "литовском р");
        aliases("Нижневолжский район", "нижневолжском районе", "нижне-волжском район");
        aliases("приуральский район", "приуральском районе", "приурадьек", "приуральском р.");
        aliases("приозерный район", "приозерн р", "приозерн");
        aliases("северный район", "северном р", "северном");
        aliases("белорусский район", "белорусском районе");
                
        aliases("Або-Бьерноборгская", "Або - Бьерноборгская");
        aliases("Акмолинская обл.", "Акмолинская");
        aliases("Амурская обл.", "Амурская", "Амурмская");
        aliases("г. Баку", "Бакинское градонач", "Бакинское градояан");
        aliases("г. Варшава", "Варшава", "г. Варшава.");
        aliases("Виленская", "Виленская.");
        aliases("Витебская", "Витебсиая", "Витебска я");
        aliases("Гродненская", "гродненская");
        aliases("Дагестанская обл.", "Дагестанская");
        aliases("Елисаветпольская", "Елизаветпольская", "Елисаветнодьекая", "Елисаветпольскаа",
                "Елисаветпольсхая", "Елнсаветпольская", "Елисаветиольская");
        aliases("Екатеринославская", "Екатеранославская");
        aliases("Забайкальская обл.", "Забайкальская", "Забайкальсая", "Бабайкальская");
        aliases("Закаспийская обл.", "Закаспийская");
        aliases("Калишская", "Еалишск&я", "К-алишская", "Калипиская", "Калишcкая");
        aliases("Камчатская", "Камчатская обл.");
        aliases("Карсская обл.", "Карсская");
        aliases("Келецкая", "Кедецкая", "Келцкая");
        aliases("Ковенская", "Ковенсиая");
        aliases("Кубанская обл.", "Кубанская");
        aliases("Лифляндская", "ЛиФляндская", "Лифлфяндская", "Лифляанская", "Лифляндсксая",
                "Лифлянская", "Лифдяндская");
        aliases("Ломжинская", "Ломжинскаа", "Ломжинокая", "Ловшинская");
        aliases("Люблинская", "Люблянская");
        aliases("г. Москва", "г. Моcква", "Москва");
        aliases("г. Николаев", "Николаевское град", "Николаевское градон");
        aliases("Нюландская", "Нюландсвая");
        aliases("г. Одесса", "Одесса");

        aliases("Область войска Донского", "Донскаго в обл.", "Донскаго войска область", "Донская", 
                "Обл Войска Донского", "Области Войска Донского", "Донского войска обл",
                "Донского войска обл.", "Донск. войска обл.", "Обл в Донского", "Обл войска Донск", 
                "Обл войска Донскаго", "Обл войска Донского", "Обл. войска Донского");

        aliases("Петроковская", "Петрояовская");
        aliases("Плоцкая", "Плоцвая");
        aliases("Приморская обл.", "Приморская", "Приморекая");
        aliases("Радомская", "Радомекая", "Радомккая", "Радонская", "Раломская");

        aliases("Самаркандская обл.", "Самарканская", "Самаркандская");
        aliases("г. Санкт-Петербург", "г. Петербург", "г. Петроград", "г. С.-Петербург", "С.-Петербург");
        aliases("Сахалин", "Остр Сахалин", "Остров Сахалин", "Сахалинская",
                "Сев Сахалин", "Сев. Сахалин", "Остр. Сахалин");
        aliases("г. Севастополь", "Севастопольское", "Севастопольское градон");
        aliases("Седлецкая", "Седлцкая", "Седлевская", "Седледкая");
        aliases("Семиреченская обл.", "Семиреченская", "Семириченская", "Семриеченская", "Семвреченская", 
                "Семнреченская", "Сѳмиреченская", "Семиреченская");
        aliases("Семипалатинская обл.", "Семипалатинская");
        aliases("Смоленская", "Смоленсяая");
        aliases("Сыр-Дарьинская обл.", "Сьр-Дарьинская", "Сыр-Дарьинская", "Сыр-Даринская обл.",
                "Сыр-Даринсккая", "Сыр-Дарьинска я");
        
        aliases("Терская обл.", "Терская");
        aliases("Тифлисская", "Тисяисская");
        aliases("Тургайская обл.", "Тургайская", "Тургайсксая");
        aliases("Уральская обл.", "Уральская", "У ральская");
        aliases("Уфимская", "Уфимскя");
        aliases("Ферганская обл.", "Ферганская", "Фергранская");
        aliases("Херсонская", "Херссвская");
        aliases("Эстляндская", "Эстляндсхая");
        aliases("Якутская обл.", "Якутская", "Якуткая обл.");
        aliases("Ярославская", "Ярославская.");
    }

    private static final Set<String> validTerritoryNames = new HashSet<>();

    public static Set<String> getValidTerritoryNames() throws Exception
    {
        if (validTerritoryNames.size() == 0)
        {
            for (String s : Util.loadResource("valid-territory-names.txt").replace("\r\n", "\n").split("\n"))
            {
                s = s.trim();
                if (s.length() != 0)
                    validTerritoryNames.add(s);
            }
        }

        return validTerritoryNames;
    }
    
    public static void checkValidTerritoryName(String name) throws Exception
    {
        if (!getValidTerritoryNames().contains(name))
            throw new Exception("Inavlid territory name: " + name);
    }
}
