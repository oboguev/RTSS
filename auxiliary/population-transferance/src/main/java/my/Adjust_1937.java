package my;

import data.population.PopulationByLocality;
import data.selectors.Gender;
import data.selectors.Locality;

/* ***************************************************************

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
Таким образом, некоторая неизвестная часть этих контингентов очевидно попала в возрастную таблицу, а другая часть не попала.
В этих условиях, единственный и лучший подход, который мы можем принять: это добавить к итогам "старшей" части возрастной
таблицы 1939 года поправочную невязку в 1,944,896 чел. (1.2% всего населения или 1.6% для групп 10+).
Её половозрастное распределение, однако, остаётся неизвестным, и лучшее, что мы можем сделать -- это предположить,
что невязка вызвана неучётом части РККА или "контингентов" и распределить невязку равномерно среди населения 18-49 
пропорционально численности в возрастной таблице, и относя 90% её на мужчин, а 10% на женщин. 

*********************************************************************/

public class Adjust_1937
{
    private final double total_adjustment = 1_944_896;
    private final double male_adjustment = total_adjustment * 0.9;
    private final double female_adjustment = total_adjustment * 0.1;

    private final int age1 = 18;
    private final int age2 = 49;

    private double male_sum;
    private double female_sum;

    public PopulationByLocality adjust(final PopulationByLocality p) throws Exception
    {
        male_sum = p.sum(Locality.TOTAL, Gender.MALE, age1, age2);
        female_sum = p.sum(Locality.TOTAL, Gender.FEMALE, age1, age2);

        PopulationByLocality pto = p.clone();
        pto.resetUnknown();
        adjust(pto, Locality.RURAL);
        adjust(pto, Locality.URBAN);
        pto.resetTotal();
        pto.recalcTotal();
        pto.validate();
        return pto;
    }

    private void adjust(PopulationByLocality pto, Locality locality)
            throws Exception
    {
        adjust(pto, locality, Gender.MALE, male_adjustment / male_sum);
        adjust(pto, locality, Gender.FEMALE, female_adjustment / female_sum);
        pto.makeBoth(locality);
    }

    private void adjust(PopulationByLocality pto, Locality locality, Gender gender, double factor)
            throws Exception
    {
        for (int age = age1; age <= age2; age++)
        {
            double v = pto.get(locality, gender, age);
            pto.add(locality, gender, age, v * factor);
        }
    }
}
