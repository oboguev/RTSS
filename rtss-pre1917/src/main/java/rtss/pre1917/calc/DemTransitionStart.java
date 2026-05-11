package rtss.pre1917.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import rtss.math.trend.Trendline;
import rtss.pre1917.calc.EvalCountryTaxon.Options;
import rtss.pre1917.calc.containers.TaxonYearData;
import rtss.pre1917.calc.containers.TaxonYearlyPopulationData;
import rtss.util.Util;

public class DemTransitionStart
{
    public static void main(String[] args)
    {
        try
        {
            EvalCountryTaxon.warmup();
            
            // new DemTransitionStart().calc("Новороссия", 1914, null);

            new DemTransitionStart().calc("РСФСР-1991", 1914);
            new DemTransitionStart().calc("Новороссия", 1914);
            new DemTransitionStart().calc("Малороссия", 1913);
            new DemTransitionStart().calc("Белоруссия", 1913);
            new DemTransitionStart().calc("Белоруссия без Смоленской", 1913);
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }

    }

    private void calc(String tname, int toYear) throws Exception
    {
        calc(tname, toYear, null);

        int[] excludeYears = { 1898, 1905, 1908, 1909, 1910 };
        calc(tname, toYear, asList(excludeYears));
    }

    private List<Integer> asList(int[] v)
    {
        return Arrays.stream(v).boxed().collect(Collectors.toList());
    }

    private void calc(String txname, int toYear, List<Integer> excludeYears) throws Exception
    {
        Util.out("");
        Util.out("***************************************");
        Util.out("");
        Util.out("Тренд рождаемости для " + txname);
        if (excludeYears == null || excludeYears.size() == 0)
            Util.out("без исключения лет");
        else
            Util.out("с исключением лет: " + excludeYears.toString());

        List<Double> ax = new ArrayList<Double>();
        List<Double> ay = new ArrayList<Double>();

        TaxonYearlyPopulationData yds = EvalCountryTaxon.calc(txname, toYear, Options.SILENT);
        for (int year : yds.years())
        {
            TaxonYearData yd = yds.get(year);
            
            if (yd.cbr_middle == null)
                continue;

            if (excludeYears != null && excludeYears.contains(year))
                continue;

            ax.add(year + 0.5);
            ay.add(yd.cbr_middle);
        }

        Trendline tl = Trendline.create(ax, ay);
        
        double t1896 = tl.predict(1896.5);
        double p90 = t1896 * 0.9;
        double tryear = (p90 - tl.b) / tl.a;
        
        Util.out(String.format("Начало перехода: %.1f", tryear));
        
        if (Util.False)
        {
            for (int year = 1896; year <= 1921; year++)
            {
                TaxonYearData yd = yds.get(year);

                double x = year + 0.5;
                Util.out(String.format("%.1f [%5.2f] -> %.2f [%6.2f%%]", 
                                       x, 
                                       yd != null && yd.cbr_middle != null ? yd.cbr_middle : null, 
                                       tl.predict(x), 100.0 * tl.predict(x) / t1896));
            }
            
            Util.noop();
        }
    }
}
