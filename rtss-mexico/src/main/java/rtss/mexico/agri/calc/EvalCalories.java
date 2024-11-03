package rtss.mexico.agri.calc;

import java.util.HashMap;
import java.util.Map;

import rtss.mexico.agri.entities.CaloricContent;
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
        new Preprocess().preprocess(cs);

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
    }
}
