package rtss.ww2losses.population194x;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/*
 * Вспомогательные функции для правки состава населения
 */
public abstract class AdjustPopulation
{
    protected static final int[] ADH_binning = { 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 5, 16 };

    public Population adjust(Population p) throws Exception
    {
        return p;
    }
    
    public abstract String name();

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

    /*
     * Проверить, что разбивка населения по возрастным группам (как-то 5-летней агрегации) 
     * не изменилась 
     */
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

    /* ============================================================================== */

    /* 
     * пересчитать структуру возрастных корзин так, чтобы указанные возраста имели отдельные 1-годовые корзины 
     */
    protected int[] rebin(int[] binning, List<Integer> isolateAges) throws Exception
    {
        Util.assertion(Util.sum(binning) == Population.MAX_AGE + 1);
        for (int age : isolateAges)
            binning = rebin(binning, age);

        Util.assertion(Util.sum(binning) == Population.MAX_AGE + 1);
        return binning;
    }

    private int[] rebin(int[] binning, int age)
    {
        List<Integer> list = new ArrayList<>();
        
        int x = 0;
        
        for (int w : binning)
        {
            int x1 = x;
            int x2 = x1 + w - 1;
            
            if (!(age >= x1 && age <= x2))
            {
                list.add(w);
            }
            else if (age == x1 && age == x2)
            {
                list.add(w);
            }
            else if (age == x1)
            {
                list.add(1);
                list.add(w - 1);
            }
            else if (age == x2)
            {
                list.add(w - 1);
                list.add(1);
            }
            else 
            {
                list.add(age - x1);
                list.add(1);
                list.add(x2 - age);
            }

            x += w;
        }

        return Util.toIntArray(list);
    }

    @SuppressWarnings("unused")
    private Bin[] binning2bins(final int[] binning) throws Exception
    {
        if (Util.sum(binning) != Population.MAX_AGE + 1)
            throw new IllegalArgumentException(String.format("сумма ширины корзин %d != %d", Util.sum(binning), Population.MAX_AGE + 1));

        Bin[] bins = new Bin[binning.length];
        int age = 0;

        for (int k = 0; k < binning.length; k++)
        {
            bins[k] = new Bin(age, age + binning[k] - 1, 0);
            age += binning[k];
        }

        return Bins.bins(bins);
    }

    @SuppressWarnings("unused")
    private int[] bins2binning(Bin bin0) throws Exception
    {
        /* check bins are contigious and widsth sum is correct */
        Util.assertion(bin0.age_x1 == 0);
        int width = 0;
        for (Bin bin = bin0; bin != null; bin = bin.next)
        {
            Util.assertion(bin.age_x1 >= 0 && bin.age_x2 >= 0 && bin.age_x2 >= bin.age_x1);
            width += bin.age_x2 - bin.age_x1 + 1;
            if (bin.next != null)
                Util.assertion(bin.next.age_x1 == bin.age_x2 + 1);
        }

        Util.assertion(width == Population.MAX_AGE + 1);

        /* actually make binning */
        int count = 0;
        for (Bin bin = bin0; bin != null; bin = bin.next)
            count++;

        int[] binning = new int[count];
        int k = 0;
        for (Bin bin = bin0; bin != null; bin = bin.next)
        {
            binning[k++] = bin.age_x2 - bin.age_x1 + 1;
        }

        return binning;
    }
}
