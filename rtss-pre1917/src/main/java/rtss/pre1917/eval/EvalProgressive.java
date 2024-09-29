package rtss.pre1917.eval;

import rtss.pre1917.data.DataSetType;
import rtss.pre1917.data.TerritoryDataSet;

/*
 * Вычислить progressive_population отчётом от переписи 1897 года с приложением ежегодных
 * сведений о числе рождений, смертей и внутренней миграции. Только для набора данных УГВИ.
 */
public class EvalProgressive
{
    private final TerritoryDataSet tds;

    public EvalProgressive(TerritoryDataSet tds) throws Exception
    {
        if (tds.dataSetType != DataSetType.UGVI)
            throw new Exception("Прогрессивная численность населения может быть расчитана только для набора УГВИ");
        this.tds = tds;
    }

    public void evalProgressive() throws Exception
    {
        // ###
    }
}
