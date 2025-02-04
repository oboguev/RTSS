package rtss.ww2losses.population_194x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.population.struct.Population;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.math.interpolate.LinearInterpolator;
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
        /*
         * Возрастающая сила коррекции с возраста @age1 по @age_apex.
         * Убыааюшая сила коррекции с возраста @age_apex по @age2.
         */
        public final Gender gender;
        public final double age1;
        public final double age2;
        public final double age_apex;
        public final double amount;

        public Adjustment(Gender gender, double age1, double age2, double age_apex, double amount)
        {
            this.gender = gender;
            this.age1 = age1;
            this.age2 = age2;
            this.age_apex = age_apex;
            this.amount = amount;
        }
    }

    public static CombinedMortalityTable adjust_ussr(CombinedMortalityTable mt) throws Exception
    {
        Adjustment male = null, female = null;
        male = new Adjustment(Gender.MALE, 80.5, 90.5, 83.2, 0.01);
        female = new Adjustment(Gender.FEMALE, 80.5, 93.5, 83.2, 0.01);
        return adjust(mt, male, female, null);
    }

    public static CombinedMortalityTable adjust_rsfsr(CombinedMortalityTable mt) throws Exception
    {
        Adjustment male = null, female = null;
        male = new Adjustment(Gender.MALE, 80.5, 93.5, 83.8, 0.01);
        female = new Adjustment(Gender.FEMALE, 81.5, 86.5, 83.8, 0.01);
        return adjust(mt, male, female, null);
    }

    private static CombinedMortalityTable adjust(CombinedMortalityTable mt, Adjustment adj_male, Adjustment adj_female, Population p) throws Exception
    {
        Util.assertion(adj_male.gender == Gender.MALE);
        Util.assertion(adj_female.gender == Gender.FEMALE);

        double[] qx_both = mt.getSingleTable(Locality.TOTAL, Gender.BOTH).qx();
        double[] qx_male = mt.getSingleTable(Locality.TOTAL, Gender.MALE).qx();
        double[] qx_female = mt.getSingleTable(Locality.TOTAL, Gender.FEMALE).qx();

        double[] male_fraction;
        if (p != null)
            male_fraction = male_fraction(p);
        else
            male_fraction = male_fraction(qx_both, qx_male, qx_female);

        if (adj_male != null)
            qx_male = adjust(qx_male, adj_male);

        if (adj_female != null)
            qx_female = adjust(qx_female, adj_female);

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

    /* ======================================================================================================= */

    private static double[] adjust(double[] qx, Adjustment adj) throws Exception
    {
        double[] adjustment = new double[qx.length];
        double[] rqx = new double[qx.length];
        
        LinearInterpolator li = new LinearInterpolator(adj.age1, 0, adj.age_apex, adj.amount);
        
        for (int age = (int) Math.floor(adj.age1); age <= Math.ceil(adj.age_apex); age++)
        {
            Part part = part(age, adj.age1, adj.age_apex);
            if (part.width() != 0)
                adjustment[age] += li.interp(part.mid()) * part.width();
        }

        li = new LinearInterpolator(adj.age_apex, adj.amount, adj.age2, 0);

        for (int age = (int) Math.floor(adj.age_apex); age <= Math.ceil(adj.age2); age++)
        {
            Part part = part(age, adj.age_apex, adj.age2);
            if (part.width() != 0)
                adjustment[age] += li.interp(part.mid()) * part.width();
        }

        for (int age = 0; age < qx.length; age++)
        {
            rqx[age] = qx[age] - adj.amount * adjustment[age];
            Util.assertion(rqx[age] > 0);
        }
        
        // ### return rqx

        return qx;
    }

    /*
     * определить, какая доля возрастного диапазона [age... age+1[ находится в диапазоне [age1...age2]
     */
    public static class Part
    {
        public double xa1;
        public double xa2;
        
        public Part(double xa1, double xa2)
        {
            this.xa1 = xa1;
            this.xa2 = xa2;
        }
        
        public double width()
        {
            return xa2 - xa1;
        }
        
        public double mid()
        {
            return (xa1 + xa2) / 2;
        }
    }
    
    private static Part part(int age, double age1, double age2) throws Exception
    {
        return part(age, age + 1.0, age1, age2);
    }

    private static Part part(double a1, double a2, double b1, double b2) throws Exception
    {
        Util.assertion(a2 >= a1);
        Util.assertion(b2 >= b1);

        if (a1 >= b2 || a2 <= b1)
            return new Part(0, 0);

        double xa1 = Math.max(a1, b1);
        double xa2 = Math.min(a2, b2);

        Util.assertion(xa2 >= xa1 && xa1 >= b1 && xa2 <= b2);

        return new Part(xa1, xa2);
    }

    /* ======================================================================================================= */

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

        // проверка мниимальной разумности значений
        for (int age = 0; age < qx_both.length; age++)
        {
            Util.assertion(male_fraction[age] >= 0.2 && male_fraction[age] <= 0.7);
        }

        return male_fraction;
    }

    private static double[] male_fraction(Population p) throws Exception
    {
        double[] male_fraction = new double[Population.MAX_AGE + 1];

        for (int age = 0; age < male_fraction.length; age++)
        {
            double m = p.male(age);
            double f = p.female(age);
            male_fraction[age] = m / (m + f);
        }

        return male_fraction;
    }
}
