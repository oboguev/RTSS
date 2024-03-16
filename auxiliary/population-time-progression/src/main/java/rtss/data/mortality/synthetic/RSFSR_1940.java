package rtss.data.mortality.synthetic;

import rtss.data.bin.Bin;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;

/*
 * Таблица смертности для РСФСР 1940 г.
 * 
 * Для мужчин и женщин (сельских и городских совокупно):
 *  
 * Е.М. Андреев, Л.Е. Дарский, Т.Л. Харькова, "Демографическая история России : 1927-1959", 
 * НИИ статистики Госкомстата России, Отделение демографии, М. 1998, стр. 167-169
 * 
 * Доп. см. Е.М.Андреев, "Снижение младенческой смертности в России в 1940-1958 гг." // "Развитие населения и демографическая политика. Памяти А.Я. Кваши. Сборник статей" (ред. М.Б. Денисенко, В.В. Елизарова), 
 * М.: МАКС Пресс, 2014, стр. 108-128
 *
 * Мы генерируем только x, lx, dx, qx и px.
 * 
 * Строимые таблицы: MALE TOTAL, FEMALE TOTAL, BOTH TOTAL
 */
public class RSFSR_1940 extends CombinedMortalityTable
{
    public RSFSR_1940()
    {
        SingleMortalityTable mt;
        Bin[] bins;
        Bin[] male_bins;
        Bin[] female_bins;
        
        // males total 1940, АДХ-РСФСР page 167
        bins = bins(makeBin(0, 259.8),
                             makeBin(1, 4, 58.5),
                             makeBin(5, 9, 7.0),
                             makeBin(10, 14, 3.1),
                             makeBin(15, 19, 4.4),
                             makeBin(20, 24, 4.7),
                             makeBin(25, 29, 5.6),
                             makeBin(30, 34, 6.5),
                             makeBin(35, 39, 10.2),
                             makeBin(40, 44, 13.5),
                             makeBin(45, 49, 18.5),
                             makeBin(50, 54, 24.6),
                             makeBin(55, 59, 34.3),
                             makeBin(60, 64, 44.8),
                             makeBin(65, 69, 62.3),
                             makeBin(70, 74, 93.7),
                             makeBin(75, 79, 133.7),
                             makeBin(80, 84, 247.8),
                             makeBin(85, 100, 275.4));
        m.put(key(Locality.TOTAL, Gender.MALE), makeSingleTable(male_bins = bins));
        
        // males total 1940, АДХ-РСФСР page 169
        bins = bins(makeBin(0, 225.4),
                             makeBin(1, 4, 53.9),
                             makeBin(5, 9, 6.3),
                             makeBin(10, 14, 3.0),
                             makeBin(15, 19, 3.7),
                             makeBin(20, 24, 3.8),
                             makeBin(25, 29, 4.6),
                             makeBin(30, 34, 5.6),
                             makeBin(35, 39, 6.5),
                             makeBin(40, 44, 7.4),
                             makeBin(45, 49, 9.0),
                             makeBin(50, 54, 11.5),
                             makeBin(55, 59, 16.1),
                             makeBin(60, 64, 24.0),
                             makeBin(65, 69, 37.6),
                             makeBin(70, 74, 64.9 ),
                             makeBin(75, 79, 99.2),
                             makeBin(80, 84, 150.8),
                             makeBin(85, 100, 226.8));
        m.put(key(Locality.TOTAL, Gender.FEMALE), makeSingleTable(female_bins = bins));

        // ### make both: состав 157 - 159
        mt = null;
        m.put(key(Locality.TOTAL, Gender.BOTH), mt);
    }
    
    private Bin makeBin(int age, double avg)
    {
        return makeBin(age, age, avg);
    }

    private Bin makeBin(int age1, int age2, double avg)
    {
        return new Bin(age1, age2 + 1, avg);
    }
    
    private Bin[] bins(Bin... bins)
    {
        return bins;
    }

    private SingleMortalityTable makeSingleTable(Bin... bins)
    {
        // ###
        return null;
    }
}
