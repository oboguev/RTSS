package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/*
 * Таблицы смертности СССР и РСФСР имеют слегка заыышенные значения коэффициентов смертности в некоторых
 * старших возрастных группах, что приводит к отрицательному балансу (дефициту) потерь во время войны в этих группах.
 * 
 * Отрицательный дисбаланс составляет около 80 тыс. человек для СССР и 30 тыс. чел. для РСФСР.
 * 
 * Понизить значения коэффициентов смертности, так чтобы устранить отрицательный дефицит
 * в этих возрастных группах. 
 */
public class AdjustSeniorRates
{
    public static class Adjustment
    {
        public final Gender gender;
        public final double age1;
        public final double age2;
        public final double age_apex;

        public Adjustment(Gender gender, double age1, double age2, double age_apex)
        {
            this.gender = gender;
            this.age1 = age1;
            this.age2 = age2;
            this.age_apex = age_apex;
        }
    }

    public static CombinedMortalityTable adjust_ussr(CombinedMortalityTable mt) throws Exception
    {
        Adjustment male = new Adjustment(Gender.MALE, 80.5, 90.5, 83.2);
        Adjustment female = new Adjustment(Gender.FEMALE, 80.5, 93.5, 83.2);
        return adjust(mt, male, female);
    }

    public static CombinedMortalityTable adjust_rsfsr(CombinedMortalityTable mt) throws Exception
    {
        Adjustment male = new Adjustment(Gender.MALE, 80.5, 93.5, 83.8);
        Adjustment female = new Adjustment(Gender.FEMALE, 81.5, 86.5, 83.8);
        return adjust(mt, male, female);
    }

    private static CombinedMortalityTable adjust(CombinedMortalityTable mt, Adjustment male, Adjustment female) throws Exception
    {
        Util.assertion(male.gender == Gender.MALE);
        Util.assertion(female.gender == Gender.FEMALE);

        double[] qx_both = mt.getSingleTable(Locality.TOTAL, Gender.BOTH).qx();
        double[] qx_male = mt.getSingleTable(Locality.TOTAL, Gender.MALE).qx();
        double[] qx_female = mt.getSingleTable(Locality.TOTAL, Gender.FEMALE).qx();

        double[] male_fraction = male_fraction(qx_both, qx_male, qx_female);
        
        // ### edit qx_male, qx_female
        
        qx_both = recombine_both(male_fraction, qx_male, qx_female);

        String comment = mt.comment() + ", поправки для старших возрастов";
        
        SingleMortalityTable smt_male = SingleMortalityTable.from_qx(comment, qx_male);
        SingleMortalityTable smt_female = SingleMortalityTable.from_qx(comment, qx_female);
        SingleMortalityTable smt_both = SingleMortalityTable.from_qx(comment, qx_both);
        
        CombinedMortalityTable cmt = CombinedMortalityTable.newEmptyTable();
        cmt.setTable(Locality.TOTAL, Gender.MALE, smt_male);
        cmt.setTable(Locality.TOTAL, Gender.FEMALE, smt_female);
        cmt.setTable(Locality.TOTAL, Gender.BOTH, smt_both);
        
        return cmt;
    }

    /*
     * Составить qx_both из @qx_male и @qx_female, с учтётом весовой пропорции полов. 
     */
    private static double[] recombine_both(double[] male_fraction, double[] qx_male, double[] qx_female) throws Exception
    {
        Util.assertion(male_fraction.length == qx_male.length);
        Util.assertion(male_fraction.length == qx_female.length);

        double[] qx_both = new double[male_fraction.length];

        for (int age = 0; age < qx_both.length; age++)
            qx_both[age] = male_fraction[age] * qx_male[age] + (1 - male_fraction[age]) * qx_female[age];

        return qx_both;
    }

    /*
     * Вычислить долю мужского населения, по годам, использованную при составлении таблицы.
     * Функция опредляет величину из самой таблицы.
     * На практике, таблица ГКС 1938-1939 гг. выказывает значительные вариации в половой пропорции,
     * со значиями доли мужского пола колеблющимися от 0.25 до 0.66.
     */
    private static double[] male_fraction(double[] qx_both, double[] qx_male, double[] qx_female) throws Exception
    {
        Util.assertion(qx_both.length == qx_male.length);
        Util.assertion(qx_both.length == qx_female.length);

        double[] male_fraction = new double[qx_both.length];

        for (int age = 0; age < qx_both.length; age++)
        {
            // если мужская и женская смертности точно совпадают, пропорции следует определять из данных о населении
            // или интерполяцией соседних значений пропорции
            Util.assertion(qx_male[age] != qx_female[age]);
            male_fraction[age] = (qx_both[age] - qx_female[age]) / (qx_male[age] - qx_female[age]);
        }

        for (int age = 0; age < qx_both.length; age++)
        {
            // проверка мниимальной разумности значений
            Util.assertion(male_fraction[age] >= 0.2 && male_fraction[age] <= 0.7);
        }

        return male_fraction;
    }
}
