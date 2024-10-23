package rtss.pre1917.tools;

import java.util.ArrayList;
import java.util.List;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.util.WeightedAverage;
import rtss.util.Util;

public class FlagUnderRegistration extends ShowAreaValues
{
    public static void main(String[] args)
    {
        try
        {
            new FlagUnderRegistration().flagUnderRegistration();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private static final double PROMILLE = 1000.0;

    protected FlagUnderRegistration() throws Exception
    {
    }

    private void flagUnderRegistration() throws Exception
    {
        List<String> ignored = new ArrayList<>();

        for (String tname : Util.sort(tdsUGVI.keySet()))
        {
            if (Taxon.isComposite(tname))
                continue;

            try
            {
                flagUnderRegistration(tname);
            }
            catch (IgnoreException ex)
            {
                ignored.add(tname);
            }
        }

        if (ignored.size() != 0)
        {
            Util.out("");
            for (String tname : ignored)
                Util.out("Ignored: " + tname);
        }
    }

    private void flagUnderRegistration(String tname) throws Exception
    {
        Territory t = tdsUGVI.get(tname);
        WeightedAverage wa_cbr = new WeightedAverage();
        WeightedAverage wa_cdr = new WeightedAverage();

        for (int year = 1911; year <= 1913; year++)
        {
            wa_cbr.add(cbr(t, year), 1.0);
            wa_cdr.add(cdr(t, year), 1.0);
        }

        double cbr_2 = wa_cbr.doubleResult();
        double cdr_2 = wa_cdr.doubleResult();

        wa_cbr.reset();
        wa_cdr.reset();

        boolean flag = false;

        for (int year = 1896; year <= 1899; year++)
        {
            double cbr = cbr(t, year);
            double cdr = cdr(t, year);

            wa_cbr.add(cbr, 1.0);
            wa_cdr.add(cdr, 1.0);

            double threshold = 0.95;

            if (cbr < threshold * cbr_2)
                flag = true;

            if (cdr < threshold * cdr_2)
                flag = true;
        }

        double cbr_1 = wa_cbr.doubleResult();
        double cdr_1 = wa_cdr.doubleResult();

        if (cbr_1 < cbr_2)
            flag = true;

        if (cdr_1 < cdr_2)
            flag = true;

        if (flag)
            Util.out("Flagged: " + tname);
    }

    private double cbr(Territory t, int year) throws Exception
    {
        TerritoryYear ty = t.territoryYearOrNull(year);
        if (ty == null)
            throw new IgnoreException();

        Long pop = ty.progressive_population.total.both;
        Long births = ty.births.total.both;
        if (pop == null || births == null)
            throw new IgnoreException();

        return (PROMILLE * births) / pop;
    }

    private double cdr(Territory t, int year) throws Exception
    {
        TerritoryYear ty = t.territoryYearOrNull(year);
        if (ty == null)
            throw new IgnoreException();

        Long pop = ty.progressive_population.total.both;
        Long deaths = ty.deaths.total.both;
        if (pop == null || deaths == null)
            throw new IgnoreException();

        return (PROMILLE * deaths) / pop;
    }

    public static class IgnoreException extends Exception
    {
    }
}
