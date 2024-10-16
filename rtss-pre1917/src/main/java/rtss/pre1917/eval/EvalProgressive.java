package rtss.pre1917.eval;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.DataSetType;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;

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
 * Она, однако, может быть вычислена для суммы составных частей (см. LoadOptions.MERGE_POST1897_REGIONS).
 * 
 */
public class EvalProgressive
{
    private final TotalMigration totalMigration = TotalMigration.getTotalMigration();
    private final TerritoryDataSet census;

    private final TerritoryDataSet tds;
    private final TerritoryDataSet xtds;

    public EvalProgressive(TerritoryDataSet tds) throws Exception
    {
        if (tds.dataSetType != DataSetType.UGVI)
            throw new Exception("Прогрессивная численность населения может быть расчитана только для набора УГВИ");
        this.tds = tds;
        this.xtds = tds.dup();
        xtds.adjustFemaleBirths();
        new FillMissingBD(xtds).fillMissingBD();
        census = new LoadData().loadCensus1897(tds.loadOptions.toArray(new LoadOptions[0]));
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
        return tname;
    }

    private void evalProgressive(String tname, Territory tCensus) throws Exception
    {
        TerritoryYear tyCensus = tCensus.territoryYearOrNull(1897);

        Territory t = tds.get(tname);
        TerritoryYear ty1896 = t.territoryYearOrNull(1896);
        TerritoryYear ty1897 = t.territoryYearOrNull(1897);
        TerritoryYear ty1898 = t.territoryYearOrNull(1898);

        Territory xt = xtds.get(tname);
        TerritoryYear xty1896 = xt.territoryYearOrNull(1896);
        TerritoryYear xty1897 = xt.territoryYearOrNull(1897);

        long in = xty1897.births.total.both - xty1897.deaths.total.both;
        in += totalMigration.saldo(tname, 1897);
        long in1 = Math.round(in * 27.0 / 365.0);
        long in2 = in - in1;

        ty1897.progressive_population.total.both = tyCensus.population.total.both - in1;
        ty1898.progressive_population.total.both = tyCensus.population.total.both + in2;

        if (xty1896 != null)
        {
            in = xty1896.births.total.both - xty1896.deaths.total.both;
            in += totalMigration.saldo(tname, 1896);
            ty1896.progressive_population.total.both = ty1897.progressive_population.total.both - in;
        }

        for (int year = 1898; year <= 1916; year++)
        {
            TerritoryYear xty = xt.territoryYearOrNull(year);
            TerritoryYear ty = t.territoryYearOrNull(year);
            TerritoryYear ty_next = t.territoryYearOrNull(year + 1);

            if (ty == null || ty_next == null || xty == null)
                continue;

            in = null2zero(xty.births.total.both) - null2zero(xty.deaths.total.both);
            in += totalMigration.saldo(tname, year);

            ty_next.progressive_population.total.both = ty.progressive_population.total.both + in;
        }
    }

    private long null2zero(Long v)
    {
        return v == null ? 0 : v;
    }
}
