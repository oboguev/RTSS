package rtss.losses199x;

import java.util.HashMap;
import java.util.Map;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.projection.ForwardPopulationUR;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class BirthsDeficit
{
    // private CombinedMortalityTable cmt = LoadData.mortalityTable1986();
    private CombinedMortalityTable cmt = LoadData.mortalityTable1989();
    private AgeSpecificFertilityRates asfr_urban = LoadData.loadASFR(Locality.URBAN);
    private AgeSpecificFertilityRates asfr_rural = LoadData.loadASFR(Locality.RURAL);

    public BirthsDeficit() throws Exception
    {
    }

    public void eval() throws Exception
    {
        PopulationByLocality p1989 = LoadData.populationCensus1989();
        PopulationContext p = p1989.toPopulationContext();
        Map<Integer, Double> year2births = new HashMap<>();

        /*
         * передвижка от 12.1.1989 до 1.1.1990 с шагом 365-11 дней
         */
        ForwardPopulationUR fw = new ForwardPopulationUR();
        fw.setBirthRateUrban(asfr_urban);
        fw.setBirthRateRural(asfr_rural);
        fw.forward(p, cmt, (365 - 11) / 365.0);

        /* оценка числа рождений за полный календарный 1989 год */
        year2births.put(1989, fw.getObservedBirths() * 365.0 / (365 - 11));

        /*
         *  передвижка от 1.1.1990 до 1.1.2016 с шагом год
         */
        for (int year = 1990; year <= 2015; year++)
        {
            fw = new ForwardPopulationUR();
            fw.setBirthRateUrban(asfr_urban);
            fw.setBirthRateRural(asfr_rural);
            fw.forward(p, cmt, 1.0);
            year2births.put(year, fw.getObservedBirths());
        }

        Util.out("");
        Util.out("Ожидаемое число рождений (по передвижке)");
        Util.out("при сохранении возрастных коэффициентов смертности и плодовитости");
        Util.out("");
        for (int year = 1989; year <= 2015; year++)
        {
            Util.out(String.format("%4d %,9d", year, Math.round(year2births.get(year))));
        }

        // ####
        Util.noop();

        // ### сравнить с Map<Integer,Double> mb = LoadData.actualBirths(1989, 2015);
    }
}
