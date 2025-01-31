package rtss.ww2losses.util;

import rtss.data.asfr.AgeSpecificFertilityRatesByYear;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.synthetic.PopulationADH;
import rtss.ww2losses.params.AreaParameters;

public class CalibrateASFR
{
    /*
     * Определить поправочный (калибровочный) коэффциент к набору таблиц возрастных коэффициентов плодовитости.
     * 
     * Поправка нужна т.к. таблицы составленные по анамнестическому опросу 1960 года относятся не к РСФСР, а к
     * территории частично перекрывающейся с частью территории РСФСР.
     * 
     * Мы вычисляем число рождений вытекающее из значений в.к.п. для 1940 года и структуры населения РСФСР в середине 
     * 1940 года, и сравниваем с фактическим числом рождений в РСФСР в 1940 году.
     * 
     * Из этого сравнения мы высчисляем поправочный (калибровочный) множитель, прилагаемый затем для
     * значений в.к.п. в 1941-1945 гг. перед отнесением их к населению РСФСР.   
     */
    public static double calibrate1940(AreaParameters ap, AgeSpecificFertilityRatesByYear asfrs) throws Exception
    {
        PopulationByLocality p1940 = PopulationADH.getPopulationByLocality(ap.area, 1940);
        PopulationByLocality p1941 = PopulationADH.getPopulationByLocality(ap.area, 1941);
        PopulationByLocality p = p1940.avg(p1941);
        
        double cbr = asfrs.getForYear(1940).birthRate(p);
        double multiplier = ap.CBR_1940_MIDYEAR / cbr;
        return multiplier;
    }
}
