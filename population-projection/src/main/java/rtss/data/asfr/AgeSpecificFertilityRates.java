package rtss.data.asfr;

import java.util.ArrayList;
import java.util.List;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

/**
 * Возрастные коэффициенты женской плодовитости
 * (годовое количество рождений на 1000 женщин данной возрастной группы)
 */
public class AgeSpecificFertilityRates
{
    private Bin[] bins;
    
    public AgeSpecificFertilityRates(Bin... bins) throws Exception
    {
        this.bins = Bins.clone(bins); 
    }

    public AgeSpecificFertilityRates(List<Bin> bins) throws Exception
    {
        this.bins = Bins.clone(Bins.bins(bins)); 
    }
    
    public Bin[] binsReadonly() throws Exception
    {
        return Bins.clone(bins);
    }
    
    public Bin[] binsWritable() throws Exception
    {
        return bins;
    }

    public double forAge(int age)
    {
        for (Bin bin : bins)
        {
            if (age >= bin.age_x1 && age <= bin.age_x2)
                return bin.avg;
        }

        return 0;
    }
    
    /*
     * список возрастных корзин
     */
    public List<String> ageGroups()
    {
        List<String> list = new ArrayList<>();
        for (Bin bin : bins)
            list.add(String.format("%d-%d", bin.age_x1, bin.age_x2));
        return list;
    }
    
    public double forAgeGroup(String ageGroup) throws Exception
    {
        for (Bin bin : bins)
        {
            String ag = String.format("%d-%d", bin.age_x1, bin.age_x2);
            if (ag.equals(ageGroup))
                return bin.avg;
        }

        throw new Exception("ASFR does not have age group " + ageGroup);
    }

    /*
     * Годовое количество рождений в данном населении
     */
    public double births(PopulationByLocality p) throws Exception
    {
        return births(p.forLocality(Locality.TOTAL));
    }

    public double births(Population p) throws Exception
    {
        double sum = 0;
        
        for (int age = Bins.firstBin(bins).age_x1; age <= Bins.lastBin(bins).age_x2; age++)
        {
            sum += p.get(Gender.FEMALE, age) * forAge(age) / 1000;
        }
        
        return sum;
    }
    
    /*
     * Рождаемость в данном населении (нормированная на оба пола)
     */
    public double birthRate(PopulationByLocality p) throws Exception
    {
        return birthRate(p.forLocality(Locality.TOTAL));
    }

    public double birthRate(Population p) throws Exception
    {
        return 1000 * births(p) / p.sum(Gender.BOTH, 0, Population.MAX_AGE);
    }
    
    /*
     * Увеличить или уменьшить возрастные коэффициенты рождаемости пропорциональным образом 
     * (сохраняя их соотношение) так, чтобы итоговая рождаемость в данном населении составила @cbr.
     * 
     * Первоначальная структура не изменяется, возвращается новая структура.
     */
    public AgeSpecificFertilityRates rescaleToBirthRate(PopulationByLocality p, double cbr) throws Exception
    {
        return rescaleToBirthRate(p.forLocality(Locality.TOTAL), cbr);
    }

    public AgeSpecificFertilityRates rescaleToBirthRate(Population p, double cbr) throws Exception
    {
        double scale = cbr / birthRate(p);
        
        List<Bin> list = new ArrayList<>();
        for (Bin bin : bins)
        {
            bin = new Bin(bin);
            bin.avg *= scale;
            list.add(bin);
        }
        
        return new AgeSpecificFertilityRates(Bins.bins(list));
    }
    
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (Bin bin : bins)
            sb.append(String.format("%d-%d %6.1f" + Util.nl, bin.age_x1, bin.age_x2, bin.avg));
        return sb.toString();
    }
}
