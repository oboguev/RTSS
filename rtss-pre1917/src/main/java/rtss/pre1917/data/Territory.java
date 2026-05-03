package rtss.pre1917.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.math.algorithms.MathUtil;
import rtss.pre1917.data.migration.TotalMigration;
import rtss.util.Util;

public class Territory
{
    public final String name;
    public Boolean hasValidVitalRate;
    private Map<Integer, TerritoryYear> year2value = new HashMap<>();

    public Territory(String name)
    {
        this.name = name;
    }

    public TerritoryYear territoryYear(int year)
    {
        TerritoryYear ty = year2value.get(year);

        if (ty == null)
        {
            ty = new TerritoryYear(this, year);
            year2value.put(year, ty);
        }

        return ty;
    }

    public TerritoryYear territoryYearOrNull(int year)
    {
        return year2value.get(year);
    }

    public void copyYear(TerritoryYear ty)
    {
        ty = ty.dup(this);
        year2value.put(ty.year, ty);
    }

    public boolean hasYear(int year)
    {
        return year2value.containsKey(year);
    }

    public void removeYear(int year)
    {
        year2value.remove(year);
    }

    public Territory dup()
    {
        return dup(name);
    }

    public Territory dup(String name)
    {
        Territory t = new Territory(name);
        t.hasValidVitalRate = hasValidVitalRate;

        for (int year : year2value.keySet())
        {
            TerritoryYear ty = year2value.get(year);
            ty = ty.dup(t);
            t.year2value.put(year, ty);
        }

        return t;
    }

    public List<Integer> years()
    {
        List<Integer> list = new ArrayList<>(year2value.keySet());
        Collections.sort(list);
        return list;
    }

    public int minYear(int dflt)
    {
        int res = -1;

        for (int year : years())
        {
            if (res == -1)
                res = year;
            else
                res = Math.min(res, year);
        }

        if (res == -1)
            res = dflt;

        return res;
    }

    public int maxYear(int dflt)
    {
        int res = -1;

        for (int year : years())
        {
            if (res == -1)
                res = year;
            else
                res = Math.max(res, year);
        }

        if (res == -1)
            res = dflt;

        return res;
    }

    public String toString()
    {
        return name;
    }

    public void adjustFemaleBirths()
    {
        for (TerritoryYear ty : year2value.values())
            ty.adjustFemaleBirths();
    }

    public void leaveOnlyTotalBoth()
    {
        for (TerritoryYear ty : year2value.values())
            ty.leaveOnlyTotalBoth();
    }

    /*
     * Изменить progressive_population.total.both начиная с года (@year + 1) и во все последующие годы на величину @delta.
     * Соответствует дополнительному приросту (или потерям) населения за год @year. 
     */
    public void cascadeAdjustProgressivePopulation(int year, long delta)
    {
        for (int y : years())
        {
            TerritoryYear ty = this.territoryYearOrNull(y);
            if (ty != null && y >= year + 1 && ty.progressive_population.total.both != null)
                ty.progressive_population.total.both += delta;
        }
    }

    /*
     * В году @year произошли дополнительные @extraDeaths смертей
     */
    public void extraDeaths(int year, long extraDeaths)
    {
        for (int y : years())
        {
            TerritoryYear ty = this.territoryYearOrNull(y);
            if (ty != null)
            {
                if (y == year && ty.deaths.total.both != null)
                    ty.deaths.total.both += extraDeaths;

                if (y > year && ty.population.total.both != null && Util.False)
                    ty.population.total.both -= extraDeaths;

                if (y > year && ty.progressive_population.total.both != null)
                    ty.progressive_population.total.both -= extraDeaths;
            }
        }
    }

    private static final double PROMILLE = 1000.0;

    public double calc_mid_CBR_total_both(int year) throws Exception
    {
        return calc_mid_CBR_total_both(year, false);
    }
    
    public Double calc_mid_CBR_total_both(int year, boolean allowNull) throws Exception
    {
        try
        {
            TerritoryYear ty = territoryYearOrNull(year);
            if (ty.births.total.both == null && allowNull)
                return null;

            return (PROMILLE * ty.births.total.both) / calc_avg_year_progressive_population_total_both(year);
        }
        catch (Exception ex)
        {
            throw new Exception(String.format("Ошибка вычисления среднегодового CBR %s в %d году", name, year), ex);
        }
    }

    public double calc_mid_CDR_total_both(int year) throws Exception
    {
        return calc_mid_CDR_total_both(year, false);
    }
    
    public Double calc_mid_CDR_total_both(int year, boolean allowNull) throws Exception
    {
        try
        {
            TerritoryYear ty = territoryYearOrNull(year);
            if (ty.deaths.total.both == null && allowNull)
                return null;

            return (PROMILLE * ty.deaths.total.both) / calc_avg_year_progressive_population_total_both(year);
        }
        catch (Exception ex)
        {
            throw new Exception(String.format("Ошибка вычисления среднегодового CDR %s в %d году", name, year), ex);
        }
    }

    public long calc_avg_year_progressive_population_total_both(int year) throws Exception
    {
        return calc_avg_year_progressive_population_total_both(year, false);
    }

    public Long calc_avg_year_progressive_population_total_both(int year, boolean allowNull) throws Exception
    {
        try
        {
            TerritoryYear ty = territoryYearOrNull(year);

            Long pop1 = ty.progressive_population.total.both;
            Long pop2 = null;

            TerritoryYear ty2 = territoryYearOrNull(year + 1);
            if (ty2 != null &&
                ty2.progressive_population != null &&
                ty2.progressive_population.total != null &&
                ty2.progressive_population.total.both != null)
            {
                pop2 = ty2.progressive_population.total.both;
            }
            else
            {
                long migr = ty.migration.total.both;
                if (migr == 0)
                    migr = TotalMigration.getTotalMigration().saldo(name, year);

                long incr = ty.births.total.both - ty.deaths.total.both + migr;
                pop2 = pop1 + incr;
            }

            return MathUtil.log_average(pop1, pop2);
        }
        catch (Exception ex)
        {
            if (allowNull)
            {
                Util.err(String.format("Ошибка вычисления среднегодового населения %s в %d году", name, year));
                return null;
            }
            else
            {
                throw new Exception(String.format("Ошибка вычисления среднегодового населения %s в %d году", name, year), ex);
            }
        }
    }
}
