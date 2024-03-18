package rtss.data.mortality.synthetic;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.ChartXY;
import rtss.util.plot.ChartXYSPlineBasic;
import rtss.util.plot.ChartXYSplineAdvanced;

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
    public RSFSR_1940() throws Exception
    {
        SingleMortalityTable mt;
        
        // males total 1940 mortality, АДХ-РСФСР page 167
        Bin[] male_mortality_bins = Bins.bins(makeBin(0, 259.8),
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
        setTable(Locality.TOTAL, Gender.MALE, makeSingleTable(male_mortality_bins));
        
        // females total 1940 mortality, АДХ-РСФСР page 169
        Bin[] female_mortality_bins = Bins.bins(makeBin(0, 225.4),
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
                                                makeBin(70, 74, 64.9),
                                                makeBin(75, 79, 99.2),
                                                makeBin(80, 84, 150.8),
                                                makeBin(85, 100, 226.8));
        setTable(Locality.TOTAL, Gender.FEMALE, makeSingleTable(female_mortality_bins));
        
        // male population 1940, АДХ-РСФСР page 157
        Bin[] male_population_bins = Bins.bins(makeBin(0, 4, 7429),
                                               makeBin(5, 9, 5388),
                                               makeBin(10, 14, 6854),
                                               makeBin(15, 19, 5117),
                                               makeBin(20, 24, 3951),
                                               makeBin(25, 29, 5155),
                                               makeBin(30, 34, 4433),
                                               makeBin(35, 39, 3463),
                                               makeBin(40, 44, 2551),
                                               makeBin(45, 49, 1942),
                                               makeBin(50, 54, 1623),
                                               makeBin(55, 59, 1288),
                                               makeBin(60, 64, 1060),
                                               makeBin(65, 69, 787),
                                               makeBin(70, 74, 447),
                                               makeBin(75, 79, 240),
                                               makeBin(80, 84, 101),
                                               makeBin(85, 100, 58));

        // female population 1940, АДХ-РСФСР page 159
        Bin[] female_population_bins = Bins.bins(makeBin(0, 4, 7301),
                                                 makeBin(5, 9, 5412),
                                                 makeBin(10, 14, 6917),
                                                 makeBin(15, 19, 5293),
                                                 makeBin(20, 24, 4480),
                                                 makeBin(25, 29, 5500),
                                                 makeBin(30, 34, 4568),
                                                 makeBin(35, 39, 3921),
                                                 makeBin(40, 44, 3129),
                                                 makeBin(45, 49, 2462),
                                                 makeBin(50, 54, 2139),
                                                 makeBin(55, 59, 2010),
                                                 makeBin(60, 64, 1686),
                                                 makeBin(65, 69, 1316),
                                                 makeBin(70, 74, 825),
                                                 makeBin(75, 79, 466),
                                                 makeBin(80, 84, 222),
                                                 makeBin(85, 100, 145));

        // ### make both: NB bins 0-4 are different from 0 and 1-4
        mt = null;
        m.put(key(Locality.TOTAL, Gender.BOTH), mt);
        
        // display(Locality.TOTAL, Gender.MALE);
    }
    
    private Bin makeBin(int age, double avg)
    {
        return makeBin(age, age, avg);
    }

    private Bin makeBin(int age1, int age2, double avg)
    {
        return new Bin(age1, age2, avg);
    }
    
    private SingleMortalityTable makeSingleTable(Bin... bins) throws Exception
    {
        double[] curve = MakeCurve.curve(bins);
        curve = Util.divide(curve, 1000);
        return SingleMortalityTable.from_qx("computed", curve);
    }
    
    private void display(Locality locality, Gender gender) throws Exception
    {
        double[] qx = getSingleTable(locality, gender).qx();
        
        Util.print("RSFSR 1940 qx", qx, 0);

        new ChartXY("RSFSR 1940 qx", "age", "mortality")
            .addSeries("qx", qx)
            .display();
        
        new ChartXYSPlineBasic("RSFSR 1940 qx", "age", "mortality")
            .addSeries("qx", qx)
            .display();

        new ChartXYSplineAdvanced("RSFSR 1940 qx", "age", "mortality")
            .addSeries("qx", qx)
            .display();
    }
}
