package rtss.pre1917.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

/*
 * Проверить, что для территорий имеются данные, и начиная с какого года
 */
public class VerifyTerriroryAvailableData
{
    public static void main(String[] args)
    {
        try
        {
            new VerifyTerriroryAvailableData().do_main();
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        TerritoryDataSet tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY,
                                                       LoadOptions.MERGE_CITIES
                                                       // , LoadOptions.MERGE_POST1897_REGIONS
                                                       );
        Taxon taxon = Taxon.of("Империя", 1913, tds);
        taxon = taxon.flatten(tds, 1913);
        List<String> tnames = new ArrayList<String>(taxon.territories.keySet());
        Collections.sort(tnames);
        
        tds.leaveOnlyTotalBoth();

        for (String tname : tnames)
            explore(tds, tname);
    }

    private void explore(TerritoryDataSet tds, String tname)
    {
        Territory t = tds.get(tname);

        if (t == null)
        {
            Util.out(String.format("%s missing", tname));
            return;
        }

        List<Integer> years = t.years();
        int y0 = years.get(0);
        String gaps = gaps(years);
        String nodata = nodata(t);
        
        if (y0 == 1881 && gaps.equals("none") && nodata.equals(""))
        {
            // Util.out(String.format("%s full", tname));
        }
        else if (gaps.equals("none") && nodata.equals(""))
        {
            Util.out(String.format("%s from %d", tname, years.get(0)));
        }
        else if (nodata.equals(""))
        {
            Util.out(String.format("%s from %d gaps: %s", tname, years.get(0), gaps));
        }
        else if (gaps.equals("none"))
        {
            Util.out(String.format("%s from %d nodata: %s", tname, years.get(0), nodata));
        }
        else 
        {
            Util.out(String.format("%s from %d gaps: %s nodata: %s", tname, years.get(0), gaps, nodata));
        }
    }

    private String gaps(List<Integer> years)
    {
        if (years == null || years.size() < 2)
        {
            return "none";
        }

        List<Integer> sortedYears = years.stream()
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .toList();

        if (sortedYears.size() < 2)
        {
            return "none";
        }

        StringBuilder gaps = new StringBuilder();

        for (int i = 1; i < sortedYears.size(); i++)
        {
            int previous = sortedYears.get(i - 1);
            int current = sortedYears.get(i);

            for (int year = previous + 1; year < current; year++)
            {
                if (!gaps.isEmpty())
                    gaps.append(' ');

                gaps.append(year);
            }
        }

        return gaps.isEmpty() ? "none" : gaps.toString();
    }
    
    private String nodata(Territory t)
    {
        StringBuilder sb = new StringBuilder();
        
        for (int year : t.years())
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            if (ty == null || year >= 1914)
                continue;
            
            if (ty.births.total.both == null || ty.deaths.total.both == null)
            {
                if (sb.length() != 0)
                    sb.append(" ");
                sb.append("" + year);
            }
        }
        
        return sb.toString();
    }
}


/****************************************************************

Кутаисская с Батумской:
- Кутаисская from 1886
- Батумская from 1903

Приморская обл. с Камчатской обл.:
- Приморская обл. full
- Камчатская обл. from 1909

Люблинская с Седлецкой и Холмской:
- Люблинская full
- Седлецкая full
- Холмская from 1913

Область войска Донского from 1882 => за 1881 взято по Временнику на 1881 год

Средняя Азия (с 1888)
- Закаспийская обл. from 1888
- Самаркандская обл. from 1888
- Тургайская обл. from 1882
- Ферганская обл. from 1882 gaps: 1886 1887 
- Сыр-Дарьинская обл. from 1882 gaps: 1887
- Семиреченская обл. from 1882

Кавказ-СССР (с 1886)
- Бакинская с Баку from 1886
- Елисаветпольская from 1886
- Карсская обл. from 1888
- Тифлисская from 1886
- Эриванская from 1886

Кавказ-РСФСР (с 1886)
- Дагестанская обл. from 1886
- Терская обл. from 1886
- Кубанская обл. from 1886
- Кутаисская from 1886
- Ставропольская from 1886
- Черноморская from 1888 gaps: 1890 1891 1892 1893 1894 1895 1896 => ###выделить из Кубанской

- Амурская обл. from 1881 gaps: 1887 => ####интерполировать 1887 из соседних годов
- Сахалин from 1887 => ###скопировать в 1886 из 1887

- ####Астраханская from 1881 gaps: 1887 1888 1889 1890 1891 1892 1893 1894 1895
- Астраханская (кочевники) from 1886 gaps: 1887
- Астраханская (оседлое население) from 1885

..............



Астраханская from 1881 gaps: 1887 1888 1889 1890 1891 1892 1893 1894 1895 nodata: 1885
Астраханская (кочевники) from 1886 gaps: 1887 nodata: 1886 1888 1889 1890 1891 1894 1896
Астраханская (оседлое население) from 1885 nodata: 1886 1896

Область войска Донского from 1881 nodata: 1885

Забайкальская обл. from 1881 nodata: 1882 1884 1886 1888

Калишская from 1881 nodata: 1913
Келецкая from 1881 nodata: 1913
Ломжинская from 1881 nodata: 1913
Петроковская from 1881 nodata: 1913
Седлецкая from 1881 nodata: 1913

Кутаисская from 1886 nodata: 1904 1905
Карсская обл. from 1888 nodata: 1888 1889 1913
Черноморская from 1888 gaps: 1890 1891 1892 1893 1894 1895 1896 nodata: 1889
Эриванская from 1886 nodata: 1888

Приморская обл. from 1881 nodata: 1882 1883 1884 1888
Сахалин from 1887 nodata: 1903 1904 1905 1906 1907
Якутская обл. from 1881 nodata: 1882
Амурская обл. from 1881 gaps: 1887 nodata: 1883 1884 1886 1888 1889 1890 1891 1892

Закаспийская обл. from 1888 nodata: 1888 1889 1891 1893
Самаркандская обл. from 1888 nodata: 1888 1889 1892 1893 1894 1902 1903 1904 1905 1906 1907 1908 1909 1910 1911 1912 1913
Семипалатинская обл. from 1881 nodata: 1884 1885 1889 1913

Сыр-Дарьинская обл. from 1882 gaps: 1887 nodata: 1882 1883 1884 1885 1886 1888 1889 1892 1902 1903 1904 1905
Тургайская обл. from 1882 nodata: 1882 1883 1884
Ферганская обл. from 1882 gaps: 1886 1887 nodata: 1883 1885

..............

Империя с 1888
РСФСР-1991 с 1886
СССР-1991 с 1888
СССР-1926 с 1888
Европейская часть РСФСР-1991 c 1886
Сибирь c 1881
Новороссия с 1881
Малороссия с 1881
Белоруссия с 1881
Белоруссия без Смоленской с 1881
Литва с 1881
Кавказ 1886
Средняя Азия с 1888
привислинские губернии с 1881
Остзейские губернии с 1881
50 губерний Европейской России 1881

*/
