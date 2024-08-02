package rtss.pre1917;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class CrossVerify
{
    public void verify(Map<String, Territory> territories)
    {
        calc_1893(territories);
        check_population_jump(territories);
        
        
        // ### population jump year-to-next over 2%
        // ### population jump  mismatching births - deaths
        // ### implied (calculated) CBR or CDR mismatching listed
        // ### back-calculate population
    }
    
    /**
     * Вычислить население на начало 1893 года по косвенным данным
     */
    private void calc_1893(Map<String, Territory> territories)
    {
        for (Territory ter : territories.values())
        {
            TerritoryYear t93 = ter.territoryYear(1893);
            TerritoryYear t94 = ter.territoryYear(1894);
            
            if (t93.population == null && t93.births != null && t93.deaths != null && t94.population != null)
            {
                t93.population = t94.population - (t93.births - t93.deaths);
            }
            else if (t93.population == null && t93.cdr != null && t93.cbr != null && t94.population != null)
            {
                double v = 1 + (t93.cbr - t93.cdr) / 1000.0;
                t93.population = Math.round(t94.population / v); 
            }
            else
            {
                Util.noop();
            }
        }
    }
    
    private void check_population_jump(Map<String, Territory> territories)
    {
        List<String> msgs = new ArrayList<>(); 
        
        for (Territory ter : territories.values())
        {
            int previous_year = -1;
            long previous_population = 0;
            for (int year = 1893; year <= 1914; year++)
            {
                TerritoryYear ty = ter.territoryYear(year);
                
                if (ty.population != null)
                {
                    if (previous_year != - 1)
                    {
                        double v = (double) ty.population / previous_population;
                        double vv = Math.pow(v, 1.0 / (year - previous_year));
                        if (vv < 0.9 || vv > 1.15)
                        {
                            String s = String.format("Population jump %d-%d %s by %.3f, yearly %.3f", 
                                                     previous_year, year, ter.name, v, vv);
                            msgs.add(s);
                        }
                    }
                    
                    previous_year = year;
                    previous_population = ty.population;
                }
            }
        }
        
        Collections.sort(msgs);
        for (String s : msgs)
            Util.err(s);
    }
}
