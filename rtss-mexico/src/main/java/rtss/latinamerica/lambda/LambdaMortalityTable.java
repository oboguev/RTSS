package rtss.latinamerica.lambda;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rtss.csv.CSVSmartReader;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class LambdaMortalityTable
{
    private static CSVSmartReader cachedCSV;

    private static CSVSmartReader loadCSV() throws Exception
    {
        if (cachedCSV == null)
            cachedCSV = CSVSmartReader.fromResource("latinamerica/LAMBdA/life-tables/annual/LAMBdA_annual_single_age_LT_upto_2017.csv");
        return cachedCSV;
    }

    public static List<Integer> countryTableYears(String cname) throws Exception
    {
        String ename = CountryName.ename(cname);
        List<Integer> years = new ArrayList<>();

        CSVSmartReader csv = loadCSV();

        int colCtry = csv.column("ctry");
        int colYear = csv.column("year");
        if (colCtry < 0 || colYear < 0)
            throw new Exception("Unexpected file structure");

        for (int nr = 0; nr < csv.rowCount(); nr++)
        {
            String ctry = csv.asString(nr, colCtry);
            if (ctry != null && ctry.equals(ename))
            {
                int year = csv.asInt(nr, colYear);
                if (!years.contains(year))
                    years.add(year);
            }
        }

        Collections.sort(years);
        return years;
    }

    public static CombinedMortalityTable countryMortalityTable(String cname, int year) throws Exception
    {
        String ename = CountryName.ename(cname);
        CSVSmartReader csv = loadCSV();

        double[] qxm = load_qx(csv, ename, year, "m");
        double[] qxf = load_qx(csv, ename, year, "f");
        
        SingleMortalityTable sm = SingleMortalityTable.from_qx(cname + " " + year + " male", qxm);
        SingleMortalityTable sf = SingleMortalityTable.from_qx(cname + " " + year + " female", qxf);
        
        CombinedMortalityTable cmt = CombinedMortalityTable.newEmptyTable();
        cmt.setTable(Locality.TOTAL, Gender.MALE, sm);
        cmt.setTable(Locality.TOTAL, Gender.FEMALE, sf);
        cmt.comment(cname + " " + year);
        cmt.setSource(cname + " " + year);
        cmt.seal();

        return cmt;
    }

    private static double[] load_qx(CSVSmartReader csv, String ename, int year, String gender) throws Exception
    {
        int colCtry = csv.column("ctry");
        int colYear = csv.column("year");
        int colAge = csv.column("age");
        int colQx = csv.column("Qx_" + gender);
        if (colCtry < 0 || colYear < 0 || colAge < 0 || colQx < 0)
            throw new Exception("Unexpected file structure");

        int lastage = -1;

        double[] qx = new double[Population.MAX_AGE + 1];

        for (int nr = 0; nr < csv.rowCount(); nr++)
        {
            String ctry = csv.asString(nr, colCtry);
            if (ctry == null || !ctry.equals(ename))
                continue;
            int cyear = csv.asInt(nr, colYear);
            if (cyear != year)
                continue;

            int age = csv.asInt(nr, colAge);
            double q = csv.asDouble(nr, colQx);

            if (age != 1 + lastage || age > Population.MAX_AGE)
                throw new Exception("Unexpected file structure");

            qx[age] = q;
            lastage = age;
        }

        if (lastage == -1)
            throw new Exception("No data for " + ename + " " + year);

        for (int age = lastage + 1; age <= Population.MAX_AGE; age++)
            qx[age] = 1;

        return qx;
    }

    public static void main(String[] args)
    {
        /*
         * Test
         */
        try
        {
            List<Integer> years = countryTableYears("Гондурас");
            CombinedMortalityTable cmt = countryMortalityTable("Гондурас", 1940);
            Util.unused(years, cmt);
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }
}
