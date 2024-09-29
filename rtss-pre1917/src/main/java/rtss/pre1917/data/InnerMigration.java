package rtss.pre1917.data;

import java.util.HashMap;
import java.util.Map;

public class InnerMigration
{
    public static class InnerMigrationAmount
    {
        public InnerMigrationAmount(String tname)
        {
            this.tname = tname;
        }

        private String tname;
        Map<Integer, Long> year2inflow = new HashMap<>();
        Map<Integer, Long> year2outflow = new HashMap<>();;
    }

    private Map<String, InnerMigrationAmount> tname2ima = new HashMap<>();

    public void setInFlow(String tname, int year, long amount) throws Exception
    {
        setValue(makeInnerMigrationAmount(tname).year2inflow, year, amount);
    }

    public void setOutFlow(String tname, int year, long amount) throws Exception
    {
        setValue(makeInnerMigrationAmount(tname).year2outflow, year, amount);
    }

    private InnerMigrationAmount makeInnerMigrationAmount(String tname)
    {
        InnerMigrationAmount ima = tname2ima.get(tname);

        if (ima == null)
        {
            ima = new InnerMigrationAmount(tname);
            tname2ima.put(tname, ima);
        }

        return ima;

    }

    private void setValue(Map<Integer, Long> year2flow, int year, long amount) throws Exception
    {
        if (year2flow.containsKey(year))
            throw new Exception("Dupicate data");

        year2flow.put(year, amount);
    }

    private String mapTerritoryName(String tname)
    {
        switch (tname)
        {
        case "Бакинская с Баку":
            tname = "Бакинская";
            break;

        case "Варшавская с Варшавой":
            tname = "Варшавская";
            break;

        case "Московская с Москвой":
            tname = "Московская";
            break;

        case "Санкт-Петербургская с Санкт-Петербургом":
            tname = "Санкт-Петербургская";
            break;

        case "Таврическая с Севастополем":
            tname = "Таврическая";
            break;

        case "Херсонская с Одессой":
            tname = "Херсонская";
            break;
        }

        return tname;
    }

    public long inFlow(String tname, int year)
    {
        InnerMigrationAmount ima = tname2ima.get(mapTerritoryName(tname));
        if (ima == null)
            return 0;

        Long v = ima.year2inflow.get(year);
        if (v == null)
            v = 0L;

        return v;
    }

    public long outFlow(String tname, int year)
    {
        InnerMigrationAmount ima = tname2ima.get(mapTerritoryName(tname));
        if (ima == null)
            return 0;

        Long v = ima.year2outflow.get(year);
        if (v == null)
            v = 0L;

        return v;
    }
}
