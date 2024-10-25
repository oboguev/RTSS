package rtss.pre1917.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.util.WeightedAverage;
import rtss.util.Util;

/*
 * Выявить территории с заниженной регистрацией рождений и смертей
 * в начале периода сравнительно с концом периода.
 */
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

    Set<String> skip = Set.of("Бакинская с Баку",
                              "Дагестанская обл.",
                              "Елисаветпольская",
                              "Закаспийская обл.",
                              "Карсская обл.",
                              "Кутаисская с Батумской",
                              "Семиреченская обл.",
                              "Сыр-Дарьинская обл.",
                              "Ферганская обл.",
                              "Черноморская",
                              "Самаркандская обл.");

    private static final double PROMILLE = 1000.0;

    protected FlagUnderRegistration() throws Exception
    {
        super(LoadOptions.MERGE_POST1897_REGIONS);
    }

    private void flagUnderRegistration() throws Exception
    {
        List<String> nodata = new ArrayList<>();

        for (String tname : Util.sort(tdsUGVI.keySet()))
        {
            if (Taxon.isComposite(tname) || skip.contains(tname))
                continue;

            try
            {
                flagUnderRegistration(tname);
            }
            catch (NoDataException ex)
            {
                nodata.add(tname);
            }
        }

        if (nodata.size() != 0)
        {
            Util.out("");
            for (String tname : nodata)
                Util.out("No data: " + tname);
        }
    }

    private void flagUnderRegistration(String tname) throws Exception
    {
        Territory t = tdsUGVI.get(tname);
        WeightedAverage wa_cbr = new WeightedAverage();
        WeightedAverage wa_cdr = new WeightedAverage();

        for (int year = 1909; year <= 1913; year++)
        {
            wa_cbr.add(cbr(t, year), 1.0);
            wa_cdr.add(cdr(t, year), 1.0);
        }

        double cbr_2 = wa_cbr.doubleResult();
        double cdr_2 = wa_cdr.doubleResult();

        wa_cbr.reset();
        wa_cdr.reset();
        
        boolean printed = false;

        for (int year = 1896; year <= 1902; year++)
        {
            boolean flag = false;

            double cbr = cbr(t, year);
            double cdr = cdr(t, year);

            wa_cbr.add(cbr, 1.0);
            wa_cdr.add(cdr, 1.0);

            double threshold = 0.95;
            String s_cbr = null;
            String s_cdr = null;

            if (cbr / cbr_2 < threshold)
                s_cbr = String.format("%2.1f", 100.0 * cbr / cbr_2 );

            if (cdr / cdr_2 < threshold)
                s_cdr = String.format("%2.1f", 100.0 * cdr / cdr_2 );
            
            if (s_cbr != null || s_cdr != null)
            {
                if (! printed)
                {
                    Util.out(tname);
                    printed = true;
                }
                
                if (s_cbr == null)
                    s_cbr = "";

                if (s_cdr == null)
                    s_cdr = "";
                
                while (s_cbr.length() < 4)
                    s_cbr = " " + s_cbr;
                
                while (s_cdr.length() < 4)
                    s_cdr = " " + s_cdr;
                
                Util.out(String.format("    %d %s %s", year, s_cbr, s_cdr));
            }
        }
    }

    private double cbr(Territory t, int year) throws Exception
    {
        TerritoryYear ty = t.territoryYearOrNull(year);
        if (ty == null)
            throw new NoDataException();

        Long pop = ty.progressive_population.total.both;
        Long births = ty.births.total.both;
        if (pop == null || births == null)
            throw new NoDataException();

        return (PROMILLE * births) / pop;
    }

    private double cdr(Territory t, int year) throws Exception
    {
        TerritoryYear ty = t.territoryYearOrNull(year);
        if (ty == null)
            throw new NoDataException();

        Long pop = ty.progressive_population.total.both;
        Long deaths = ty.deaths.total.both;
        if (pop == null || deaths == null)
            throw new NoDataException();

        return (PROMILLE * deaths) / pop;
    }

    public static class NoDataException extends Exception
    {
        private static final long serialVersionUID = 1L;
    }
}
