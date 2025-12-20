package rtss.losses199x;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import rtss.data.ValueConstraint;
// import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.Population;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.data.selectors.LocalityGender;
import rtss.losses199x.util.PrintTable;
import rtss.rosbris.RosBrisDeathRates;
import rtss.rosbris.RosBrisPopulationExposureForDeaths;
import rtss.rosbris.RosBrisTerritory;
import rtss.util.Util;
import rtss.util.plot.PopulationChart;

public class ExcessDeaths
{
    private Map<Integer, PopulationByLocality> year2deaths_at_actual_rates = new HashMap<>();;
    private Map<Integer, PopulationByLocality> year2deaths_at_reference_rates = new HashMap<>();

    private final int yy1 = 1989;
    private final int yy2 = 2015;

    public void eval() throws Exception
    {
        year2deaths_at_actual_rates.clear();
        year2deaths_at_reference_rates.clear();

        RosBrisPopulationExposureForDeaths.use2021census(true);
        RosBrisDeathRates.use2021census(true);

        // CombinedMortalityTable cmt = LoadData.mortalityTable1986();
        // RosBrisDeathRates reference_rates = RosBrisDeathRates.from(cmt, RosBrisTerritory.RF_BEFORE_2014, 1986);
        RosBrisDeathRates reference_rates = RosBrisDeathRates.loadMX(RosBrisTerritory.RF_BEFORE_2014, 1989);

        if (Util.False)
        {
            Util.out("");
            Util.out("==== age mx_reference mx_89");
            Util.out("");
            RosBrisDeathRates rates89 = RosBrisDeathRates.loadMX(RosBrisTerritory.RF_BEFORE_2014, 1989);
            for (int age = 0; age <= 50; age++)
            {
                double mx_reference = reference_rates.mx(Locality.URBAN, Gender.MALE, age);
                double mx_89 = rates89.mx(Locality.URBAN, Gender.MALE, age);
                Util.out(String.format("%3d %.5f %.5f", age, mx_reference, mx_89));
            }
            Util.out("===================");
        }

        for (int year = yy1; year <= yy2; year++)
        {
            PopulationByLocality exposure = RosBrisPopulationExposureForDeaths.getPopulationByLocality(RosBrisTerritory.RF_BEFORE_2014, year);
            RosBrisDeathRates rates = RosBrisDeathRates.loadMX(RosBrisTerritory.RF_BEFORE_2014, year);

            PopulationByLocality d_at_actual_rates = deaths(exposure, rates);
            PopulationByLocality d_at_reference_rates = deaths(exposure, reference_rates);

            year2deaths_at_actual_rates.put(year, d_at_actual_rates);
            year2deaths_at_reference_rates.put(year, d_at_reference_rates);
        }

        /* ========================================================================== */

        print(Locality.URBAN);
        print(Locality.RURAL);
        print(Locality.TOTAL);

        /*
         * total_excess даёт результат несколько отличающийся от print
         * из-за разного учёта отрицательных величин
         */

        PopulationByLocality total_excess = null;

        for (int year = yy1; year <= yy2; year++)
        {
            PopulationByLocality d_at_actual_rates = year2deaths_at_actual_rates.get(year);
            PopulationByLocality d_at_reference_rates = year2deaths_at_reference_rates.get(year);
            PopulationByLocality excess = d_at_actual_rates.sub(d_at_reference_rates);

            if (total_excess == null)
            {
                total_excess = excess;
            }
            else
            {
                total_excess = total_excess.add(excess, ValueConstraint.NONE);
            }
        }

        total_excess.makeBoth();
        total_excess.recalcTotalLocalityFromUrbanRural();
        total_excess.validate();

        for (Locality locality : Locality.TotalUrbanRural)
        {
            Population p = total_excess.forLocality(locality);
            
            Util.out(String.format("Средний возраст избыточной смерти населения %s %s = %.1f", 
                                   locality.name(), Gender.MALE.name(),
                                   averageAge(p, Gender.MALE)));

            Util.out(String.format("Средний возраст избыточной смерти населения %s %s = %.1f", 
                                   locality.name(), Gender.FEMALE.name(),
                                   averageAge(p, Gender.FEMALE)));

            Util.out(String.format("Средний возраст избыточной смерти населения %s %s = %.1f", 
                                   locality.name(), Gender.BOTH.name(),
                                   averageAge(p, Gender.BOTH)));
            
            Util.out("");
        }
        
        Util.noop();
        
        // total_excess.forLocality(Locality.TOTAL).toPopulationContext().display("TOTAL");

        for (Locality locality : Locality.TotalUrbanRural)
        {
            final String imageExportDirectory = "C:\\@\\losses199x";
            final int IMAGE_CX = 2400;
            final int IMAGE_CY = 1200;
            final int TN_CX = IMAGE_CX / 2;
            final int TN_CY = IMAGE_CY / 2;

            String title = "Избыточные смерти населения " + locality.name();
            Population p;

            switch (locality)
            {
            case TOTAL:
            case URBAN:
                p = total_excess.forLocality(locality);
                p = p.clone().setLocality(Locality.TOTAL);
                new PopulationChart(title)
                        .show("", p)
                        .exportImage(IMAGE_CX, IMAGE_CY, imageExportDirectory + File.separator + locality + ".png")
                        .exportImage(TN_CX, TN_CY, imageExportDirectory + File.separator + locality + "_tn.png");
                break;

            case RURAL:
                /*
                 * toPopulationContext предназачена для населений с обычной структурой,
                 * а не произвольно быстро осциллирующих набооров
                 */
                p = total_excess.forLocality(locality);
                p = p.clone().setLocality(Locality.TOTAL);
                new PopulationChart(title)
                        .show("", p)
                        .exportImage(IMAGE_CX, IMAGE_CY, imageExportDirectory + File.separator + locality + ".png")
                        .exportImage(TN_CX, TN_CY, imageExportDirectory + File.separator + locality + "_tn.png");
                break;
            }
        }

        Util.noop();
    }

    /*
     * Число и струкура смертей при данной экспозиции и уровнях смертности
     */
    private PopulationByLocality deaths(PopulationByLocality exposure, RosBrisDeathRates rates) throws Exception
    {
        LocalityGender[] lgs = { LocalityGender.URBAN_MALE, LocalityGender.URBAN_FEMALE, LocalityGender.RURAL_MALE, LocalityGender.RURAL_FEMALE };

        PopulationByLocality deaths = PopulationByLocality.newPopulationByLocality();

        for (LocalityGender lg : lgs)
        {
            for (int age = 0; age <= Population.MAX_AGE; age++)
            {
                double mx = rates.mx(lg.locality, lg.gender, age);
                double pop = exposure.get(lg.locality, lg.gender, age);
                deaths.set(lg.locality, lg.gender, age, pop * mx);
            }
        }

        deaths.recalcFromUrbanRuralBasic();

        return deaths;
    }

    private void print(Locality locality) throws Exception
    {
        Util.out("");
        Util.out("Избыточные смерти для местности " + locality);
        Util.out("");

        PrintTable pt = new PrintTable(yy2 - yy1 + 1 + 3, 13);

        int nr = 0;

        {
            int nc = 0;

            pt.put(nr, nc++, "Year");
            pt.put(nr, nc++, "    ");

            pt.put(nr, nc++, "MALE");
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "    ");

            pt.put(nr, nc++, "FEMALE");
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "    ");

            pt.put(nr, nc++, "BOTH");
            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "");
        }

        {
            nr++;
            int nc = 0;

            pt.put(nr, nc++, "");
            pt.put(nr, nc++, "    ");

            pt.put(nr, nc++, "expected");
            pt.put(nr, nc++, "actual");
            pt.put(nr, nc++, "excess");
            pt.put(nr, nc++, "    ");

            pt.put(nr, nc++, "expected");
            pt.put(nr, nc++, "actual");
            pt.put(nr, nc++, "excess");
            pt.put(nr, nc++, "    ");

            pt.put(nr, nc++, "expected");
            pt.put(nr, nc++, "actual");
            pt.put(nr, nc++, "excess");
        }

        for (int year = yy1; year <= yy2; year++)
        {
            PopulationByLocality d_reference = year2deaths_at_reference_rates.get(year);
            PopulationByLocality d_actual = year2deaths_at_actual_rates.get(year);

            nr = year - yy1 + 2;
            int nc = 0;

            // year
            pt.put(nr, nc++, "" + year);
            pt.put(nr, nc++, "");

            // male: expected -- actual -- excess
            put4(pt, nr, nc, d_reference, d_actual, locality, Gender.MALE, true);
            nc += 4;

            // female: expected -- actual -- excess
            put4(pt, nr, nc, d_reference, d_actual, locality, Gender.FEMALE, true);
            nc += 4;

            // both: expected -- actual -- excess
            put4(pt, nr, nc, d_reference, d_actual, locality, Gender.BOTH, false);
        }

        pt.print();
    }

    private void put4(PrintTable pt, int nr, int nc, PopulationByLocality d_reference, PopulationByLocality d_actual, Locality locality,
            Gender gender, boolean spacer) throws Exception
    {
        double expected = d_reference.sum(locality, gender, 0, Population.MAX_AGE);
        double actual = d_actual.sum(locality, gender, 0, Population.MAX_AGE);
        double excess = actual - expected;
        if (excess < 0)
            excess = 0;

        pt.put(nr, nc++, f2s(expected));
        pt.put(nr, nc++, f2s(actual));
        pt.put(nr, nc++, f2s(excess));

        if (spacer)
            pt.put(nr, nc++, "");

    }

    private String f2s(double v)
    {
        return String.format("%,d", Math.round(v));
    }

    @SuppressWarnings("unused")
    private void neg2zero(PopulationByLocality p) throws Exception
    {
        neg2zero(p.forLocality(Locality.URBAN));
        neg2zero(p.forLocality(Locality.RURAL));
        neg2zero(p.forLocality(Locality.TOTAL));
    }

    private void neg2zero(Population p) throws Exception
    {
        neg2zero(p, Gender.MALE);
        neg2zero(p, Gender.FEMALE);
        neg2zero(p, Gender.BOTH);
    }

    private void neg2zero(Population p, Gender gender) throws Exception
    {
        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            if (p.get(gender, age) < 0)
                p.set(gender, age, 0);
        }
    }

    private double averageAge(Population p, Gender gender) throws Exception
    {
        double age_pop_sum = 0;
        double pop_sum = 0;

        for (int age = 0; age <= Population.MAX_AGE; age++)
        {
            double pop = p.get(gender, age);
            age_pop_sum += age * pop;
            pop_sum += pop;
        }

        return age_pop_sum / pop_sum;
    }
}
