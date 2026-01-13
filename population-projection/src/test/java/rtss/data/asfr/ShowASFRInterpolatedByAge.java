package rtss.data.asfr;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.selectors.Area;
import rtss.util.Util;

public class ShowASFRInterpolatedByAge
{
    public static void main(String[] args)
    {
        try
        {
            if (Util.True)
            {
                Util.out("Женская плодовитость по анамнестическому опросу 1960 года");
                Util.out("");
                do_main(AgeSpecificFertilityRatesByYear.load("age_specific_fertility_rates/survey-1960.xlsx"));
            }

            if (Util.False)
            {
                Util.out("Женская плодовитость для населения РСФСР по реконструкции АДХ");
                Util.out("");
                do_main(AgeSpecificFertilityRatesByYear.load(Area.RSFSR, "Total"));
            }
            
            if (Util.False)
            {
                Util.out("Женская плодовитость для населения СССР по реконструкции АДХ");
                Util.out("");
                do_main(AgeSpecificFertilityRatesByYear.load(Area.USSR));
            }
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
    
    private static void do_main(AgeSpecificFertilityRatesByYear yearly_asfrs) throws Exception
    {
        int[] display_years = { 1927, 1928, 1929, 1936, 1937, 1938, 1939 };

        for (int year : display_years)
        {
            AgeSpecificFertilityRates asfr = yearly_asfrs.getForYear(year);
            AgeSpecificFertilityRates xasfr = InterpolateASFR_ByAge.interpolate(asfr);
            xasfr.display("Женская возрастная плодовитость для " + year + " года");
            dump(xasfr, "Женская возрастная плодовитость для " + year + " года (рождений в год на 1000 женщин)");
        }
    }

    private static void dump(AgeSpecificFertilityRates asfr, String title) throws Exception
    {
        Util.out(title + "");
        Util.out("");
        Util.out("возраст плодовитость");

        Bin[] bins = asfr.binsReadonly();
        Bin firstBin = Bins.firstBin(bins);
        Bin lastBin = Bins.lastBin(bins);

        for (int age = firstBin.age_x1; age <= lastBin.age_x2; age++)
        {
            double fr = asfr.forAge(age);
            if (fr != 0)
                Util.out(String.format("%3d %10.4f", age, fr));
        }

        Util.out("");
        Util.out("===============================================");
        Util.out("");
    }
}
