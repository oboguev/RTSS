package rtss.mexico.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import rtss.un.wpp.WPP;
import rtss.un.wpp.WPP2024;
import rtss.util.Util;

public class MexPopulationWpp
{
    public static void main(String[] args)
    {
        try
        {
            org.apache.poi.util.IOUtils.setByteArrayMaxOverride(300_000_000);

            if (Util.True)
            {
                Util.out("=======================================================");
                Util.out("Население Мексики на начало и середину года (UN WPP):");
                Util.out("");
                new MexPopulationWpp().do_wpp();
            }
        }
        catch (Throwable ex)
        {
            Util.err("*** Exception");
            ex.printStackTrace();
        }
    }
    
    private void do_wpp() throws Exception
    {
        Util.out("год на-начало на-середину");

        try (WPP wpp = new WPP2024())
        {
            Map<Integer, Map<String, Object>> mx = wpp.forCountry("Mexico");
            List<Integer> years = new ArrayList<>(mx.keySet());
            Collections.sort(years);
            for (int year : years)
            {
                Double pi = null;
                Double pm = null;
                
                Map<String, Object> m = mx.get(year);
                for (String key : m.keySet())
                {
                    if (key.toLowerCase().contains("Total Population, as of 1 January".toLowerCase()))
                    {
                        pi = WPP.asDouble(m.get(key));
                    }
                    else if (key.toLowerCase().contains("Total Population, as of 1 July ".toLowerCase()))
                    {
                        pm = WPP.asDouble(m.get(key));
                    }
                }
                
                Util.out(String.format("%d %s %s", year, n2s(pi), n2s(pm)));
            }
        }
    }

    private String n2s(Double v)
    {
        if (v == null)
            return "-";
        else
            return String.format("%,3d", Math.round(v));
    }
}
