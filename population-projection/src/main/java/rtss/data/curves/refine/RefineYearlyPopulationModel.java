package rtss.data.curves.refine;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class RefineYearlyPopulationModel
{
    /*
     * Характерное модельное соотношение убыли среднегодового населения для возрастов 0-4,
     * при переходе от возраста (x) к возрасту (x + 1), где x = [0...4].
     * Взято по таблицам смертности Госкомстата для СССР 1926-1927, 1939-1939 и 1958-1959 гг.
     * 
     * Распределение весьма близко между календарными годами (оба пола, убыль в возрастах 0-4):
     * 
     *     1926 = 53.612, 22.581, 11.097, 7.223, 5.487 
     *     1938 = 62.054, 20.411,  9.147, 5.275, 3.113
     *     1958 = 64.474, 17.232,  8.530, 5.790, 3.974
     *     
     * То же для убыли в возрастах 5-9:
     * 
     *     1926 = 31.194, 23.981, 18.735, 14.801, 11.288
     *     1938 = 30.145, 23.602, 18.736, 15.045, 12.472
     *     1958 = 23.622, 21.850, 20.276, 17.913, 16.339
     */
    public static class AttritionModel
    {
        public int acutalYear;
        public final double L0;
        public final double[] attrition;

        public AttritionModel(int acutalYear, double L0, double[] attrition)
        {
            this.acutalYear = acutalYear;

            // average yearly population in age year 0 (assuming initial population at year start 100_000),
            // comes from mortality table Lx(0)
            this.L0 = L0;

            // yearly drops in average yearly population from year to year
            // comes from mortality table Lx(ageyear + 1) - Lx(ageyear)
            this.attrition = Util.dup(attrition);
        }

        public AttritionModel clone()
        {
            return new AttritionModel(acutalYear, L0, attrition);
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

        static AttritionModel forMortalityTable(int year, String tablePath, Gender gender) throws Exception
        {
            CombinedMortalityTable mt = new CombinedMortalityTable(tablePath);
            SingleMortalityTable smt = mt.getSingleTable(Locality.TOTAL, gender);

            double L0 = smt.get(0).Lx;
            double[] dLx = new double[15];
            for (int age = 0; age < dLx.length; age++)
                dLx[age] = smt.get(age).Lx - smt.get(age + 1).Lx;

            return new AttritionModel(year, L0, dLx);
        }
    }

    static class AllModels
    {
        AllModels() throws Exception
        {
        }

        final AttritionModel model_m_1926 = AttritionModel.forMortalityTable(1926, "mortality_tables/USSR/1926-1927", Gender.MALE);
        final AttritionModel model_m_1938 = AttritionModel.forMortalityTable(1938, "mortality_tables/USSR/1938-1939", Gender.MALE);
        final AttritionModel model_m_1958 = AttritionModel.forMortalityTable(1958, "mortality_tables/USSR/1958-1959", Gender.MALE);

        final AttritionModel model_f_1926 = AttritionModel.forMortalityTable(1926, "mortality_tables/USSR/1926-1927", Gender.FEMALE);
        final AttritionModel model_f_1938 = AttritionModel.forMortalityTable(1938, "mortality_tables/USSR/1938-1939", Gender.FEMALE);
        final AttritionModel model_f_1958 = AttritionModel.forMortalityTable(1958, "mortality_tables/USSR/1958-1959", Gender.FEMALE);
    }

    private static AllModels allModels = null;

    public static AttritionModel select_model(Integer yearHint, Gender gender) throws Exception
    {
        if (allModels == null)
            allModels = new AllModels();

        if (yearHint == null)
            yearHint = 1938;

        AttritionModel model = null;

        if (gender == Gender.MALE)
        {
            // ### interpolate
            if (yearHint >= 1943)
                model = allModels.model_m_1958;
            else if (yearHint >= 1933)
                model = allModels.model_m_1938;
            else
                model = allModels.model_m_1926;
        }
        else if (gender == Gender.FEMALE)
        {
            // ### interpolate
            if (yearHint >= 1943)
                model = allModels.model_f_1958;
            else if (yearHint >= 1933)
                model = allModels.model_f_1938;
            else
                model = allModels.model_f_1926;
        }
        else
        {
            throw new IllegalArgumentException("неверный указатель пола");
        }

        model = model.clone();
        model.acutalYear = yearHint;

        return model;
    }
}
