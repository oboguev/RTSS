package rtss.mexico.agri.calc;

import java.util.HashMap;
import java.util.Map;

import rtss.mexico.agri.entities.CaloricContent;
import rtss.mexico.agri.entities.CaloricContent.CaloricElement;
import rtss.mexico.agri.entities.CultureSet;
import rtss.mexico.population.MexPopulationCombineEstimates;
import rtss.util.Util;

/*
 * Потребление растительных калорий населением Мексики
 * на основе данных урожаев и сельсакохозяйственного экспорта и импорта.
 */
public class EvalCalories
{
    public static void main(String[] args)
    {
        try
        {
            new EvalCalories().eval();
            Util.out("** Done");
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private Map<Integer, Long> population = MexPopulationCombineEstimates.result();

    private EvalCalories() throws Exception
    {
    }

    private void eval() throws Exception
    {
        CultureSet cs = new MergeCultureSets().merge();
        new Preprocess(cs).preprocess();

        Map<Integer, CaloricContent> y2cc = new HashMap<>();

        for (int year = 1897; year <= 1982; year++)
        {
            long pop = (population.get(year) + population.get(year + 1)) / 2;
            CaloricContent cc = CaloricContent.eval(cs, year, pop);
            if (cc.size() != 0)
                y2cc.put(year, cc);
        }

        /* ============================================================================ */

        Util.out("Сырой расчёт без коррекции по наличным продуктам: ккал на человека в день:");
        Util.out("");
        for (int year : Util.sort(y2cc.keySet()))
        {
            Util.out(String.format("%d %.0f", year, y2cc.get(year).sum()));
        }

        /* ============================================================================ */

        Util.out("");
        Util.out("Расчёт с поправкой на отсутствующие данные: ккал на человека в день:");
        Util.out("");
        for (int year : Util.sort(y2cc.keySet()))
        {
            CaloricContent cc = y2cc.get(year);
            double cal = cc.sum();
            double scale = calcScaleUp(y2cc, cc, year);
            cal *= scale;
            Util.out(String.format("%d %.0f (scale=%.2f)", year, cal, scale));
        }

        /* ============================================================================ */

        if (Util.True)
        {
            final int diff_year1 = 1961; 
            final int diff_year2 = 1981; 
            CaloricContent cc1 = y2cc.get(diff_year1);
            CaloricContent cc2 = y2cc.get(diff_year2);
            Util.out("");
            Util.out(String.format("Разница между %d и %d :", diff_year1, diff_year2));
            Util.out(String.format("Калории в %d: %.0f ", diff_year1, cc1.sum()));
            Util.out(String.format("Калории в %d: %.0f ", diff_year2, cc2.sum()));
            Util.out("");
            CaloricContent ccd = CaloricContent.sub(cc2, cc1);
            for (CaloricElement ce : ccd.sort())
            {
                Util.out(String.format("%s %.0f", ce.name, ce.calories));
            }
        }

    }

    private double calcScaleUp(Map<Integer, CaloricContent> y2cc, CaloricContent cc, int ccyear) throws Exception
    {
        if (ccyear >= 1927)
            return 1.0;
        
        double v = 0;
        int nyears = 0;

        for (int year = 1927; year <= 1932; year++)
        {
            CaloricContent xcc = y2cc.get(year);
            double all = xcc.sum();
            double select = xcc.sum(cc.keySet());
            v += all / select;
            nyears++;
        }

        return v / nyears;
    }
}
