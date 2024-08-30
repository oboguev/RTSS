package rtss.pre1917.eval;

import java.lang.reflect.Field;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.util.WeightedAverage;

public class MergeTaxon
{
    private TerritoryDataSet territories;
    
    public MergeTaxon(TerritoryDataSet territories)
    {
        this.territories = territories;
    }
    
    public Territory mergeTeaxon(String txname) throws Exception
    {
        Territory src = territories.get(txname);
        Territory res = new Territory(txname); 
        
        for (int year : src.years())
        {
            Taxon tx = Taxon.of(txname, year, territories);
            tx = tx.flatten(territories.dataSetType);
            
            TerritoryYear ty = res.territoryYear(year);
            
            sum_ur(ty, "population", tx);
            sum_ur(ty, "midyear_population", tx);
            sum_ur(ty, "births", tx);
            sum_ur(ty, "deaths", tx);
            
            rate(ty, "cbr", tx);
            rate(ty, "cdr", tx);
            
            if (ty.cbr != null && ty.cdr != null)
            {
                ty.ngr = ty.cbr - ty.cdr;
            }
            else
            {
                ty.ngr = null;
            }
        }

        return res;
    }
    
    private void sum_ur(TerritoryYear ty, String selector, Taxon tx) throws Exception
    {
        sum_vg(ty, selector + ".total", tx);
        sum_vg(ty, selector + ".rural", tx);
        sum_vg(ty, selector + ".urban", tx);
    }

    private void sum_vg(TerritoryYear ty, String selector, Taxon tx) throws Exception
    {
        sum_long(ty, selector + ".male", tx);
        sum_long(ty, selector + ".female", tx);
        sum_long(ty, selector + ".both", tx);
    }

    private void sum_long(TerritoryYear ty, String selector, Taxon tx) throws Exception
    {
        double res = 0;
        int count = 0;
        
        for (String tname : tx.territories.keySet())
        {
            Territory ter2 = territories.get(tname);
            if (ter2 != null && ter2.hasYear(ty.year))
            {
                TerritoryYear ty2 = ter2.territoryYear(ty.year);
                Long lv = getLong(ty2, selector);
                if (lv != null)
                {
                    res += lv * tx.territories.get(tname);
                    count++;
                }
            }
        }
        
        if (count != 0)
            setLong(ty, selector, Math.round(res));
    }
    
    private void rate(TerritoryYear ty, String selector, Taxon tx) throws Exception
    {
        WeightedAverage wa = new WeightedAverage(); 
        
        for (String tname : tx.territories.keySet())
        {
            Territory ter2 = territories.get(tname);
            if (ter2 != null && ter2.hasYear(ty.year))
            {
                TerritoryYear ty2 = ter2.territoryYear(ty.year);
                
                Long pop = ty2.population.total.both;
                if (pop == null)
                    pop = ty2.midyear_population.total.both;

                Double rate = getDouble(ty2, selector);
                
                if (pop != null && rate != null)
                    wa.add(rate, pop);
            }
        }
        
        if (wa.count() != 0)
            setDouble(ty, selector, wa.doubleResult());
    }
    
    /* ================================================================================ */
    
    private Long getLong(Object o, String selector) throws Exception
    {
        String[] sa = selector.split(".");
        
        for (String s : sa)
        {
            Field field = o.getClass().getDeclaredField(s);    
            field.setAccessible(true);
            o = field.get(o);            
        }

        return (Long) o;
    }

    private void setLong(Object o, String selector, Long value) throws Exception
    {
        String[] sa = selector.split(".");
        
        for (int k = 0; k < sa.length; k++)
        {
            String s = sa[k];
            
            Field field = o.getClass().getDeclaredField(s);    
            field.setAccessible(true);

            if (k == sa.length - 1)
            {
                field.set(o, value);
            }
            else
            {
                o = field.get(o);            
            }
        }
    }

    private Double getDouble(Object o, String selector) throws Exception
    {
        String[] sa = selector.split(".");
        
        for (String s : sa)
        {
            Field field = o.getClass().getDeclaredField(s);    
            field.setAccessible(true);
            o = field.get(o);            
        }

        return (Double) o;
    }

    private void setDouble(Object o, String selector, Double value) throws Exception
    {
        String[] sa = selector.split(".");
        
        for (int k = 0; k < sa.length; k++)
        {
            String s = sa[k];
            
            Field field = o.getClass().getDeclaredField(s);    
            field.setAccessible(true);

            if (k == sa.length - 1)
            {
                field.set(o, value);
            }
            else
            {
                o = field.get(o);            
            }
        }
    }
}
