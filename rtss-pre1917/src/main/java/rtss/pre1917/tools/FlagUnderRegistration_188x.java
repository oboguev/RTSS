package rtss.pre1917.tools;

import rtss.data.selectors.BirthDeath;
import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.util.WeightedAverage;
import rtss.util.Util;

/*
 * Выявить территории с заниженной регистрацией рождений и смертей
 * в 1881-1885 гг сравнительно с средним за 1886-1889 
 */
public class FlagUnderRegistration_188x
{
    public static void main(String[] args)
    {
        try
        {
            Util.out("Контроль в 1881-1885 гг. по среднему за 1886-1889");
            Util.out("");
            new FlagUnderRegistration_188x(0.12).flagUnderRegistration();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private final TerritoryDataSet tds;
    private final double threshold;

    private FlagUnderRegistration_188x(double threshold) throws Exception
    {
        this.threshold = threshold;
        this.tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY,
                                           LoadOptions.EVAL_SPLIT_ASTRAKHAN,
                                           LoadOptions.EVAL_PROGRESSIVE_ASTRAKHAN_ONLY);
    }

    private void flagUnderRegistration() throws Exception
    {
        for (String tname : Util.sort(tds.keySet()))
        {
            if (Taxon.isComposite(tname))
                continue;
            
            switch (tname)
            {
            case "Холмская":
            case "Черноморская":
            case "Сыр-Дарьинская":
            case "Самаркандская обл.":
            case "Карсская обл.":
            case "Камчатская обл.":
            case "Закаспийская обл.":
            case "Астраханская (кочевники)":
            case "Ростовское и./Д град.":
                continue;
            }

            if (flagUnderRegistration(tname, BirthDeath.BIRTH) | flagUnderRegistration(tname, BirthDeath.DEATH))
                Util.out("");
        }

    }

    private boolean flagUnderRegistration(String tname, BirthDeath what) throws Exception
    {
        Territory t = tds.get(tname);
        WeightedAverage wa = new WeightedAverage();

        for (int year = 1886; year <= 1889; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            if (what == BirthDeath.BIRTH)
            {
                if (ty != null && ty.births.total.both != null)
                    wa.add(ty.births.total.both, 1.0);
            }
            else
            {
                if (ty != null && ty.deaths.total.both != null)
                    wa.add(ty.deaths.total.both, 1.0);
            }
        }

        boolean printed = false;

        if (wa.count() == 0)
        {
            Util.err("No 1886-1889 data for " + tname);
            return true;
        }

        for (int year = 1881; year <= 1885; year++)
        {
            TerritoryYear ty = t.territoryYearOrNull(year);
            if (ty == null)
                continue;
            Long count = (what == BirthDeath.BIRTH) ? ty.births.total.both : ty.deaths.total.both;
            if (count == null)
                continue;
            if (Math.abs(count - wa.doubleResult()) > wa.doubleResult() * threshold)
            {
                Util.out(String.format("%s %d %ss %,d vs. later average %,d", tname, year, what.name().toLowerCase(), count, Math.round(wa.doubleResult())));
                printed = true;
            }
        }

        return printed;
    }
}
