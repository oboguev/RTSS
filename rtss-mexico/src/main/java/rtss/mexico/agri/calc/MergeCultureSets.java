package rtss.mexico.agri.calc;

import rtss.mexico.agri.entities.Culture;
import rtss.mexico.agri.entities.CultureDefinition;
import rtss.mexico.agri.entities.CultureSet;
import rtss.mexico.agri.entities.CultureYear;
import rtss.mexico.agri.loader.CultureDefinitions;
import rtss.mexico.agri.loader.LoadAE;
import rtss.mexico.agri.loader.LoadCultureDefinitions;
import rtss.mexico.agri.loader.LoadEH;
import rtss.mexico.agri.loader.LoadSARH;
import rtss.util.Util;

public class MergeCultureSets
{
    public static void main(String[] args)
    {
        try
        {
            new MergeCultureSets().merge();
            Util.out("** Done");
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private CultureSet csAEEarly = LoadAE.loadEarly();
    private CultureSet csEH = LoadEH.load();
    private CultureSet csSARH = LoadSARH.load();
    private CultureDefinitions cds = LoadCultureDefinitions.load();

    public MergeCultureSets() throws Exception
    {
    }

    public CultureSet merge() throws Exception
    {
        // обрезать наборы по времени, 
        // устранить непищевые культуры и культуры без данных для указанных лет
        CultureSet cs = csSARH.dup();
        clip(cs, 1925, 1982);
        clip(csAEEarly, 1897, 1908);
        clip(csEH, 1897, 1908);

        // копировать данные для трёх культур из csAEEarly (где они точнее) в csEH
        override(csEH, csAEEarly, "cebada en grano");
        override(csEH, csAEEarly, "chile seco");
        override(csEH, csAEEarly, "chile verde");
        
        // влить годы из csEH в cs
        for (Culture cSrc : csEH.cultures())
        {
            Culture cDst = cs.get(cSrc.name);
            
            for (int year : cSrc.years())
            {
                CultureYear cySrc = cSrc.cultureYear(year);
                cDst.dupYear(cySrc);
            }
        }
        
        return cs;
    }

    private void clip(CultureSet cs, int y1, int y2) throws Exception
    {
        cs.deleteYearRange(0, y1 - 1);
        cs.deleteYearRange(y2 + 1, Integer.MAX_VALUE);

        // remove culture with no year data
        for (Culture c : cs.cultures())
        {
            if (c.years().size() == 0)
                cs.remove(c);
        }

        // remove cultures with no calories 
        for (Culture c : cs.cultures())
        {
            CultureDefinition cd = cds.get(c.name);
            if (cd.kcal_kg == null || cd.kcal_kg == 0)
                cs.remove(c);
        }
    }

    private void override(CultureSet csDst, CultureSet csSrc, String cname) throws Exception
    {
        Culture cDst = csDst.get(cname);
        Culture cSrc = csSrc.get(cname);

        for (int year : cSrc.years())
        {
            CultureYear cySrc = cSrc.cultureYear(year);
            CultureYear cyDst = cDst.cultureYear(year);
            
            cyDst.production = cySrc.production;
            cyDst.production_raw = cySrc.production_raw;
            cyDst.surface = cySrc.surface;
            cyDst.yield = cySrc.yield;
            cyDst.rice_kind = cySrc.rice_kind;
        }
    }
}
