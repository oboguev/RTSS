package rtss.losses199x;

import java.util.HashMap;
import java.util.Map;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.projection.ForwardPopulationUR;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Locality;
import rtss.losses199x.util.ActualBirths;
import rtss.rosbris.RosBrisTerritory;
import rtss.util.Util;

public class BirthsDeficit
{
    // private CombinedMortalityTable cmt = LoadData.mortalityTable1986();
    private CombinedMortalityTable cmt = LoadData.mortalityTable1989();
    private AgeSpecificFertilityRates asfr_urban = LoadData.loadASFR(Locality.URBAN);
    private AgeSpecificFertilityRates asfr_rural = LoadData.loadASFR(Locality.RURAL);
    
    private Map<Integer, Double> actualBirths = new ActualBirths().getActualBirths(1989, 2015, RosBrisTerritory.RF_BEFORE_2014);

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
        Util.out("EXPECTED = ожидаемое число рождений");
        Util.out("ACTUAL = фактическое число рождений");
        Util.out("DFFICIT = дефицит");
        Util.out("");
        Util.out("year expected actual deficit");
        for (int year = 1989; year <= 2015; year++)
        {
            long expected = Math.round(year2births.get(year));
            long actual = Math.round(actualBirths.get(year));
            long deficit = expected - actual;
            if (deficit < 0)
                deficit = 0;
            Util.out(String.format("%4d %,9d %,9d %,9d", year, expected, actual, deficit));
        }

        Util.noop();
    }
}
