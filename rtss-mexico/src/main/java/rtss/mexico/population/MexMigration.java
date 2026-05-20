package rtss.mexico.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rtss.csv.CSVSmartReader;
import rtss.util.Util;

public class MexMigration
{
    public static void main(String[] args)
    {
        try
        {
            org.apache.poi.util.IOUtils.setByteArrayMaxOverride(300_000_000);

            if (Util.True)
            {
                Util.out("");
                Util.out("=======================================================");
                Util.out("Эмиграция из Мексики и иммиграция в Мексику (CONAPO 2025):");
                Util.out("");
                Util.out("год эмигрантов иммигрантов");
                new MexMigration()
                        .do_main_1("conapo-2025-05/International Migrants by Age and Five-Year Period 1950-2069/02_mig_inter_quinquen_proyecciones.csv");

                Util.out("");
                Util.out("=======================================================");
                Util.out("Эмиграция из Мексики и иммиграция в Мексику (CONAPO 2025):");
                Util.out("");
                Util.out("год эмигрантов иммигрантов");
                new MexMigration()
                        .do_main_2("conapo-2025-05/Demographic indicators 1950-2070/05_indicadores_demograficos_proyecciones.csv");
            }
        }
        catch (Throwable ex)
        {
            Util.err("*** Exception");
            ex.printStackTrace();
        }
    }

    private static class YearData
    {
        public double emigrants;
        public double immigrants;
    }

    private void do_main_1(String path) throws Exception
    {
        CSVSmartReader csv = CSVSmartReader.fromResource(path);
        Map<String, YearData> myear = new HashMap<>();
        Map<String, YearData> mage = new HashMap<>();

        YearData total = new YearData();

        for (int nr = 0; nr < csv.rowCount(); nr++)
        {
            String year = csv.asString(nr, "ANIO");
            String age = csv.asString(nr, "EDAD");
            double em = csv.asDouble(nr, "EMIGRANTES");
            double im = csv.asDouble(nr, "INMIGRANTES");

            int y1 = n1(year);
            if (y1 >= 2025)
                continue;

            YearData yd = myear.computeIfAbsent(year, y -> new YearData());
            yd.emigrants += em;
            yd.immigrants += im;

            yd = mage.computeIfAbsent(age, y -> new YearData());
            yd.emigrants += em;
            yd.immigrants += im;

            total.emigrants += em;
            total.immigrants += im;
        }

        /* ----------------------------------------------------------------- */

        List<String> years = new ArrayList<>(myear.keySet());
        Collections.sort(years);

        for (String year : years)
        {
            YearData yd = myear.get(year);
            Util.out(String.format("%s %s %s", year, f2s(yd.emigrants), f2s(yd.immigrants)));
        }

        /* ----------------------------------------------------------------- */

        Util.out("");
        Util.out("Разбивка по возрасту");
        Util.out("");
        Util.out("возраст эмигрантов иммигрантов");

        List<String> ages = new ArrayList<>(mage.keySet());
        Collections.sort(ages);

        for (String age : ages)
        {
            YearData yd = mage.get(age);
            Util.out(String.format("%s %s %s", age, f2s(yd.emigrants), f2s(yd.immigrants)));
        }

        /* ----------------------------------------------------------------- */

        Util.out("");
        Util.out("всего эмигрантов иммигрантов");
        Util.out(String.format("%s %s %s", "*", f2s(total.emigrants), f2s(total.immigrants)));
    }

    private void do_main_2(String path) throws Exception
    {
        CSVSmartReader cvs = CSVSmartReader.fromResource(path);

        Util.out("");
        Util.out("всего эмигрантов иммигрантов");

        int total_emigrants = 0;
        int total_immigrants = 0;

        for (int nr = 0; nr < cvs.rowCount(); nr++)
        {
            int geo = cvs.asInt(nr, "CVE_GEO");
            if (geo != 0)
                continue;

            int year = cvs.asInt(nr, "ANIO");
            int emigrants = cvs.asInt(nr, "EMI_INT");
            int immigrants = cvs.asInt(nr, "INM_INT");

            total_emigrants += emigrants;
            total_immigrants += immigrants;

            Util.out(String.format("%s %s %s", year, f2s(emigrants), f2s(immigrants)));
        }

        Util.out("");
        Util.out("всего эмигрантов иммигрантов");
        Util.out(String.format("%s %s %s", "*", f2s(total_emigrants), f2s(total_immigrants)));
    }

    private String f2s(double v) throws Exception
    {
        String sv = String.format("%,f", v);
        sv = stripTrailingDecimalZeros(sv);
        return sv;
    }

    private static String stripTrailingDecimalZeros(String sv)
    {
        return sv.replaceFirst("\\.?0+$", "");
    }

    static int n1(String sv)
    {
        String s = sv.trim();

        if (!s.matches("\\d+-\\d+"))
            throw new IllegalArgumentException("Expected two integers separated by dash: " + sv);

        int dash = s.indexOf('-');
        return Integer.parseInt(s.substring(0, dash));
    }
}
