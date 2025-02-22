package rtss.data.curves.refine;

import rtss.data.bin.Bin;
import rtss.data.curves.CurveVerifier;
import rtss.data.curves.InterpolatePopulationAsMeanPreservingCurve.InterpolationOptions;
import rtss.data.curves.refine.RefineYearlyPopulationModel.ChildAttritionModel;
import rtss.data.selectors.Gender;
import rtss.util.Util;
import rtss.util.ValuesMatter;

import ch.qos.logback.classic.Level;

/*
 * Tune initial age points of the disaggregated populaton to conform young-age typical mortality pattern.
 * See more detatiled explanation in module RefineYearlyPopulationCore.
 */
public class RefineYearlyPopulation
{
    public static double[] refine(Bin[] bins, String title, double[] p, Integer yearHint, Gender gender, InterpolationOptions options)
            throws Exception
    {
        final double[] p0 = p;

        if (Util.False)
            return p0;

        /*
         * Модели детской смертности обычных лет неприложимы в 1932 и 1933 годах 
         * с их глубоким падением рождаемости и одновременно ростом детской смертности
         */
        if (yearHint == 1932 || yearHint == 1933)
            return p0;

        if (bins.length < 3 ||
            bins[0].widths_in_years != 5 ||
            bins[1].widths_in_years != 5 ||
            bins[3].widths_in_years != 5)
        {
            return p0;
        }

        /*
         * Attrition array describes how population normally decreases (from age 0) over age years
         * under typical mortality pattern of the era. 
         * It also accounts for the drop in births (in the U-shaped population case).
         */
        double[] attrition = null;

        /*
         * Number of age points to tune.
         */
        int nTunablePoints = 10;

        /*
         * The number of subsequent age points to use for curve smoothness evaluation; 
         * actually this is max(0, nFixedPoints - 1) points.  
         */
        int nFixedPoints = 2;

        if (bins[0].avg > bins[1].avg &&
            bins[1].avg > bins[2].avg)
        {
            /*
             * Случай:
             *      _
             *        _
             *          _
             *          
             * Gradually decreasing population for the initial three bins (i.e. normal case). 
             * Assume approximately steady births through recent 10 years.
             * 
             * Настраивать первые 10 точек (возраста 0-9), а следующие две использовать для контроля гладкости
             * совокупной получаемой кривой.           
             */
            nTunablePoints = 10; // ages 0-9
            nFixedPoints = 2; // ages 10-11
            ChildAttritionModel model = RefineYearlyPopulationModel.selectModel(yearHint, nTunablePoints, gender, ValuesMatter.RELATIVE);
            attrition = model.attrition09();

            if (p[9] <= p[10])
            {
                /*
                 * Перегиб наступает в bins[1], в p[9].
                 * Возраст 9 -- точка перегиба, а дальше от неё идёт возрастание.
                 * Симулировать для rc.refineSeriesIterative выход на равнину после точки 9 (начиная с 10), 
                 * дабы синтезировать более плавную кривую до этой точки.   
                 */
                p = Util.splice(p, 0, nTunablePoints + nFixedPoints - 1);
                p[11] = p[10] = p[9];
            }
            else if (p[11] > p[10])
            {
                /*
                 * Перегиб наступает в p[10].
                 * Симулировать для rc.refineSeriesIterative выход на равнину, 
                 * дабы синтезировать более плавную кривую до этой точки.   
                 */
                p = Util.splice(p, 0, nTunablePoints + nFixedPoints - 1);
                p[11] = p[10];
            }
        }
        else if (bins[0].avg > bins[1].avg &&
                 bins[1].avg < bins[2].avg &&
                 bins[0].avg > bins[2].avg * 1.05)
        {
            /*
             * Случай:
             *      _   
             *          _  
             *        _ 
             *        
             * U-shaped population. 
             * This means there was a drop in births during birth years in the second bin.
             * We should adjust attrition weights to account for it.           
             */
            int np = locateTurnpoint(bins, p);

            if (np == 5 && p[5] > p[4])
            {
                /*
                 * Нижняя точка -- в возрасте 4 года (или даже ранее, но это необычно).
                 * Возрасты 5 и 6 нарастают от неё. 
                 */
                nTunablePoints = 5; // ages 0-4
                nFixedPoints = 0;
            }
            else
            {
                /*
                 * Нижняя точка -- в возрасте 9 лет или старше.
                 */
                nTunablePoints = np;
                nFixedPoints = 1;
            }

            attrition = adjustedAttrition(bins, gender, yearHint, nTunablePoints);
        }
        else
        {
            return p0;
        }

        /*
         * if points at the right margin keep trending down, extend nFixedPoints
         */
        for (;;)
        {
            int n = nTunablePoints + nFixedPoints;
            if (n >= 20 || n >= p.length)
                break;
            if (p[n] < p[n - 1])
                nFixedPoints++;
            else
                break;
        }

        // double importance_smoothness = 0.94;
        double importance_smoothness = 0.70;
        if (options.secondaryRefineYearlyAgesSmoothness() != null)
            importance_smoothness = options.secondaryRefineYearlyAgesSmoothness();
        Util.assertion(importance_smoothness >= 0.001 && importance_smoothness <= 0.999);

        double importance_target_diff_matching = 1.0 - importance_smoothness;

        int extraPoints = 3; // for derivatives
        int plength = Math.max(10, nTunablePoints + nFixedPoints + extraPoints);
        p = Util.splice(p0, 0, plength - 1);

        try
        {
            RefineYearlyPopulationCore rc = new RefineYearlyPopulationCore(Util.dup(p),
                                                                           Util.normalize(Util.splice(attrition, 0, nTunablePoints - 1)),
                                                                           importance_smoothness,
                                                                           importance_target_diff_matching,
                                                                           nTunablePoints,
                                                                           nFixedPoints,
                                                                           title,
                                                                           null);

            Level outerLogLevel = Level.INFO;
            Level innerLogLevel = Level.INFO;

            if (options.debugSecondaryRefineYearlyAges())
                outerLogLevel = Level.DEBUG;

            double[] px = rc.refineSeriesIterative(outerLogLevel, innerLogLevel);

            Util.assertion(px.length == nTunablePoints);

            p = Util.dup(p0);
            Util.insert(p, px, 0);

            verifyMonotonicity(p, nTunablePoints);
            CurveVerifier.validate_means(p, bins);

            if (Util.True)
            {
                // Util.out("RefineYearlyPopulation processed " + title);
                return p;
            }
            else
            {
                return p0;
            }
        }
        catch (Exception ex)
        {
            Util.err("RefineYearlyPopulation failed for " + title + ", error: " + ex.getLocalizedMessage());
            // ex.printStackTrace();
            // throw ex;
            return p0;
        }
    }

    /*
     * Locate a turning point within bin[1].
     * Turning point is a local minimum, and next point after it goes up.
     * Or it is the last point in the bin. 
     */
    private static int locateTurnpoint(Bin[] bins, double[] p)
    {
        Bin bin = bins[1];
        for (int age = bin.age_x1;; age++)
        {
            if (age == bin.age_x2 || p[age + 1] > p[age])
                return age;
        }
    }

    private static void verifyMonotonicity(double[] p, int nTunablePoints) throws Exception
    {
        for (int k = 0; k < nTunablePoints; k++)
        {
            if (p[k] < p[k + 1])
                throw new Exception("curve child segment is not monotonic");
        }
    }

    /* ====================================================================================== */

    /*
     * Корректировать кривую ожидаемого падения населения с учётом не только смертности,
     * но и падения рождений в предшествующие годы
     */
    private static double[] adjustedAttrition(Bin[] bins, Gender gender, Integer yearHint, int nTurnAge) throws Exception
    {
        /*
         * build the curve of expected model population progress for ages 0...14 
         * under regular natural mortality pattern for the era, 
         * assuming steady year-to-year births
         *   
         * TODO: учесть неравномерность и падение детской смертности со второй половины 1940-х
         */
        ChildAttritionModel model = RefineYearlyPopulationModel.selectModel(yearHint, 15, gender, ValuesMatter.ABSOLUTE);
        double[] p = new double[15];
        p[0] = model.L0;
        for (int age = 1; age <= 14; age++)
            p[age] = p[age - 1] - model.attrition[age - 1];

        double b0 = Util.sum_range(p, 0, 4) / 5;
        double b1 = Util.sum_range(p, 5, 9) / 5;
        double b2 = Util.sum_range(p, 10, 14) / 5;
        double a = (b1 - b2) / (b0 - b2);
        Util.assertion(a >= 0);

        b0 = bins[0].avg;
        b1 = bins[1].avg;
        b2 = bins[2].avg;
        double v1 = a * b0 + (1 - a) * b2;

        /*
         * @v1 is expected population in bins[1] if there were no drop in birth rates 
         * 
         * (b0 - v1) = relative weight of regular natural attrition
         * (v1 - b1) = relative weight of births drop in previous years
         */
        Util.assertion(b0 > b2);
        Util.assertion(b0 > b1 && b1 <= b2);

        Util.assertion(v1 >= b1);
        Util.assertion(b0 > v1 && v1 > b2);

        /*
         * Кривая падения численности населения из-за естественной сметности
         */
        double[] naturalAttrition = model.attrition09();
        naturalAttrition = Util.normalize(Util.splice(naturalAttrition, 0, nTurnAge - 1));

        /*
         * Создать кривую распределения влияния падения рождений  
         */
        double[] birthDrop = new double[naturalAttrition.length];
        fillBirthDrop(birthDrop, yearHint);
        Util.assertion(nTurnAge >= 6 && nTurnAge <= 10);

        return Util.sumWeightedNormalized(b0 - v1, naturalAttrition, v1 - b1, birthDrop);
    }

    /*
     * Распределение интенсивности падений рождения
     */
    private static void fillBirthDrop(double[] birthDrop, int calendarYear) throws Exception
    {
        /*
         * TODO: использовать calendarYear для вычисления "эха" известных провалов числа рождений. 
         * 
         * Specific birth drops were caused by 1915-1922, 1932-1934, 1941-1945 
         * and to a lesser extent by 1946-1947.
         * 
         * Right now we use a very simplified model.
         */

        switch (birthDrop.length)
        {
        // fill the tail of birthDrop
        case 6:
            fillBirthDrop(birthDrop, 0.25, 0.75, 1);
            break;
        case 7:
            fillBirthDrop(birthDrop, 0.25, 0.75, 1);
            break;
        case 8:
            fillBirthDrop(birthDrop, 0.25, 0.75, 1);
            break;
        case 9:
            fillBirthDrop(birthDrop, 0.5, 1, 1);
            break;
        case 10:
            fillBirthDrop(birthDrop, 0.5, 1, 1);
            break;
        default:
            Util.assertion(false);
        }
    }

    /*
     * Заполнить хвост массива @birthDrop значениями @values
     */
    private static void fillBirthDrop(double[] birthDrop, double... values)
    {
        for (int k = 0; k < values.length; k++)
            birthDrop[birthDrop.length - 1 - k] = values[values.length - 1 - k];
    }
}
