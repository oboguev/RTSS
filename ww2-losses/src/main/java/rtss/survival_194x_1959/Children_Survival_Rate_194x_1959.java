package rtss.survival_194x_1959;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.mortality.synthetic.MortalityTableADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/**********************************************************************************************************************

Вычислить вероятность доживания детей родившихся в июне 1941 - мае 1945 гг. до переписи 15 января 1959 года
в том случае, если бы 1941-1945 не были военными годами.

В этом случае детская смертность характерная для 1938-1939 г. сохранилась бы до 1943 года, а затем начала снижение 
из-за введения сульфаниламидных, а позднее и других антибактериальных препаратов, как это и произошло в действительности.  

(Р.И. Сифман, "К вопросу о причинах снижения детской смертности в годы Великой Отечественной войны" // 
Продолжительность жизни: анализ и моделирование, ред. Е.М. Андреев и А.Г. Вишневский, М.: Статистика, 1979, стр. 50-60).

См. тж.:

Е.А. Кваша, "Младенческая смертность в России в XX веке" // "Социологические исследования", 2003 №6, стр. 47-55

Е.М. Андреев, "Снижение младенческой смертности в России в 1940-1958 гг." // 
"Развитие населения и демографическая политика. Памяти А.Я. Кваши. Сборник статей" (ред.М.Б. Денисенко, В.В. Елизарова), М.: МАКС Пресс, 2014. стр. 92-111

Е.М. Андреев, Л.Е. Дарский, Т.Л. Харькова, "Демографическая история России : 1927-1959", НИИ статистики Госкомстата России, Отделение демографии, М. 1998, таблица приложения 3, стр. 164-165

Как видно из расчётов Андреева, Дарского и Харьковой, снижение младенческой смертности в 1948-1958 гг. носило почти линейный характер, с незначительными годовыми уклонениями от линии тренда. 
1946 и 1947 гг. оказываются в том же тренде, но по горькой иронии из-за прироста младенческой смертности в 1947 году от голода. Если бы не советский голод 1946-1947 гг., можно полагать,
что снижение младенческой по-прежнему носило линейный со временем характер, но по более пологой траектории. Можно полагать, что снижение и остальных составляющих детской смертности в 
возрастах старше первого года жизни также носило характер близкий к линейному.   

Можно поэтому с основанием полагать, что в условиях мира, если бы война 1941-1945 гг. не произошла, возрастные уровни детской смертности характерные для 1938-1939 гг.
сохранялись бы до 1943 года, а затем начали плавное снижение к уровням 1958-1959 гг., причём это снижение носило линейный или близкий к линейному характер.

Соответственно этому, мы расчитываем вероятность доживания детей родившихся в июне 1941 - мае 1945 гг. на основе таблицы возрастной смертности 1938-1939 гг.
применяемой до 1943 года, а затем на основе таблицы интерполируемой между 1943 и 1958 гг. с постепенно убывающей год от года смертностью.

Мы расчитывем вероятность доживания до января 1959 для детей каждого из четырёх лет войны (в том случае, если бы она не произошла).
Для детей первого года войны средняя дата рождения - январь 1942. 
Для детей второго года войны средняя дата рождения - январь 1943.
Для детей третьего года войны средняя дата рождения - январь 1944.
Для детей четвёртого года войны средняя дата рождения - январь 1945.

************************************************************************************************************************/

public class Children_Survival_Rate_194x_1959
{
    public static void main(String[] args)
    {
        try
        {
            Children_Survival_Rate_194x_1959 m = new Children_Survival_Rate_194x_1959();
            m.do_main();
        }
        catch (Throwable ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
            System.exit(1);
        }

        Util.out("");
        Util.out("*** Completed.");
    }
    
    public Children_Survival_Rate_194x_1959() throws Exception
    {
    }

    private void do_main() throws Exception
    {
        eval_survival(Area.USSR);
        Util.out("");
        Util.out("===============================================================");
        Util.out("");
        eval_survival(Area.RSFSR);
    }
    
    private void eval_survival(Area area) throws Exception
    {
        double[] p = new double[4];
        
        Util.out("Вероятность детей родившихся в июне 1941 - мае 1945 гг. дожить до переписи 15 января 1959 года, если бы не было войны:");
        Util.out("для " + area);
        Util.out("");

        Util.out("год войны  вероятность доживания до 1959");
        Util.out("=========  =============================");
        for (int war_year = 1; war_year <= 4;  war_year ++)
        {
            int birth_year = 1941 + war_year;
            double px = eval_survival_rate(birth_year, area);
            Util.out(String.format("%9d           %.2f", war_year, px));
            p[war_year - 1] = px;
        }
        Util.out(String.format("%9s           %.2f", "среднее", Util.average(p)));
    }
    
    final private CombinedMortalityTable mt1938_ussr = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");
    final private CombinedMortalityTable mt1958_ussr = new CombinedMortalityTable("mortality_tables/USSR/1958-1959");
    final private CombinedMortalityTable mt1940_rsfsr = MortalityTableADH.getMortalityTable(Area.RSFSR, 1940); 
    final private CombinedMortalityTable mt1958_rsfsr = new CombinedMortalityTable("mortality_tables/RSFSR/1958-1959");
    
    private CombinedMortalityTable mt1942;
    private CombinedMortalityTable mt1958;
  
    private double eval_survival_rate(int birth_year, Area area) throws Exception
    {
        if (area == Area.USSR)
        {
            mt1942 = mt1938_ussr;
            mt1958 = mt1958_ussr;
            // mt1958 = mt1958_rsfsr; 
        }
        else if (area == Area.RSFSR)
        {
            mt1942 = mt1940_rsfsr;
            mt1958 = mt1958_rsfsr;
        }

        double px = 1.0;
        
        for (int year = birth_year; year <= 1958; year++)
        {
            CombinedMortalityTable mt = tableForYear(year);
            int age = year - birth_year;
            MortalityInfo mi = mt.get(Locality.TOTAL, Gender.BOTH, age);
            px *= mi.px;
        }
        
        return px;
    }
    
    private CombinedMortalityTable tableForYear(int year) throws Exception
    {
        if (year <= 1942)
        {
            return mt1942;
        }
        else if (year >= 1958)
        {
            return mt1958;
        }
        else
        {
            // interpolate between mt1938 in 1942 and mt1958 in 1958
            double weight = ((double)year - 1942) / (1958 - 1942);
            return CombinedMortalityTable.interpolate(mt1942, mt1958, weight);
        }
    }
}
