package rtss.latinamerica.lambda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountryName
{
    private static Map<String, String> rname2ename = new HashMap<>();
    private static Map<String, String> ename2rname = new HashMap<>();
    private static Map<String, String> ename2code = new HashMap<>();

    static
    {
        define("Аргентина", "Argentina");
        define("Бразилия", "Brazil");
        define("Чили", "Chile");
        define("Колумбия", "Colombia");
        define("Коста-Рика", "Costa Rica");
        define("Куба", "Cuba");
        define("Доминиканская республик", "Dominican Republic");
        define("Эквадор", "Ecuador");
        define("Сальвадор", "El Salvador");
        define("Гватемала", "Guatemala");
        define("Гондурас", "Honduras");
        define("Мексика", "Mexico");
        define("Никарагуа", "Nicaragua");
        define("Панама", "Panama");
        define("Парагвай", "Paraguay");
        define("Перу", "Peru");
        define("Уругвай", "Uruguay");
        define("Венесуэла", "Venezuela");
    }

    private static void define(String rname, String ename)
    {
        rname2ename.put(rname, ename);
        ename2rname.put(ename, rname);
        String code = ename.replace(" ", "").substring(0, 3);
        ename2code.put(ename, code);
    }

    public static String r2e(String rname)
    {
        return rname2ename.get(rname);
    }

    public static String e2r(String ename)
    {
        return ename2rname.get(ename);
    }

    public static String e2code(String ename)
    {
        return ename2code.get(ename);
    }

    public static List<String> rnames()
    {
        List<String> list = new ArrayList<>(rname2ename.keySet());
        Collections.sort(list);
        return list;
    }

    public static List<String> enames()
    {
        List<String> list = new ArrayList<>(ename2rname.keySet());
        Collections.sort(list);
        return list;
    }

    public static String ename(String cname)
    {
        if (ename2rname.containsKey(cname))
            return cname;
        else if (r2e(cname) != null)
            return r2e(cname);
        else
            throw new IllegalArgumentException("Unknown country name: " + cname);
    }

    public static String rname(String cname)
    {
        if (rname2ename.containsKey(cname))
            return cname;
        else if (e2r(cname) != null)
            return e2r(cname);
        else
            throw new IllegalArgumentException("Unknown country name: " + cname);
    }
}
