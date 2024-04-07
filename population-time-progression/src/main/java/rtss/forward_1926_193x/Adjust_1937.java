package rtss.forward_1926_193x;

import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

/** **************************************************************

Сохранившиеся в архиве и опубликованные материалы переписи 

    В.Б. Жиромская, Ю.А. Поляков, "Всесоюзная перепись населения 1937 года : Общие итоги", М. : РОССПЭН, 2007, стр. 77-84

содержат численные расхождения.

Мы исправили urban-male-28 c 586,498 на 486,498 для схождения с таблицей 11 (оба пола) и с суммой "итого" в таблице 10 для возрастов 20-29.
Также исправлено urban-female-12 с 526,895 на 525,895 для схождения с "итого" и таблицей 11.
Другие расхождения в таблице 10 менее значительны.

В таблице 11 опечатки: 
Графа 27.3 должна быть не 1,585,569, а 1,858,749.
Графа unknown.3 должна быть не 59,918, а 50,918.

Таблица содержит городское и сельское население.
Кроме него:

1.
в РККА находились 1,681,043 мужчин и 1,526 женщин (стр. 244)

2.
контингент "А"  (стр. 251)
городское, мужчины = 192,197
городское, женщины = 6,287
городское, оба пола = 198,484

сельское, мужчины = 71,269
сельское, женщины = 977
сельское, оба пола = 72,246

всего, мужчины = 263,466
всего, женщины = 7,264
всего, оба пола = 270,730

3.
контингенты "Б" и В (стр. 246-250)

городское, мужчины = 747,392
городское, женщины = 206,316
городское, оба пола = 953,708
в т.ч. 18 лет и старше = 748,731

сельское, мужчины = 1,070,246
сельское, женщины = 365,616
сельское, оба пола = 1,435,862
в т.ч. 18 лет и старше = 1,045,373

всего, мужчины = 1,817,638
всего, женщины = 571,932
всего, оба пола = 2,389,570
в т.ч. 18 лет и старше = 1,794,104

Всего таблицей возрастной структуры переписи охвачены 160,058,329 чел., 
однако перепись сообщила об общей численности населения СССР 162,003,225 человек, включая контингенты РККА и НКВД (стр. 37).

Невязка в 1,944,896 чел. (1.2% всего населения или 1.6% для групп 10+) относима на РККА и часть погранохраны 
(Андреев,Дарский,Харькова, "Население Советского Союза 1922-1991", М. Наука, 1993, стр. 27).

Срок действительной службы установленный законом СССР об обязательной военной службе 13.8.1930 г. составлял 2 года в частях РККА
и 3 или 4 года (в зависимости от подготовки) в частях РККФ и пограничной охраны.

Чтобы учесть невязку, мы распределили 85% её величины на призывные возраста 18.5-21.5, а 15% на возраста 21.5-49 пропорционально 
их численности в возрастной таблице, относя 90% невязки на мужчин, а на 10% на женщин.

*********************************************************************/

public class Adjust_1937
{
    private final double total_adjustment = 1_944_896;

    public PopulationByLocality adjust(final PopulationByLocality p) throws Exception
    {
        PopulationByLocality pto = p.clone();
        pto.resetUnknown();
        
        adjust_1(pto, total_adjustment * 0.85);
        adjust_2(pto, total_adjustment * 0.15);

        pto.resetTotal(); // ### или в обратном порядке?
        pto.recalcTotal();
        pto.validate();
        
        return pto;
    }
    
    /* ================================================================================================= */

    /*
     * Для призывных возрастов (18.5-21.5)
     */
    private void adjust_1(PopulationByLocality p, double amount) throws Exception
    {
        double w18 = 0.5;
        double w19 = 1.0;
        double w20 = 1.0;
        double w21 = 0.5;

        double s18 = w18 * p.get(Locality.TOTAL, Gender.BOTH, 18);
        double s19 = w19 * p.get(Locality.TOTAL, Gender.BOTH, 19);
        double s20 = w20 * p.get(Locality.TOTAL, Gender.BOTH, 20);
        double s21 = w21 * p.get(Locality.TOTAL, Gender.BOTH, 21);
        double s_all = s18 + s19 + s20 + s21;
        
        adjust_for_age(p, 18, amount * s18 / s_all);
        adjust_for_age(p, 19, amount * s19 / s_all);
        adjust_for_age(p, 20, amount * s20 / s_all);
        adjust_for_age(p, 21, amount * s21 / s_all);
    }

    /*
     * Увеличить численность населения в указанном возрасте на @amount,
     * распределив 90% прибавки на мужчин, 10% на женщин
     */
    private void adjust_for_age(PopulationByLocality p, int age, double amount) throws Exception
    {
        adjust_for_age(p, age, Gender.MALE, 0.9 * amount);
        adjust_for_age(p, age, Gender.FEMALE, 0.1 * amount);
    }

    /*
     * Увеличить численность населения указанного возраста и пола на @amount,
     * распределив его между городским и сельским населением пропорционально их уже имеющейся численности
     */
    private void adjust_for_age(PopulationByLocality p, int age, Gender gender, double amount) throws Exception
    {
        double rural = p.get(Locality.RURAL, gender, age); 
        double urban = p.get(Locality.URBAN, gender, age);
        
        p.add(Locality.RURAL, gender, age, amount * rural / (rural + urban));
        p.add(Locality.URBAN, gender, age, amount * urban / (rural + urban));
                
        p.makeBoth(Locality.RURAL);
        p.makeBoth(Locality.URBAN);
    }
    
    /* ================================================================================================= */

    /*
     * Для старших возрастов (21.5 - 49)
     */
    private void adjust_2(PopulationByLocality p, double amount) throws Exception
    {
        double s_all = 0;
        for (int age = 21; age <= 49; age++)
        {
            double w = (age == 21) ? 0.5 : 1.0;
            double s = w * p.get(Locality.TOTAL, Gender.BOTH, age);
            s_all += s;
        }

        for (int age = 21; age <= 49; age++)
        {
            double w = (age == 21) ? 0.5 : 1.0;
            double s = w * p.get(Locality.TOTAL, Gender.BOTH, age);
            adjust_for_age(p, age, amount * s / s_all);
        }
    }
}
