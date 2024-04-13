package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.InterpolateMortalityTable;

/**
 * Вычислить таблицу смертности населения СССР для 1940 года.
 * 
 * К сожалению, АДХ не опубликовали вычисленные и использованные ими возрастные показатели смертности, в т.ч. для 1940 года.
 * Насколько можно судить, они полагали их близкими по величение к госкомстатовскому расчёту для 1938-1939 гг.:
 * 
 *     "Расчет возрастных чисел умерших проводился в несколько этапов путем передвижки населения 1939 г. с использованием 
 *      модифицированных таблиц смертности 1938-1939 гг., так, чтобы при данных числах живущих в каждом возрасте они давали 
 *      заданное общее число умерших" (АДХ-СССР, стр. 54).
 *      
 * В работе АДХ-РСФСР они затем пришли к существенно большей оценке детской смертности.
 * 
 * Использование структуры населения для начала 1940 года продвинутой от переписи 1939 года по таблице ГКС-СССР-1938, 
 * а также уровней рождаемости и смертности вычисленных АДХ для 1940 года даёт итоговую таблицу составленную 
 * на 92.9% из ГКС-СССР-1938 и на 7.1% из АДХ-РСФСР-1940.
 *      
 * #### 78
 * 
 */
public class USSR_MortalityTable_1940  extends UtilBase_194x
{
    public CombinedMortalityTable evaluate() throws Exception
    {
        CombinedMortalityTable mt1 = CombinedMortalityTable.load("mortality_tables/USSR/1938-1939");
        mt1.comment("ГКС-СССР-1938");

        CombinedMortalityTable mt2 = CombinedMortalityTable.loadTotal("mortality_tables/RSFSR/1940");
        mt2.comment("АДХ-РСФСР-1940");

        return InterpolateMortalityTable.forTargetRates(mt1, mt2, new USSR_Population_In_Early_1940().evaluate(), USSR_CBR_1940, USSR_CDR_1940);
    }
}
