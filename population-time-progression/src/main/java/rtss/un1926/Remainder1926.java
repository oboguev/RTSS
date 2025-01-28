package rtss.un1926;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.MortalityTableADH;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.ForwardPopulationT;
import rtss.data.population.forward.ForwardPopulationUR;
import rtss.data.population.forward.PopulationContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/*
 * Остаток в живых недоучёта переписи 1926 года в начале 1927-1941 гг.
 * 
 * Перепись населения СССР 1926 года недоучла 1.5 млн. чел, в основном детей в возрасте 0-2 лет,
 * но также молодых женщин Средней Азии и Закавказья (Андреев, Дарский, Харькова,
 * "Население Советского Союза 1922-1991", стр. 16-23).
 * 
 * По территории РСФСР в границах 1991 года недоучёт оценивается в 777 тыс. чел.
 * (Андреев, Дарский, Харькова, "Демографическая история России 1927-1959", стр 20-28).
 * 
 * Программа вычисляет остаток этой недоучённой массы на начало 1928 ... 1941 гг.
 * с учётом смертности, т.е. число оставшихся из неё в живых на начало соответствующего года.
 * 
 * В силу заведомой приблизительности как начальной оценки, так и исчисления, разница между днём 
 * переписи (17.12.1926) и 1 января 1927 года игнорируется.
 */
public class Remainder1926
{
    public static void main(String[] args)
    {
        try
        {
            new Remainder1926().do_main(Area.USSR);
            new Remainder1926().do_main(Area.RSFSR);
        }
        catch (Throwable ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private CombinedMortalityTable mt1926;
    private CombinedMortalityTable mt1938;

    private void do_main(Area area) throws Exception
    {
        Util.out("");
        Util.out("Остаток в живых недоучёта переписи 1926 года (" + area.toString() + ", тыс. чел.):");
        Util.out("");

        initTables(area);

        PopulationByLocality p = initPopulation(area);
        PopulationByLocality prem = p;
        PopulationContext fctx = null;

        for (int year = 1927; year <= 1941; year++)
        {
            double sum = prem.sum(Locality.TOTAL, Gender.BOTH, 0, PopulationByLocality.MAX_AGE);
            Util.out(String.format("%d: %f", year, sum));

            if (year == 1941)
                break;

            /* передвижка */
            if (fctx == null)
            {
                fctx = new PopulationContext();
                p = fctx.begin(p);
            }

            CombinedMortalityTable mt = mortalityTableForYear(area, year);

            if (p.hasRuralUrban())
                p = new ForwardPopulationUR().forward(p, fctx, mt, 1.0);
            else
                p = new ForwardPopulationT().forward(p, fctx, mt, 1.0);

            prem = fctx.end(p);
        }
    }

    /*
     * Уровни младенческой смертности по АДХ:
     * Е.М. Андреев, Л.Е. Дарский, Т.Л. Харькова, "Население Советского Союза 1922-1991", стр. 57, 135
     */
    private final double ADH_infant_CDR_1926_1927 = 189.5;
    private final double ADH_infant_CDR_1938_1939 = 171.0;

    private void initTables(Area area) throws Exception
    {
        mt1926 = new CombinedMortalityTable("mortality_tables/USSR/1926-1927");
        mt1938 = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");

        mt1926 = PatchMortalityTable.patchInfantMortalityRate(mt1926, ADH_infant_CDR_1926_1927, "infant mortality patched to AHD");
        mt1938 = PatchMortalityTable.patchInfantMortalityRate(mt1938, ADH_infant_CDR_1938_1939, "infant mortality patched to AHD");
    }

    private CombinedMortalityTable mortalityTableForYear(Area area, int year) throws Exception
    {
        if (area == Area.USSR)
        {
            if (year <= 1927)
                return mt1926;

            if (year >= 1938)
                return mt1938;

            double weight = ((double) year - 1926) / (1938 - 1926);
            return CombinedMortalityTable.interpolate(mt1926, mt1938, weight);
        }
        else
        {
            if (year < 1927)
                year = 1927;
            if (year > 1940)
                year = 1940;
            return MortalityTableADH.getMortalityTable(area, year);
        }
    }

    private PopulationByLocality initPopulation(Area area) throws Exception
    {
        double m0, m1, m2, f0, f1, f2;

        /*
         * Недоучёт мальчиков и девочек (тыс.) доживших до переписи, по возрасту (0-2 года) (стр. 20) 
         */
        if (area == Area.USSR)
        {
            PopulationByLocality p = PopulationByLocality.newPopulationByLocality();
            p.forLocality(Locality.URBAN).zero();
            p.forLocality(Locality.RURAL).zero();

            m2 = 2333 - 2217;
            m1 = 2513 - 2285;
            m0 = 2922 - 2632;

            f2 = 2306 - 2198;
            f1 = 2488 - 2242;
            f0 = 2832 - 2551;

            /*
             * Доля городского населения в возрасте 0-2 лет, по итогам переписи 1926 года = 14.2%
             */
            final boolean DoSmoothPopulation = true;
            PopulationByLocality p1926 = PopulationByLocality.census(Area.USSR, 1926).smooth(DoSmoothPopulation);
            double xu = p1926.sum(Locality.URBAN, Gender.BOTH, 0, 2) / p1926.sum(Locality.TOTAL, Gender.BOTH, 0, 2);

            /*
             * Мы могли бы распределить недоучёт детей 0-2 лет пропорционально населению этого возраста
             * по переписи, но вероятнее, что недоучёт был многократно сильнее проявлен на селе, чем в городе.
             * Мы не располагаем оценкой разбивки недоучёта между городскими и сельскими территориями, 
             * и произвольно уменьшаем долю городского недоучёта в 4 раза сравнительно с долей городских детей.
             */
            xu = xu * 0.25;
            double xr = 1.0 - xu;

            p.set(Locality.URBAN, Gender.MALE, 0, m0 * xu);
            p.set(Locality.URBAN, Gender.MALE, 1, m1 * xu);
            p.set(Locality.URBAN, Gender.MALE, 2, m2 * xu);

            p.set(Locality.URBAN, Gender.FEMALE, 0, f0 * xu);
            p.set(Locality.URBAN, Gender.FEMALE, 1, f1 * xu);
            p.set(Locality.URBAN, Gender.FEMALE, 2, f2 * xu);

            p.set(Locality.RURAL, Gender.MALE, 0, m0 * xr);
            p.set(Locality.RURAL, Gender.MALE, 1, m1 * xr);
            p.set(Locality.RURAL, Gender.MALE, 2, m2 * xr);

            p.set(Locality.RURAL, Gender.FEMALE, 0, f0 * xr);
            p.set(Locality.RURAL, Gender.FEMALE, 1, f1 * xr);
            p.set(Locality.RURAL, Gender.FEMALE, 2, f2 * xr);

            p.recalcFromUrbanRuralBasic();

            /*
             * Мы не располагаем возрастной структурой непереписанных кавказских и среднеазиатских женщин,
             * находившихся в основном в возрастах 8-27 лет, ни оценкой специфической регионально-половой 
             * смертности этого населения, однако смертность в этом возрасте низка, поэтому мы не станем 
             * расчитывать поправку не неё, и условно отнесём здесь всё это население к возрасту 8 лет,
             * в котором смертность почти пренебрежима.    
             */

            double s = p.sum(Locality.TOTAL, Gender.BOTH, 0, PopulationByLocality.MAX_AGE);
            s = (148_530 - 147_028) - s;

            p.set(Locality.RURAL, Gender.FEMALE, 8, s);

            p.recalcFromUrbanRuralBasic();
            return p;
        }
        else // area == Area.RSFSR
        {
            PopulationByLocality p = PopulationByLocality.newPopulationTotalOnly();
            p.forLocality(Locality.TOTAL).zero();

            m2 = 1256 - 1196;
            m1 = 1375 - 1250;
            m0 = 1686 - 1519;

            f2 = 1252 - 1192;
            f1 = 1359 - 1235;
            f0 = 1642 - 1479;

            p.set(Locality.TOTAL, Gender.MALE, 0, m0);
            p.set(Locality.TOTAL, Gender.MALE, 1, m1);
            p.set(Locality.TOTAL, Gender.MALE, 2, m2);

            p.set(Locality.TOTAL, Gender.FEMALE, 0, f0);
            p.set(Locality.TOTAL, Gender.FEMALE, 1, f1);
            p.set(Locality.TOTAL, Gender.FEMALE, 2, f2);
            
            p.makeBoth(Locality.TOTAL);
            
            return p;
       }
    }
}