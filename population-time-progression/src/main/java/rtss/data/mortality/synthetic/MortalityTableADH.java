package rtss.data.mortality.synthetic;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.mortality.MortalityInfo;
import rtss.data.mortality.SingleMortalityTable;
import rtss.data.population.Population;
import rtss.data.population.synthetic.PopulationADH;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;
import rtss.util.plot.ChartXYSplineAdvanced;

/*
 * Загрузить поло-возрастные покаатели смертности (агреггированные по возрастным группам) из файла Excel
 * и построить на их основании таблицу смертности с годовым шагом.
 */
public class MortalityTableADH
{
    public static final int MAX_AGE = CombinedMortalityTable.MAX_AGE;
    
    public static CombinedMortalityTable getMortalityTable(Area area, int year) throws Exception
    {
        return getMortalityTable(area, "" + year);
    }
    
    public static CombinedMortalityTable getMortalityTable(Area area, String year) throws Exception
    {
        // ### look in cache
        // ### try loading from resource
        
        CombinedMortalityTable cmt = CombinedMortalityTable.newEmptyTable();
        
        Bin[] male_mortality_bins = MortalityRatesFromExcel.loadRates(year, Gender.MALE, year);
        Bin[] female_mortality_bins = MortalityRatesFromExcel.loadRates(year, Gender.FEMALE, year);
        
        cmt.setTable(Locality.TOTAL, Gender.MALE, makeSingleTable(male_mortality_bins));
        cmt.setTable(Locality.TOTAL, Gender.FEMALE, makeSingleTable(female_mortality_bins));
        
        Population p = PopulationADH.getPopulation(area, year);

        Bin[] male_population_bins = p.binByAge(Gender.MALE, male_mortality_bins); 
        Bin[] female_population_bins = p.binByAge(Gender.FEMALE, female_mortality_bins); 
        
        double[] qx = new double[MAX_AGE + 1];
        for (int age = 0; age <= MAX_AGE; age++)
        {
            Bin males = Bins.binForAge(age, male_population_bins);
            Bin females = Bins.binForAge(age, female_population_bins);

            double m_fraction = males.avg / (males.avg + females.avg);
            double f_fraction = females.avg / (males.avg + females.avg);
            
            MortalityInfo mi_m = cmt.get(Locality.TOTAL, Gender.MALE, age);
            MortalityInfo mi_f = cmt.get(Locality.TOTAL, Gender.FEMALE, age);
            
            qx[age] = m_fraction * mi_m.qx + f_fraction * mi_f.qx;
        }
        
        cmt.setTable(Locality.TOTAL, Gender.BOTH, SingleMortalityTable.from_qx("computed", qx));
        cmt.comment("АДХ-РСФСР-" + year);

        // display(cmt, Locality.TOTAL, Gender.MALE);
        
        if (Util.False)
        {
            String comment = "# Таблица построена модулем " + MortalityTableADH.class.getCanonicalName() + " по данным в АДХ-Россия"; 
            cmt.saveTable("P:\\@\\zzzz", comment);
        }
        
        
        return null;
    }

    private static SingleMortalityTable makeSingleTable(Bin... bins) throws Exception
    {
        double[] curve = InterpolateAsMeanPreservingCurve.curve(bins);
        curve = Util.divide(curve, 1000);
        return SingleMortalityTable.from_qx("computed", curve);
    }

    @SuppressWarnings("unused")
    private static void display(CombinedMortalityTable cmt, Locality locality, Gender gender) throws Exception
    {
        double[] qx = cmt.getSingleTable(locality, gender).qx();
        
        Util.print(cmt.comment() + " qx", qx, 0);

        new ChartXYSplineAdvanced(cmt.comment() + " qx", "age", "mortality")
            .addSeries("qx", qx)
            .display();
    }
}
