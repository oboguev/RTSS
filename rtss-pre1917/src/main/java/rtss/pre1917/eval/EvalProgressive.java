package rtss.pre1917.eval;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.DataSetType;
import rtss.pre1917.data.InnerMigration;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;

/*
 * Вычислить progressive_population отчётом от переписи 1897 года с приложением ежегодных
 * сведений о числе рождений и смертей, а также внутренней миграции. 
 * Только для набора данных УГВИ и только после приложения MERGE_CITIES.
 * 
 * Прогрессивное население вычисляется только для базовых областей, не для таксонов.
 * Чтобы узнать прогрессивное население для таксона, нужно затем его агрегировать. 
 * 
 * Прогрессивная оценка не вычисляется для
 * Камчатской области (создана в 1909), Батумской области (создана в 1903), Холмской губернии (создана в 1912)
 * т.к. для промежутка между 1897 годом и моментом  их создания сведения о естественом движении 
 * не включены в базу (могут быть добавлены позднее, из уездных сведений УГВИ). 
 */
public class EvalProgressive
{
    private final TerritoryDataSet census = new LoadData().loadCensus1897(LoadOptions.DONT_VERIFY, LoadOptions.DONT_MERGE_CITIES);
    private final InnerMigration innerMigration = new LoadData().loadInnerMigration();

    private final TerritoryDataSet tds;
    private final TerritoryDataSet xtds;

    public EvalProgressive(TerritoryDataSet tds) throws Exception
    {
        if (tds.dataSetType != DataSetType.UGVI)
            throw new Exception("Прогрессивная численность населения может быть расчитана только для набора УГВИ");
        this.tds = tds;
        this.xtds = tds.dup();
        xtds.adjustBirths();
    }

    public void evalProgressive() throws Exception
    {
        for (String tname : tds.keySet())
        {
            if (Taxon.isComposite(tname))
                continue;

            Territory tCensus = census.get(censusTerritoryName(tname));
            if (tCensus == null)
                continue;
            
            evalProgressive(tname, tCensus);
        }
    }

    private String censusTerritoryName(String tname)
    {
        switch (tname)
        {
        case "Таврическая с Севастополем":
            tname = "Таврическая";
            break;

        case "Бакинская с Баку":
            tname = "Бакинская";
            break;
        }
        return tname;
    }
    
    private void evalProgressive(String tname, Territory tCensus) throws Exception
    {
        // ###
    }
}
