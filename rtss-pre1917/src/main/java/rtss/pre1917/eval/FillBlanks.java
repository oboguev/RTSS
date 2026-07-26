package rtss.pre1917.eval;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.util.Util;

public class FillBlanks extends FillBlanksMethod
{
    private final TerritoryDataSet tdsCensus;
    private final Territory t;
    private final TotalMigration totalMigration = TotalMigration.getTotalMigration();

    private boolean displayWarnings = true;

    public FillBlanks(Territory t, TerritoryDataSet tdsCensus) throws Exception
    {
        this.t = t;
        this.tdsCensus = tdsCensus;
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
        Territory tCensus = tdsCensus.get(censusTerritoryName(t.name));
        if (tCensus == null)
            throw new IllegalArgumentException("Нельзя заполнить пробелы для " + t.name);
        TerritoryYear tyCensus = tCensus.territoryYearOrNull(1897);
        long censusPopulation = tyCensus.population.total.both;

        TerritoryYear xty1896 = t.territoryYearOrNull(1896);
        TerritoryYear xty1897 = t.territoryYearOrNull(1897);

        long in = xty1897.births.total.both - xty1897.deaths.total.both;
        in += migration(1897);
        long in1 = Math.round(in * 27.0 / 365.0);
        // long in2 = in - in1;

        long p1897 = censusPopulation - in1;
        // long p1898 = censusPopulation + in2;

        in = xty1896.births.total.both - xty1896.deaths.total.both;
        in += migration(1896);
        long p1896 = p1897 - in;

        return p1896;
    }

    @Override
    protected void setBirhts(int year, long births)
    {
        TerritoryYear ty = t.territoryYear(year);
        if (ty.births.total.both == null)
            ty.births.total.both = births;
    }

    @Override
    protected void setDeaths(int year, long deaths)
    {
        TerritoryYear ty = t.territoryYear(year);
        if (ty.deaths.total.both == null)
            ty.deaths.total.both = deaths;
    }

    private String censusTerritoryName(String tname)
    {
        return tname;
    }

    @Override
    protected void warning(String message)
    {
        if (displayWarnings)
            Util.err("FillBlanks: " + message + " for " + t.name);
    }

    public FillBlanks displayWarnings(boolean displayWarnings)
    {
        this.displayWarnings = displayWarnings;
        return this;
    }
}
