package rtss.ww2losses.population_194x;

import rtss.data.bin.Bin;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class AdjustPopulation
{
    protected static final int[] ADH_binning = { 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 16 };

    public Population adjust(Population p) throws Exception
    {
        return p;
    }

    public PopulationByLocality adjust(PopulationByLocality p) throws Exception
    {
        Population urban = p.forLocality(Locality.URBAN);
        Population rural = p.forLocality(Locality.RURAL);
        Population total = p.forLocality(Locality.TOTAL);

        if (urban != null)
            urban = adjust(urban);

        if (rural != null)
            rural = adjust(rural);

        if (total != null)
            total = adjust(total);
        
        return new PopulationByLocality(total, urban, rural);
    }

    /*
     * Перераспределить @amount человек в возраст to_age из возрастов from_ages
     */
    protected void redistribute_to(Population p, Gender gender, int to_age, double amount, int... from_ages) throws Exception
    {
        p.add(gender, to_age, amount);

        // распределить перенос из "from" пропорционально численности в возрастах from_ages
        double from_sum = 0;
        for (int age : from_ages)
            from_sum += p.get(gender, age);

        for (int age : from_ages)
        {
            double v = p.get(gender, age);
            double share = amount * (v / from_sum);
            if (share > v)
                throw new Exception("невозможно перераспределить");
            p.sub(gender, age, share);
        }
    }

    protected void verifyBinning(final int[] binning, Population p1, Population p2) throws Exception
    {
        verifyBinning(Gender.MALE, binning, p1, p2);
        verifyBinning(Gender.FEMALE, binning, p1, p2);
        verifyBinning(Gender.BOTH, binning, p1, p2);
    }

    protected void verifyBinning(Gender gender, final int[] binning, Population p1, Population p2) throws Exception
    {
        Bin[] b1 = p1.binSumByAge(gender, binning);
        Bin[] b2 = p2.binSumByAge(gender, binning);
        Util.assertion(b1.length == b2.length);
        for (int k = 0; k < b1.length; k++)
        {
            if (!Util.same(b1[k].avg, b2[k].avg))
                throw new Exception("расклад по возрастным корзинам изменился");
        }
    }
}
