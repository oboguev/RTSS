package rtss.losses199x;

import rtss.data.asfr.AgeSpecificFertilityRates;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.projection.ForwardPopulationUR;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.population.struct.PopulationContext;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class BirthsDeficit
{
    private CombinedMortalityTable cmt = LoadData.mortalityTable();
    private AgeSpecificFertilityRates asfr_rural = LoadData.loadASFR(Locality.RURAL);
    private AgeSpecificFertilityRates asfr_urban = LoadData.loadASFR(Locality.URBAN);

    public BirthsDeficit() throws Exception
    {
    }

    public void eval() throws Exception
    {
        PopulationByLocality p1989 = LoadData.populationCensus1989();
        PopulationContext p = p1989.toPopulationContext();

        ForwardPopulationUR fw = new ForwardPopulationUR();
        fw.setBirthRateUrban(asfr_urban);
        fw.setBirthRateRural(asfr_rural);
        fw.forward(p, cmt, (365 - 11) / 365.0);
        Util.noop();

        // ### передвижка от 12.1.1989 до 1.1.1990 с шагом 365-11
        // ### передвижка от 1.1.1990 до 1.1.2016 с шагом год
        // ### записывать числа рождений
        // ### для 1989 умножить на 365.9 / (365-11)  
        // ### сравнить с Map<Integer,Double> mb = LoadData.actualBirths(1989, 2015);
    }
}
