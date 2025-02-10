package rtss.ww2losses.population194x;

import java.util.ArrayList;
import java.util.List;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.MatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchInstruction;
import rtss.data.mortality.synthetic.PatchMortalityTable.PatchOpcode;
import rtss.data.population.projection.ForwardPopulationT;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.ww2losses.params.AreaParameters;

/**
 * Вычислить таблицу смертности населения СССР или РСФСР для 1940 года.
 * 
 * === Для случая СССР:
 * 
 * Для случая СССР, АДХ к сожалению не опубликовали вычисленные и использованные ими возрастные показатели смертности, 
 * в т.ч. для 1940 года. Насколько можно судить, для взрослых возрастов они полагали их близкими по распределению (форме кривой) 
 * к госкомстатовской таблице смертности для 1938-1939 гг. ("Таблицы смертности и ожидаемой продолжительности жизни населения", 
 * М. : Государственный комитет СССР по статистике, 1989, стр. 33-50):
 *
 *    "Расчет возрастных чисел умерших проводился в несколько этапов путем передвижки населения 1939 г. с использованием модифицированных 
 *    таблиц смертности 1938-1939 гг., так, чтобы при данных числах живущих в каждом возрасте они давали заданное общее число умерших... 
 *    При модификации таблиц смертности 1938-1939 гг. мы воспользовались независимыми оценками уровня младенческой смертности, 
 *    рассчитанными нами по данным о младенческой смертности на территориях с хорошей регистрацией." (АДХ-СССР, стр. 54-55). 
 *
 * Для 1940 г. этот уровень младенческой смертности полагается равным 184.0 промилле (АДХ-СССР, стр. 135) против 163.5 по ГКС для 1938-1939.
 *
 * Общий уровень смертности в 1940 году возрос сравнительно с 1938 годом (по АДХ, с 20.9 до 21.7 промилле) при одновременном снижении 
 * уровня рождаемости (с 39.0 до 36.1 промилле), что означает ещё более высокий прирост возрастных коэфициентов смертности, чем подразумевается 
 * одним лишь возрастанием уровня смертности, т.к. число смертей распределилось из-за падение рождаемости на сравнительно меньшее население 
 * подверженное смертности (exposed population), чем было бы при прежнем уровне рождаемости.
 *
 * Соответственно этому, мы строим таблицу возрастных коэффициентов смертности для 1940 года отправляясь от госкомстатовской таблицы 1938-1939 гг. 
 * и видоизменяя её следующим образом:
 * 
 *     - Младенческая смертность (q0 для взвешенного среднего обоих полов) повышается до 184 промилле, пропорциональные повышения делаются 
 *       также в таблицах для отдельных полов.
 *     - Смертность в возрастах 1-5 лет повышается на множитель линейно спадающий с возрастом от величины 184.0/163.5 в возрасте 1 год 
 *       до 1.0 в возрасте 5 лет.
 *     - Возрастные коэффициенты смертности в возрастах 5-100 лет повышаются на одинаковый множитель, при котором полученная таблица даёт 
 *       годовую смертность в 21.7 промилле при рождаемости 36.1 промилле (с учётом смертности рождённых в текущем году). 
 *       Этот множитель исчислим как 1.326.
 *       
 * ***************
 *       
 * === Для случая РСФСР:
 * 
 * Таблица смертности на 1940 год для РСФСР основана на возрастных коэффициентах смертности РСФСР для 1940 года расчитаннных АДХ 
 * (АДХ-Россия, стр. 167-170). Для получения требуемой смертности при данной рождаемости, нам приходится уменьшить возрастные коэффициенты 
 * смертности (для всех возрастов равномерно) на множитель 0.967 сравнительно с расчитанными АДХ. Различие в 3.3% может быть связано 
 * с расхождениями вызванными дезагрегацией значений возрастных коэффициентов смертности и половозрастной структуры населения.
 * 
 */
public class MortalityTable_1940 extends UtilBase_194x
{
    private AreaParameters ap;
    
    /* уровень младенческой смертности в СССР в 1940 году по АДХ (АДХ-СССР, стр. 135) */
    private static final double PROMILLE = 1000.0;
    private static final double ADH_USSR_infant_CDR_1940 = 184.0 / PROMILLE;
    private static final boolean use_ADH_USSR_InfantMortalityRate = true;
    
    public MortalityTable_1940(AreaParameters ap) throws Exception
    {
        this.ap = ap;
    }

    public CombinedMortalityTable evaluate() throws Exception
    {
        return evaluate(false);
    }

    public CombinedMortalityTable evaluate(boolean print) throws Exception
    {
        PopulationByLocality p1940 = new Population_In_Early_1940(ap).evaluate();
        
        if (ap.area == Area.USSR)
        {
            CombinedMortalityTable mt = CombinedMortalityTable.load("mortality_tables/USSR/1938-1939");
            mt.comment("ГКС-СССР-1938");

            double[] qx = mt.getSingleTable(Locality.TOTAL, Gender.BOTH).qx();
            
            List<PatchInstruction> instructions = new ArrayList<>();
            PatchInstruction instruction;
            
            if (use_ADH_USSR_InfantMortalityRate)
            {
                /*
                 * Младенческая смертность по АДХ
                 */
                instruction = new PatchInstruction(PatchOpcode.Multiply, 0, 0, ADH_USSR_infant_CDR_1940 / qx[0]);
                instructions.add(instruction);

                instruction = new PatchInstruction(PatchOpcode.MultiplyWithDecay, 1, 5, ADH_USSR_infant_CDR_1940  / qx[0], 1.0);
                instructions.add(instruction);
            }
            
            /*
             * Рабочий дескриптор для MatchMortalityTable.match.
             * Равномерно повысить коэффициенты смертности в возрастах 5-100 так, чтобы в наседении p1940 
             * при рождаемости CBR_1940 достигалась смертность CDR_1940.
             */
            instruction = new PatchInstruction(PatchOpcode.Multiply, 5, Population.MAX_AGE, 1.0);
            instructions.add(instruction);
            
            CombinedMortalityTable xmt = MatchMortalityTable.match(mt, p1940, instructions, ap.CBR_1940, ap.CDR_1940, "модиф. для СССР 1940");
            Util.out(String.format("Для таблицы смертности СССР 1940 года все коэффициенты в возрастах 5-100 увеличены на %.4f", instruction.scale));

            /*
             * Исправить коэффициенты смертности в старших возрастах (80+)
             */
            xmt = AdjustSeniorRates.adjust_ussr(xmt);
            
            return xmt;
        }
        else if (ap.area == Area.RSFSR)
        {
            CombinedMortalityTable mt = CombinedMortalityTable.loadTotal("mortality_tables/RSFSR/1940");
            mt.comment("АДХ-РСФСР-1940");
            
            // рабочий дескриптор для MatchMortalityTable.match
            List<PatchInstruction> instructions = new ArrayList<>();
            PatchInstruction instruction = new PatchInstruction(PatchOpcode.Multiply, 0, Population.MAX_AGE, 1.0);
            instructions.add(instruction);

            /*
             * Равномерно повысить коэффициенты смертности в возрастах 0-100 так, чтобы в наседении p1940 
             * при рождаемости CBR_1940 достигалась смертность CDR_1940.
             */
            CombinedMortalityTable xmt = MatchMortalityTable.match(mt, p1940, instructions, ap.CBR_1940, ap.CDR_1940, "модиф. для РСФСР 1940");
            Util.out(String.format("Для таблицы смертности РСФСР 1940 года все коэффициенты увеличены на %.4f", instruction.scale));

            /*
             * Исправить коэффициенты смертности в старших возрастах (80+)
             */
            xmt = AdjustSeniorRates.adjust_rsfsr(xmt);

            return xmt;
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }
    
    /** ====================================================================================================================== **/

    private CombinedMortalityTable mt1;
    private CombinedMortalityTable mt2;
    
    private void init_mts() throws Exception
    {
        mt1 = CombinedMortalityTable.load("mortality_tables/USSR/1938-1939");
        mt1.comment("ГКС-СССР-1938");
        if (use_ADH_USSR_InfantMortalityRate)
        {
            mt1 = PatchMortalityTable.patchInfantMortalityRate(mt1, ADH_USSR_infant_CDR_1940 * PROMILLE, "infant mortality patched to ADH");
        }

        mt2 = CombinedMortalityTable.loadTotal("mortality_tables/RSFSR/1940");
        mt2.comment("АДХ-РСФСР-1940");
    }

    public void show_survival_rates_1941_1946() throws Exception
    {
        init_mts();
        show_survival_rates_1941_1946(mt1);
        show_survival_rates_1941_1946(mt2);
        show_survival_rates_1941_1946(evaluate());
    }
    
    /*
     * Вероятности дожития при равномерном распределении по возрастам в указанной группе
     */
    private void show_survival_rates_1941_1946(CombinedMortalityTable mt) throws Exception
    {
        Util.out("");
        Util.out(String.format("Survival rate 1941 -> 1946 using life table %s:", mt.comment()));
        Util.out("");
        Util.out("  age      M      F  ");
        Util.out("=======  =====  =====");
        
        show_survival_rates_1941_1946_newborn(mt);
        show_survival_rates_1941_1946(mt, 0, 4);
        show_survival_rates_1941_1946(mt, 5, 14);
        show_survival_rates_1941_1946(mt, 15, 24);
        show_survival_rates_1941_1946(mt, 25, 34);
        show_survival_rates_1941_1946(mt, 35, 44);
        show_survival_rates_1941_1946(mt, 45, 54);
        show_survival_rates_1941_1946(mt, 55, 64);
        show_survival_rates_1941_1946(mt, 65, 74);
        show_survival_rates_1941_1946(mt, 75, Population.MAX_AGE);
    }

    private void show_survival_rates_1941_1946_newborn(CombinedMortalityTable mt) throws Exception
    {
        double m_rate = survival_rates_1941_1946_newborn(mt, Gender.MALE);
        double f_rate = survival_rates_1941_1946_newborn(mt, Gender.FEMALE);
        Util.out(String.format("%7s  %.3f  %.3f", "newborn", m_rate, f_rate));
    }

    private void show_survival_rates_1941_1946(CombinedMortalityTable mt, int age1, int age2) throws Exception
    {
        double m_rate = survival_rates_1941_1946(mt, Gender.MALE, age1, age2);
        double f_rate = survival_rates_1941_1946(mt, Gender.FEMALE, age1, age2);
        String ages = String.format("%s-%s", age1, age2);
        Util.out(String.format("%7s  %.3f  %.3f", ages, m_rate, f_rate));
    }

    private double survival_rates_1941_1946(CombinedMortalityTable mt, Gender gender, int age1, int age2) throws Exception
    {
        // build population for the start of 1941
        PopulationByLocality p = PopulationByLocality.newPopulationTotalOnly();

        for (int age = 0; age <= PopulationByLocality.MAX_AGE; age++)
        {
            p.set(Locality.TOTAL, Gender.MALE, age, 0);
            p.set(Locality.TOTAL, Gender.FEMALE, age, 0);
        }
        
        for (int age = age1; age <= age2; age++)
            p.set(Locality.TOTAL, gender, age, 1);
        
        double sum0 = p.sum(Locality.TOTAL, gender, 0, PopulationByLocality.MAX_AGE);
        PopulationContext fctx = new PopulationContext();
        p = fctx.begin(p);

        // to early 1942
        ForwardPopulationT fw = new ForwardPopulationT();
        fw.setBirthRateTotal(0);
        p = fw.forward(p, fctx, mt, 1);

        // to early 1943
        p = fw.forward(p, fctx, mt, 1);

        // to early 1944
        p = fw.forward(p, fctx, mt, 1);

        // to early 1945
        p = fw.forward(p, fctx, mt, 1);

        // to early 1946
        p = fw.forward(p, fctx, mt, 1);

        p = fctx.end(p);

        double sum = p.sum(Locality.TOTAL, gender, 0, PopulationByLocality.MAX_AGE);

        return sum / sum0;
    }

    private double survival_rates_1941_1946_newborn(CombinedMortalityTable mt, Gender gender) throws Exception
    {
        // build population for the start of 1941
        PopulationByLocality p = PopulationByLocality.newPopulationTotalOnly();

        for (int age = 0; age <= PopulationByLocality.MAX_AGE; age++)
        {
            p.set(Locality.TOTAL, Gender.MALE, age, 0);
            p.set(Locality.TOTAL, Gender.FEMALE, age, 0);
            p.set(Locality.TOTAL, Gender.BOTH, age, 0);
        }
        
        PopulationContext fctx = new PopulationContext();
        double sum0 = 1.0;
        fctx.setDay(Locality.TOTAL, gender, 0, sum0);

        // forward to early 1942
        ForwardPopulationT fw = new ForwardPopulationT();
        fw.setBirthRateTotal(0);
        p = fw.forward(p, fctx, mt, 1);

        // to early 1943
        p = fw.forward(p, fctx, mt, 1);

        // to early 1944
        p = fw.forward(p, fctx, mt, 1);

        // to early 1945
        p = fw.forward(p, fctx, mt, 1);

        // to early 1946
        p = fw.forward(p, fctx, mt, 1);

        p = fctx.end(p);

        double sum = p.sum(Locality.TOTAL, gender, 0, PopulationByLocality.MAX_AGE);

        return sum / sum0;
   }
}