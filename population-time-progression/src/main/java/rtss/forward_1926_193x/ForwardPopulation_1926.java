package rtss.forward_1926_193x;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.synthetic.PatchMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.ForwardPopulationUR;
import rtss.data.rates.Recalibrate;
import rtss.data.rates.Recalibrate.Rates;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class ForwardPopulation_1926 extends ForwardPopulationUR
{
    protected CombinedMortalityTable mt1926 = new CombinedMortalityTable("mortality_tables/USSR/1926-1927");
    protected CombinedMortalityTable mt1938 = new CombinedMortalityTable("mortality_tables/USSR/1938-1939");

    protected ForwardPopulation_1926() throws Exception
    {
    }
    
    protected void calcBirthRates() throws Exception
    {
        /*
         * ЦСУ СССР, "Естественное движение населения Союза ССР в 1926 г.", т. 1, вып. 2, М. 1929 (стр. 39):
         * рождаемость во всём СССР = 44.0
         * рождаемость в сельских местностях СССР = 46.1
         * смертность для территории СССР = 20.3
         */
        double BirthRateTotal = 44.0;
        BirthRateRural = 46.1;
        double CDR = 20.3;

        /*
         * Для целей передвижки требуется уровень рождаемости нормированный на население начала года
         * для временного отрезка [0, T] (а не на население середины года с отрезком [-T,-T]).    
         */
        Rates rt = new Rates().cbr(BirthRateTotal).cdr(CDR);
        Rates rr = new Rates().cbr(BirthRateRural).cdr(CDR);
        rt = Recalibrate.m2e(rt);
        rr = Recalibrate.m2e(rr);

        BirthRateTotal = rt.cbr;
        BirthRateRural = rr.cbr;
        
        PopulationByLocality p1926 = PopulationByLocality.census(Area.USSR, 1926);
        final double ruralPopulation = p1926.sum(Locality.RURAL, Gender.BOTH, 0, MAX_AGE);
        final double urbanPopulation = p1926.sum(Locality.URBAN, Gender.BOTH, 0, MAX_AGE);
        
        BirthRateUrban = (BirthRateTotal * (ruralPopulation + urbanPopulation) - BirthRateRural * ruralPopulation) / urbanPopulation;
        
        /*
         * Результат вычисления рождаемости в нормировке на середину года: 
         *    городское = 34.4   сельское = 46.1
         *    
         * ЦСУ СССР, "Статистический справочник СССР за 1928", М. 1929 (стр. 76-77) приводит для Европейской части СССР на 1927 год   
         *    городское = 32.1   сельское = 45.5
         *    
         * Результат вычисления в нормировке на начало года:
         * 
         *    BirthRateTotal  44.53 = 44.5
         *    BirthRateRural  46.70 = 46.7
         *    BirthRateUrban  34.55 = 34.6
         *    СDR = 20.54 - 20.56 = 20.5 - 20.6
         */
    }
    
    protected CombinedMortalityTable interpolateMortalityTable(int year) throws Exception
    {
        if (year < 1926)
            year = 1926;
        else if (year > 1938)
            year = 1938;
        
        double weight = ((double)year - 1926) / (1938 - 1926);
        return CombinedMortalityTable.interpolate(mt1926, mt1938, weight);
    }
    
    
    /*
     * Уровни младенческой смертности по АДХ:
     * Е.М. Андреев, Л.Е. Дарский, Т.Л. Харькова, "Население Советского Союза 1922-1991", стр. 57, 135
     */
    private final double ADH_infant_CDR_1926_1927 = 189.5;
    private final double ADH_infant_CDR_1938_1939 = 171.0;
    
    /*
     * Если @useADHInfantMortalityRate = true, то изменить уровень младенческой смертности в таблицах mt1926 и mt1938
     * на вычисленный АДХ. Изменение производится равномерно (пропорционально) для всех местностей и полов.
     */
    protected void tuneInfantMortalityRate(boolean useADHInfantMortalityRate) throws Exception
    {
        if (useADHInfantMortalityRate)
        {
            mt1926 = PatchMortalityTable.patchInfantMortalityRate(mt1926, ADH_infant_CDR_1926_1927, "infant mortality patched to AHD");
            mt1938 = PatchMortalityTable.patchInfantMortalityRate(mt1938, ADH_infant_CDR_1938_1939, "infant mortality patched to AHD");
        }
    }
}
