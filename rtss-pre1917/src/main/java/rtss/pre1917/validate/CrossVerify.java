package rtss.pre1917.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.data.URValue;
import rtss.pre1917.data.ValueByGender;
import rtss.util.Util;

public class CrossVerify
{
    public void verify(TerritoryDataSet territories) throws Exception
    {
        validateHasYearData(territories);
        validateMaleFemaleBoth(territories);
        validateRuralUrbanAll(territories);
        validateGradualPopulationIncrease(territories);
        // ### проверить составные таксоны: расхождение (муж + жен - оба пола) равно отсутствующим данным в составных элементах
        // ### проверить URValue.all против суммы URValue.rural.both + URValue.urban.both   
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

    /* =============================================================================================================== */

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

        Util.out(String.format("CBR differ: %f%%", (100.0 * cbr_differ) / cbr_seen));
        Util.out(String.format("CDR differ: %f%%", (100.0 * cdr_differ) / cdr_seen));
    }

    /* =============================================================================================================== */

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

    /* =============================================================================================================== */

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

    /* =============================================================================================================== */

    /*
     * Проверить что значения муж. и жен., когда указаны, в сумме согласуются для обеих полов.
     * Проверка только для базовых областей, не составных таксонов.
     */
    private void validateMaleFemaleBoth(TerritoryDataSet territories)
    {
        for (Territory ter : territories.values())
        {
            if (Taxon.isComposite(ter.name))
                continue;

            for (int year : ter.years())
            {
                TerritoryYear ty = ter.territoryYear(year);
                validateMaleFemaleBoth(ter, year, ty.population.urban, "население городов");
                validateMaleFemaleBoth(ter, year, ty.population.rural, "население уездов");
                validateMaleFemaleBoth(ter, year, ty.births.urban, "рождения в городах");
                validateMaleFemaleBoth(ter, year, ty.births.rural, "рождения в уездах");
                validateMaleFemaleBoth(ter, year, ty.deaths.urban, "смерти в городах");
                validateMaleFemaleBoth(ter, year, ty.deaths.rural, "смерти в уездах");
            }
        }
    }

    private void validateMaleFemaleBoth(Territory ter, int year, ValueByGender value, String what)
    {
        boolean squash = Util.True;

        if (squash)
        {
            if (year == 1902 && ter.name.equals("Нижегородская") && what.equals("рождения в уездах"))
                return;
            if (year == 1902 && ter.name.equals("Ломжинская") && what.equals("рождения в уездах"))
                return;
        }

        if (value.male != null && value.female != null && value.both != null)
        {
            long df = Math.abs(value.male + value.female - value.both);
            if (df != 0)
            {
                String msg = String.format("Расхождение (муж + жен - оба пола) %d %s %s на %,d (%,d + %,d - %,d)",
                                           year, ter.name, what, df, value.male, value.female, value.both);
                Util.err(msg);
            }
        }
    }

    /* =============================================================================================================== */

    /*
     * Проверить что сумма значений городских и уездных численостей, когда указаны, согласуются с общей величиной для территории.
     * Проверка только для базовых областей, не составных таксонов.
     */
    private void validateRuralUrbanAll(TerritoryDataSet territories)
    {
        for (Territory ter : territories.values())
        {
            if (Taxon.isComposite(ter.name))
                continue;

            for (int year : ter.years())
            {
                TerritoryYear ty = ter.territoryYear(year);
                validateRuralUrbanAll(ter, year, ty.population, "население");
                validateRuralUrbanAll(ter, year, ty.births, "рождения");
                validateRuralUrbanAll(ter, year, ty.deaths, "смерти");
            }
        }
    }

    private void validateRuralUrbanAll(Territory ter, int year, URValue value, String what)
    {
        long vsum = 0;
        long vall = 0;

        if (value.urban.both == null && value.rural.both == null)
            return;

        if (value.urban.both != null)
            vsum += value.urban.both;
        if (value.rural.both != null)
            vsum += value.rural.both;

        if (value.all != null)
            vall += value.all;

        if (vsum != vall)
        {
            String msg = String.format("Расхождение (города + уезды - сумма) для %d %s %s на %,d (%s + %s - %s)",
                                       year, ter.name, what, Math.abs(vall - vsum),
                                       l2s(value.urban.both), l2s(value.rural.both), l2s(value.all));
            Util.err(msg);
        }
    }

    private String l2s(Long v)
    {
        if (v == null)
            return "[no data]";
        else
            return String.format("%,d", v);
    }

    /* =============================================================================================================== */

    private void validateHasYearData(TerritoryDataSet territories) throws Exception
    {
        for (int year = 1892; year <= 1914; year++)
        {
            if (!hasYearData(territories, year))
                Util.err(String.format("Отсутствуют данные за %d год", year));
        }
    }

    private boolean hasYearData(TerritoryDataSet territories, int year) throws Exception
    {
        return hasYearData(territories, "Архангельская", year) &&
               hasYearData(territories, "Ярославская", year) &&
               hasYearData(territories, "Империя", year);
    }

    private boolean hasYearData(TerritoryDataSet territories, String tname, int year) throws Exception
    {
        Territory ter = territories.get(tname);
        if (ter == null || !ter.hasYear(year))
            return false;

        TerritoryYear ty = ter.territoryYear(year);

        if (ty.population.all == null || ty.births.all == null || ty.deaths == null)
            return false;

        return true;
    }

    /* =============================================================================================================== */

    /*
     * Проверить, что население год от года нарастает постепенно
     */
    private void validateGradualPopulationIncrease(TerritoryDataSet territories)
    {
        for (String tname : territories.keySet())
        {
            Territory ter = territories.get(tname);
            validateGradualPopulationIncrease(ter);
        }
    }

    private void validateGradualPopulationIncrease(Territory ter)
    {
        List<TerritoryYear> tylist = new ArrayList<>();

        for (int year : ter.years())
        {
            TerritoryYear ty = ter.territoryYear(year);
            if (ty.population.all != null)
                tylist.add(ty);
        }

        while (tylist.size() >= 3)
        {
            TerritoryYear ty1 = tylist.get(0);
            TerritoryYear ty2 = tylist.get(1);
            TerritoryYear ty3 = tylist.get(2);
            
            String sdv = "ill-defined";
            long v1 = ty1.population.all;
            long v2 = ty2.population.all;
            long v3 = ty3.population.all;
            if (v3 != v1)
            {
                double fdv = (double) (v2 - v1) / (v3 - v1);
                if (fdv >= 0 && fdv <= 1.0)
                    sdv = String.format("%.1f", fdv * 100.0);
            }
            

            if (!validateGradualPopulationIncrease(ty1, ty2, ty3))
            {
                String msg = String.format("Годовое изменение населения %s [%d - %d - %d] %,d - %,d - %,d [%s]",
                                           ter.name, ty1.year, ty2.year, ty3.year,
                                           ty1.population.all, ty2.population.all, ty3.population.all,
                                           sdv);
                Util.err(msg);
            }

            tylist.remove(0);
        }
    }

    private boolean validateGradualPopulationIncrease(TerritoryYear ty1, TerritoryYear ty2, TerritoryYear ty3)
    {
        long v1 = ty1.population.all;
        long v2 = ty2.population.all;
        long v3 = ty3.population.all;
        
        if (v1 == v2 && v2 == v3)
            return true;

        if (!(v1 < v3))
            return false;

        if (v1 == v2 || v2 == v3)
            return true;

        double dv = (v3 - v1);
        if (v2 >= v1 + 0.05 * dv && v2 <= v1 + 0.95 * dv)
            return true;

        return false;
    }
}
