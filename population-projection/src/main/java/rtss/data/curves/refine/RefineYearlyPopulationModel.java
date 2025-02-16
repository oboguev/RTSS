package rtss.data.curves.refine;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.ValuesMatter;

public class RefineYearlyPopulationModel
{
    /*
     * Характерное модельное соотношение убыли среднегодового населения для возрастов 0-4,
     * при переходе от возраста (x) к возрасту (x + 1), где x = [0...4].
     * Взято по таблицам смертности Госкомстата для СССР 1926-1927, 1939-1939 и 1958-1959 гг.
     * 
     * Распределение не кардинально различается календарными годами (оба пола, удельная убыль с 0 по 14 лет в возрастах 0-4 и 5-9 и 10-14):
     * 
     *     1926  =  0.451  0.190  0.093  0.061  0.046    0.034  0.027  0.021  0.016  0.012    0.010  0.009  0.009  0.010  0.011
     *     1938  =  0.538  0.177  0.079  0.046  0.027    0.026  0.021  0.016  0.013  0.011    0.010  0.009  0.008  0.009  0.009
     *     1958  =  0.493  0.132  0.065  0.044  0.030    0.031  0.029  0.027  0.024  0.022    0.020  0.020  0.020  0.021  0.022
     *     
     * То же, удельная убыль с 0 по 9 лет в возрастах 0-4 и 5-9:
     *     
     *     1926  =  0.474  0.200  0.098  0.064  0.048    0.036  0.028  0.022  0.017  0.013
     *     1938 =   0.564  0.185  0.083  0.048  0.028    0.028  0.022  0.017  0.014  0.011
     *     1958 =   0.549  0.147  0.073  0.049  0.034    0.035  0.032  0.030  0.027  0.024
     * 
     * Отдельное распределение для возрастов 0-4 и 5-9:
     * 
     *     1926 = 53.612, 22.581, 11.097, 7.223, 5.487    31.194, 23.981, 18.735, 14.801, 11.288
     *     1938 = 62.054, 20.411,  9.147, 5.275, 3.113    30.145, 23.602, 18.736, 15.045, 12.472 
     *     1958 = 64.474, 17.232,  8.530, 5.790, 3.974    23.622, 21.850, 20.276, 17.913, 16.339
     */
    public static class ChildAttritionModel
    {
        public final double L0;
        public final double[] attrition;

        public ChildAttritionModel(double L0, double[] attrition)
        {
            // average yearly population in age year 0 (assuming initial population at year start 100_000),
            // comes from mortality table Lx(0)
            this.L0 = L0;

            // yearly drops in average yearly population from year to year
            // comes from mortality table Lx(ageyear + 1) - Lx(ageyear)
            this.attrition = Util.dup(attrition);
        }

        public ChildAttritionModel clone()
        {
            return new ChildAttritionModel(L0, attrition);
        }

        public double[] attrition04()
        {
            return Util.splice(attrition, 0, 4);
        }

        public double[] attrition59()
        {
            return Util.splice(attrition, 5, 9);
        }

        public double[] attrition09()
        {
            return Util.splice(attrition, 0, 9);
        }

        public double[] attrition014()
        {
            return Util.splice(attrition, 0, 14);
        }

        static ChildAttritionModel forMortalityTable(String tablePath, Gender gender) throws Exception
        {
            CombinedMortalityTable mt = new CombinedMortalityTable(tablePath);
            SingleMortalityTable smt = mt.getSingleTable(Locality.TOTAL, gender);

            double L0 = smt.get(0).Lx;
            double[] dLx = new double[15];
            for (int age = 0; age < dLx.length; age++)
                dLx[age] = smt.get(age).Lx - smt.get(age + 1).Lx;

            return new ChildAttritionModel(L0, dLx);
        }

        static ChildAttritionModel interpolate(int year, int y1, ChildAttritionModel m1, int y2, ChildAttritionModel m2) throws Exception
        {
            Util.assertion(year >= y1 && year <= y2);

            double a1 = (y2 - (double) year) / (y2 - y1);
            return interpolate(a1, m1, m2);
        }

        static ChildAttritionModel interpolate(double a1, ChildAttritionModel m1, ChildAttritionModel m2) throws Exception
        {
            Util.assertion(a1 >= 0 && a1 <= 1);
            Util.assertion(m1.attrition.length == m2.attrition.length);

            double L0 = a1 * m1.L0 + (1 - a1) * m2.L0;
            double attrition[] = new double[m1.attrition.length];

            for (int age = 0; age < m1.attrition.length; age++)
                attrition[age] = a1 * m1.attrition[age] + (1 - a1) * m2.attrition[age];

            return new ChildAttritionModel(L0, attrition);
        }
    }

    static class AllModels
    {
        AllModels() throws Exception
        {
        }

        final ChildAttritionModel model_m_1926 = ChildAttritionModel.forMortalityTable("mortality_tables/USSR/1926-1927", Gender.MALE);
        final ChildAttritionModel model_m_1938 = ChildAttritionModel.forMortalityTable("mortality_tables/USSR/1938-1939", Gender.MALE);
        final ChildAttritionModel model_m_1958 = ChildAttritionModel.forMortalityTable("mortality_tables/USSR/1958-1959", Gender.MALE);

        final ChildAttritionModel model_f_1926 = ChildAttritionModel.forMortalityTable("mortality_tables/USSR/1926-1927", Gender.FEMALE);
        final ChildAttritionModel model_f_1938 = ChildAttritionModel.forMortalityTable("mortality_tables/USSR/1938-1939", Gender.FEMALE);
        final ChildAttritionModel model_f_1958 = ChildAttritionModel.forMortalityTable("mortality_tables/USSR/1958-1959", Gender.FEMALE);

        // final ChildAttritionModel model_b_1926 = ChildAttritionModel.forMortalityTable("mortality_tables/USSR/1926-1927", Gender.BOTH);
        // final ChildAttritionModel model_b_1938 = ChildAttritionModel.forMortalityTable("mortality_tables/USSR/1938-1939", Gender.BOTH);
        // final ChildAttritionModel model_b_1958 = ChildAttritionModel.forMortalityTable("mortality_tables/USSR/1958-1959", Gender.BOTH);
    }

    private static AllModels allModels = null;

    public static ChildAttritionModel selectModel(Integer yearHint, int backoffYears, Gender gender, ValuesMatter valuesMatter) throws Exception
    {
        if (allModels == null)
            allModels = new AllModels();

        int modelYear;

        if (yearHint == null)
        {
            modelYear = 1926;
        }
        else
        {
            modelYear = yearHint - backoffYears;
        }

        ChildAttritionModel model = null;

        if (gender == Gender.MALE)
        {
            model = selectModel(modelYear, allModels.model_m_1926, allModels.model_m_1938, allModels.model_m_1958, valuesMatter);
        }
        else if (gender == Gender.FEMALE)
        {
            model = selectModel(modelYear, allModels.model_f_1926, allModels.model_f_1938, allModels.model_f_1958, valuesMatter);
        }
        else
        {
            throw new IllegalArgumentException("неверный указатель пола");
        }

        model = model.clone();

        return model;
    }

    private static ChildAttritionModel selectModel(
            int modelYear,
            ChildAttritionModel m1926, ChildAttritionModel m1938,
            ChildAttritionModel m1958,
            ValuesMatter valuesMatter)
            throws Exception
    {
        if (modelYear <= 1927)
        {
            return m1926;
        }
        else if (modelYear > 1927 && modelYear <= 1939)
        {
            return ChildAttritionModel.interpolate(modelYear, 1927, m1926, 1939, m1938);
        }
        else if (modelYear > 1939 && modelYear <= 1944)
        {
            return m1938;
        }
        else
        {
            throw new Exception("ChildAttritionModel не реализована для года " + modelYear);
        }
    }
}
