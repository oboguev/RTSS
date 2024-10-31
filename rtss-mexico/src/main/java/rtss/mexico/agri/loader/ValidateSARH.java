package rtss.mexico.agri.loader;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rtss.mexico.agri.entities.Culture;
import rtss.mexico.agri.entities.CultureYear;
import rtss.mexico.agri.entities.Cultures;
import rtss.util.Util;

public class ValidateSARH
{
    public static void main(String[] args)
    {
        try
        {
            new ValidateSARH().validate();
            Util.out("** Done");
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private Cultures cultures = LoadSARH.load();
    private Map<Integer, Long> population = LoadSARH.loadPopulation();

    private ValidateSARH() throws Exception
    {
    }

    private void validate() throws Exception
    {
        for (String cname : cultures.names())
        {
            Culture c = cultures.get(cname);

            for (int year : c.years())
                validate(c.cultureYear(year));

            for (CultureYear cy : c.averageCultureYears.values())
                validate(cy);

            for (CultureYear cy : c.averageCultureYears.values())
                validateAverage(cy);
        }
    }

    private void validate(CultureYear cy) throws Exception
    {
        String tag = tag(cy);

        if (cy.production != null && cy.consumption != null && Util.True)
        {
            double v = cy.production + denull(cy.importAmount) - denull(cy.exportAmount);
            double df = df(v, cy.consumption);
            if (df > 0.03)
            {
                warn(String.format("%s расхождение баланса производства-потребления на %.1f%%, напечатано: %s, вычисляется: %s",
                                   tag, df * 100.0, Util.f2s(cy.consumption), Util.f2s(v)));
            }
        }

        if (cy.consumption != null && cy.perCapita != null && cy.year > 0 && Util.True)
        {
            double v = cy.perCapita * population.get(cy.year) / 1000.0;

            double pc = 1000.0 * cy.consumption / population.get(cy.year);
            // round pc to 3 digits
            pc = Math.round(pc * 1000) / 1000.0;

            double df = df(pc, cy.perCapita);
            // ignore divergence in the last digit
            if (Math.abs(pc - cy.perCapita) < 0.0015)
                df = 0;

            if (df > 0.07)
            {
                warn(String
                        .format("%s расхождение национального и душевого потребления на %.1f%%, напечатано/вычисляется: (%s, %.0f) (%.3f, %.3f)",
                                tag, df * 100.0,
                                Util.f2s(cy.consumption), v,
                                cy.perCapita, pc));
            }
        }
    }

    private void validateAverage(CultureYear cy) throws Exception
    {
        if (!cy.comment.startsWith("Promedio "))
            throw new Exception("Does not start with Promedio:" + cy.comment);
        String spro = Util.stripStart(cy.comment, "Promedio ");
        String[] sa = spro.split("/");
        if (sa.length != 2)
            throw new Exception("Incorrect promedio line");
        int y1 = Integer.parseInt(sa[0]);
        int y2 = Integer.parseInt(sa[1]) + 1900;

        if (y1 >= 1925 && y1 <= 1980 && y2 >= 1925 && y2 <= 1980)
        {
            // ok
        }
        else
        {
            throw new Exception("Incorrect years in Promedio line");
        }

        validateAverage(cy, y1, y2, "урожай");
        validateAverage(cy, y1, y2, "экспорт");
        validateAverage(cy, y1, y2, "импорт");
        validateAverage(cy, y1, y2, "национальное потребление");
        validateAverage(cy, y1, y2, "душевое потребление");
        validateAverage(cy, y1, y2, "алкоголь");
    }

    private String tag(CultureYear cy) throws Exception
    {
        if (cy.comment != null && cy.comment.startsWith("Promedio "))
        {
            String spro = Util.stripStart(cy.comment, "Promedio ");
            String[] sa = spro.split("/");
            if (sa.length != 2)
                throw new Exception("Incorrect promedio line");
            int y1 = Integer.parseInt(sa[0]);
            int y2 = Integer.parseInt(sa[1]) + 1900;
            return String.format("%s %d-%d", cy.culture.id(), y1, y2);
        }
        else
        {
            return String.format("%s %d", cy.culture.id(), cy.year);
        }
    }

    private void validateAverage(CultureYear cy, int y1, int y2, String what) throws Exception
    {
        String tag = tag(cy);

        double v = 0;
        int nyears = 0;

        for (int year = y1; year <= y2; year++)
        {
            CultureYear xcy = cy.culture.cultureYear(year);
            if (xcy == null)
            {
                Util.err(String.format("Нет года для среднегодовых данных: %s %d", cy.culture.id(), year));
            }
            else
            {
                Double xv = xcy.get(what);
                if (xv != null)
                    v += xv;
            }
            nyears++;
        }

        v /= nyears;
        Double cv = cy.get(what);
        boolean cv_null = false;

        if (cv == null)
        {
            cv = 0.0;
            cv_null = true;
        }

        String s_promedio;
        String s_calc;

        if (what.equals("душевое потребление"))
        {
            s_promedio = String.format("%.3f", cv);
            s_calc = String.format("%.3f", v);
        }
        else
        {
            s_promedio = String.format("%.0f", cv);
            s_calc = String.format("%.0f", v);
        }

        double df = df(v, cv);

        if (s_promedio.equals(s_calc))
            df = 0;

        if (!what.equals("душевое потребление") && Math.abs(v - cv) <= 2)
            df = 0;

        if (df > 0.05 && Util.True && !cv_null)
        {
            warn(String.format("%s расхождение среднего для [%s] на %.1f%% напечатано/вычислено: %s %s",
                               tag, what,
                               df * 100.0,
                               s_promedio, s_calc));
        }
    }

    private double denull(Double d)
    {
        return d == null ? 0 : d;
    }

    private double df(double v1, double v2)
    {
        double x1 = Math.abs(v2 - v1);
        double x2 = (Math.abs(v2) + Math.abs(v1)) / 2;
        return x1 / x2;
    }

    private static Set<String> knownWarnings = null;

    private void warn(String msg) throws Exception
    {
        if (knownWarnings == null)
        {
            knownWarnings = new HashSet<>();
            String fs = Util.loadResource("agriculture/SARH-Consumos-aparentes/known-divergences.txt");
            for (String s : fs.split("\n"))
            {
                if (s.equals("") || s.startsWith("#"))
                    continue;
                knownWarnings.add(Util.despace(s).trim());
            }
        }

        if (!knownWarnings.contains(msg))
            Util.out(msg);
    }

}
