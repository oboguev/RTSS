package rtss.pre1917;

import java.util.Objects;

public class TerritoryNameYearKey
{
    public final String territoryName;
    public final int year;

    public TerritoryNameYearKey(String territoryName, int year)
    {
        this.territoryName = territoryName;
        this.year = year;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if (!(o instanceof TerritoryNameYearKey))
        {
            return false;
        }

        TerritoryNameYearKey other = (TerritoryNameYearKey) o;

        return year == other.year
               && Objects.equals(territoryName, other.territoryName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(territoryName, year);
    }

    @Override
    public String toString()
    {
        return territoryName + " " + year;
    }
}