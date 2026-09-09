package rtss.pre1917.export;

import rtss.pre1917.data.TerritoryDataSet;

public class ExportCharts
{
    private final TerritoryDataSet tdsElementaryPopulation;
    private final TerritoryDataSet tdsCompositeTaxonsPopulation;
    private final TerritoryDataSet tdsCompositeTaxonsVitalRates;

    public ExportCharts(TerritoryDataSet tdsElementaryPopulation, TerritoryDataSet tdsCompositeTaxonsPopulation, TerritoryDataSet tdsCompositeTaxonsVitalRates)
    {
        this.tdsElementaryPopulation = tdsElementaryPopulation;
        this.tdsCompositeTaxonsPopulation = tdsCompositeTaxonsPopulation;
        this.tdsCompositeTaxonsVitalRates = tdsCompositeTaxonsVitalRates;
    }
    
    public void export() throws Exception
    {
        // ### экспортировать числа и rates для элементарных территорий (tdsElementaryPopulation)
        // ### экспортировать числа и rates для таксонов (для них два вида населения: tdsCompositeTaxonsPopulation tdsCompositeTaxonsVitalRates)
    }
}
