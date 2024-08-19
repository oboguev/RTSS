package rtss.pre1917.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.util.Util;

public class CrossVerify
{
    public void verify(TerritoryDataSet territories)
    {
        new ValidateTaxons().validate_taxons(territories);
        validate_vital_rates(territories);

        // calc_1893(territories);
        // check_population_jump(territories);

        // ### check taxonomy sums
        // ### population jump year-to-next over 2%
        // ### population jump  mismatching births - deaths
        // ### implied (calculated) CBR or CDR mismatching listed
        // ### back-calculate population
    }

    private void validate_vital_rates(TerritoryDataSet territories)
    {
        String msg;

        int cbr_seen = 0;
        int cdr_seen = 0;
        int cbr_differ = 0;
        int cdr_differ = 0;

        boolean squash = Util.True;

        for (Territory ter : territories.values())
        {
            for (int year = 1891; year <= 1915; year++)
            {
                TerritoryYear ty = ter.territoryYear(year);
                TerritoryYear ty2 = ter.territoryYear(year + 1);

                if (ty.population.all != null && ty.births.all != null && ty.cbr != null)
                {
                    /*
                     * pop1: 1029 mismatches
                     * popm: 933 mismatches
                     * pop2: 693 mismatches
                     * ty2.population.all: 153 mismatches
                     */
                    long pop1 = ty.population.all;
                    long pop2 = ty.population.all + ty.births.all - ty.deaths.all;
                    @SuppressWarnings("unused")
                    long popm = (pop1 + pop2) / 2;
                    
                    double cbr = (1000.0 * ty.births.all) / pop2;

                    if (Util.False)
                    {
                        if (ty2.population.all == null)
                            continue;
                        cbr = (1000.0 * ty.births.all) / ty2.population.all;
                    }

                    cbr_seen++;

                    if (Math.abs(cbr - ty.cbr) > 0.2)
                    {
                        cbr_differ++;
                        msg = String.format("CBR differs: %s %d listed=%.1f calculated=%.1f, diff=%.1f",
                                            ter.name, year, ty.cbr, cbr, cbr - ty.cbr);
                        if (!squash)
                            Util.out(msg);
                    }
                }

                if (ty.population.all != null && ty.deaths.all != null && ty.cdr != null)
                {
                    /*
                     * pop1: 1029 mismatches
                     * popm: 933 mismatches
                     * pop2: 693 mismatches
                     * ty2.population.all: 153 mismatches
                     */
                    long pop1 = ty.population.all;
                    long pop2 = ty.population.all + ty.births.all - ty.deaths.all;
                    @SuppressWarnings("unused")
                    long popm = (pop1 + pop2) / 2;
                    
                    double cdr = (1000.0 * ty.deaths.all) / pop2;

                    if (Util.False)
                    {
                        if (ty2.population.all == null)
                            continue;
                        cdr = (1000.0 * ty.deaths.all) / ty2.population.all;
                    }

                    cdr_seen++;

                    if (Math.abs(cdr - ty.cdr) > 0.2)
                    {
                        cdr_differ++;
                        msg = String.format("CDR differs: %s %d listed=%.1f calculated=%.1f, diff=%.1f",
                                            ter.name, year, ty.cdr, cdr, cdr - ty.cdr);
                        if (!squash)
                            Util.out(msg);
                    }
                }
            }
        }
        
        Util.out(String.format("CBR differ: %f%%", (100.0 * cbr_differ) /cbr_seen));
        Util.out(String.format("CDR differ: %f%%", (100.0 * cdr_differ) /cdr_seen));
    }

    /**
     * Вычислить население на начало 1893 года по косвенным данным
     */
    private void calc_1893(TerritoryDataSet territories)
    {
        for (Territory ter : territories.values())
        {
            TerritoryYear t93 = ter.territoryYear(1893);
            TerritoryYear t94 = ter.territoryYear(1894);

            if (t93.population.all == null && t93.births.all != null && t93.deaths.all != null && t94.population.all != null)
            {
                t93.population.all = t94.population.all - (t93.births.all - t93.deaths.all);
            }
            else if (t93.population.all == null && t93.cdr != null && t93.cbr != null && t94.population.all != null)
            {
                double v = 1 + (t93.cbr - t93.cdr) / 1000.0;
                t93.population.all = Math.round(t94.population.all / v);
            }
            else
            {
                Util.noop();
            }
        }
    }

    private void check_population_jump(TerritoryDataSet territories)
    {
        List<String> msgs = new ArrayList<>();

        for (Territory ter : territories.values())
        {
            int previous_year = -1;
            long previous_population = 0;
            for (int year = 1893; year <= 1915; year++)
            {
                TerritoryYear ty = ter.territoryYear(year);

                if (ty.population.all != null)
                {
                    if (previous_year != -1)
                    {
                        double v = (double) ty.population.all / previous_population;
                        double vv = Math.pow(v, 1.0 / (year - previous_year));
                        if (vv < 0.9 || vv > 1.15)
                        {
                            String s = String.format("Population jump %d-%d %s by %.3f, yearly %.3f",
                                                     previous_year, year, ter.name, v, vv);
                            msgs.add(s);
                        }
                    }

                    previous_year = year;
                    previous_population = ty.population.all;
                }
            }
        }

        Collections.sort(msgs);
        for (String s : msgs)
            Util.err(s);
    }
}
