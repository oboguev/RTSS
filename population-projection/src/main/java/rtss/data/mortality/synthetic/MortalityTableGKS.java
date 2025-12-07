package rtss.data.mortality.synthetic;

import java.util.HashMap;
import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/*
 * Загрузить поло-возрастные покаатели смертности (агреггированные по возрастным группам) из файла Excel
 * и построить на их основании таблицу смертности с годовым шагом.
 */
public class MortalityTableGKS
{
    public static final int MAX_AGE = CombinedMortalityTable.MAX_AGE;
    private static Map<String, CombinedMortalityTable> cache = new HashMap<>();

    static public boolean UsePrecomputedFiles = true;
    static public boolean UseCache = true;
    
    private static class Context
    {
        Area area;
        String year;
        String filepath;
    }

    public static synchronized CombinedMortalityTable getMortalityTable(Area area, String year) throws Exception
    {
        String path = String.format("mortality_tables/%s/%s", area.name(), year);

        CombinedMortalityTable cmt = null;

        // look in cache
        if (UseCache)
        {
            cmt = cache.get(path);
            if (cmt != null)
                return cmt;
        }

        // try loading from resource
        if (UsePrecomputedFiles)
        {
            try
            {
                if (Util.True)
                {
                    cmt = CombinedMortalityTable.load(path);
                }
            }
            catch (Exception ex)
            {
                // ignore
                Util.noop();
            }
        }

        if (cmt == null)
        {
            Context context = new Context();
            context.area = area;
            context.year = year;
            context.filepath = String.format("mortality_tables/%s/%s-MortalityRates-GKS-%s.xlsx", area.name(), area.name(), year);

            cmt = CombinedMortalityTable.newEmptyTable();
            compute(cmt, context, Locality.TOTAL);
            compute(cmt, context, Locality.URBAN);
            compute(cmt, context, Locality.RURAL);
            cmt.comment(String.format("ГКС-%s-%s", context.area.name(), context.year));

            if (Util.False)
            {
                String comment = "# Таблица построена модулем " + MortalityTableGKS.class.getCanonicalName() + " по данным в " + context.filepath;
                cmt.saveTable("P:\\@\\zzzz", comment);
            }
        }

        if (UseCache)
        {
            cmt.seal();
            cache.put(path, cmt);
        }

        return cmt;
    }
    
    private static void compute(CombinedMortalityTable cmt, Context context, Locality locality) throws Exception
    {
        compute(cmt, context, locality, Gender.BOTH);
        compute(cmt, context, locality, Gender.MALE);
        compute(cmt, context, locality, Gender.FEMALE);
    }
    
    private static void compute(CombinedMortalityTable cmt, Context context, Locality locality, Gender gender) throws Exception
    {
        SingleMortalityTable mt = getSingleTable(context, locality, gender);
        cmt.setTable(locality, gender, mt);
    }

    private static SingleMortalityTable getSingleTable(Context context, Locality locality, Gender gender) throws Exception
    {
        Bin[] mortality_bins = MortalityRatesFromExcel.loadAgeQx(context.filepath, locality, gender);
        mortality_bins = Bins.multiply(mortality_bins, 1000.0);
        String title = String.format("ГКС-%s-%s %s %s", context.area.name(), context.year, locality.name(), gender.name());
        SingleMortalityTable mt = BuildSingleTable.makeSingleTable(mortality_bins, title);
        return mt;
    }
}
