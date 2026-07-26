package rtss.pre1917.eval;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.util.Util;

public class FillBlanks extends FillBlanksMethod
{
    private final TerritoryDataSet census;
    private final Territory t;
    private final TotalMigration totalMigration = TotalMigration.getTotalMigration();

    public FillBlanks(Territory t, TerritoryDataSet census) throws Exception
    {
        this.t = t;
        this.census = census;
    }

    @Override
    protected Long birhts(int year)
    {
        TerritoryYear ty = t.territoryYearOrNull(year);
        return ty == null ? null : ty.births.total.both;
    }

    @Override
    protected Long deaths(int year)
    {
        TerritoryYear ty = t.territoryYearOrNull(year);
        return ty == null ? null : ty.deaths.total.both;
    }

    @Override
    protected long migration(int year)
    {
        try
        {
            return totalMigration.saldo(t.name, year);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }
    }

    @Override
    protected long population1896()
    {
        Territory tCensus = census.get(censusTerritoryName(t.name));
        if (tCensus == null)
            throw new IllegalArgumentException("Нельзя заполнить пробелы для " + t.name);
        TerritoryYear tyCensus = tCensus.territoryYearOrNull(1897);
        long censusPopulation = tyCensus.population.total.both;

        TerritoryYear xty1896 = t.territoryYearOrNull(1896);
        TerritoryYear xty1897 = t.territoryYearOrNull(1897);

        long in = xty1897.births.total.both - xty1897.deaths.total.both;
        in += migration(1897);
        long in1 = Math.round(in * 27.0 / 365.0);
        long in2 = in - in1;

        long p1897 = censusPopulation - in1;
        long p1898 = censusPopulation + in2;

        in = xty1896.births.total.both - xty1896.deaths.total.both;
        in += migration(1896);
        long p1896 = p1897 - in;

        return p1896;
    }

    @Override
    protected void setBirhts(int year, long births)
    {
        TerritoryYear ty = t.territoryYear(year);
        ty.births.total.both = births;

    }

    @Override
    protected void setDeaths(int year, long deaths)
    {
        TerritoryYear ty = t.territoryYear(year);
        ty.deaths.total.both = deaths;
    }

    private String censusTerritoryName(String tname)
    {
        return tname;
    }
}
