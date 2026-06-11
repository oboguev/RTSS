package rtss.un.wpp.latinamerica;

import java.util.HashMap;
import java.util.Map;

public class CountryNames
{
    private static Map<String,String> r2e = new HashMap<>();
    
    public static String r2e(String rname)
    {
        defineCountries();
        return r2e.get(rname);
    }
    
    private static void defineCountry(String rname, String ename)
    {
        r2e.put(rname, ename);
    }

    private static void defineCountries()
    {
        if (r2e.size() != 0)
            return;
        
        defineCountry("Аргентина", "Argentina");
        defineCountry("Боливия", "Bolivia (Plurinational State of)");
        defineCountry("Бразилия", "Brazil");
        defineCountry("Венесуэла", "Venezuela (Bolivarian Republic of)");
        defineCountry("Венецуэлла", "Venezuela (Bolivarian Republic of)");
        defineCountry("Гаити", "Haiti");
        defineCountry("Гватемала", "Guatemala");
        defineCountry("Гондурас", "Honduras");
        defineCountry("Доминиканская республика", "Dominican Republic");
        defineCountry("Колумбия", "Colombia");
        defineCountry("Коста Рика", "Costa Rica");
        defineCountry("Коста-Рика", "Costa Rica");
        defineCountry("Куба", "Cuba");
        defineCountry("Мексика", "Mexico");
        defineCountry("Никарагуа", "Nicaragua");
        defineCountry("Панама", "Panama");
        defineCountry("Парагвай", "Paraguay");
        defineCountry("Перу", "Peru");
        defineCountry("Пуэрто Рико", "Puerto Rico");
        defineCountry("Пуэрто-Рико", "Puerto Rico");
        defineCountry("Сальвадор", "El Salvador");
        defineCountry("Уругвай", "Uruguay");
        defineCountry("Чили", "Chile");
        defineCountry("Эквадор", "Ecuador");
        defineCountry("Эль Сальвадор", "El Salvador");
        defineCountry("Эль-Сальвадор", "El Salvador");
    }
}
