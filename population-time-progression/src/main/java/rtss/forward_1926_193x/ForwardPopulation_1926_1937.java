package rtss.forward_1926_193x;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class ForwardPopulation_1926_1937 extends ForwardPopulation_1926
{
    public final boolean DoSmoothPopulation = true;

    private PopulationByLocality p1926 = PopulationByLocality.census(Area.USSR, 1926).smooth(DoSmoothPopulation);
    private PopulationByLocality p1937_original = PopulationByLocality.census(Area.USSR, 1937).smooth(DoSmoothPopulation);;
    private PopulationByLocality p1937 = new Adjust_1937().adjust(p1937_original);

    private Map<Integer, Double> urban_male_fraction_yyyy;
    private Map<Integer, Double> urban_female_fraction_yyyy;

    public ForwardPopulation_1926_1937() throws Exception
    {
    }

    public void forward(boolean interpolateMortalityTable) throws Exception
    {
        /*
         * Вычислить рождаемость городского и сельского населения в 1926 году 
         */
        calcBirthRates();
        
        /*
         * Вычислить оценку доли городского населения для каждого года между 1926 и 1936
         * посредством интерполяции между переписями декабря 1926 и января 1937 гг. 
         * 
         * перепись 1926 года была 1926-12-17
         * перепись 1937 года была 1937-01-06, почти в конце 1936 
         */
        double urban_female_fraction_1926 = urban_fraction(p1926, Gender.FEMALE);
        double urban_female_fraction_1936 = urban_fraction(p1937, Gender.FEMALE);

        double urban_male_fraction_1926 = urban_fraction(p1926, Gender.MALE);
        double urban_male_fraction_1936 = urban_fraction(p1937, Gender.MALE);

        urban_male_fraction_yyyy = interpolate_linear(1926, urban_male_fraction_1926, 1936,
                                                      urban_male_fraction_1936);
        urban_female_fraction_yyyy = interpolate_linear(1926, urban_female_fraction_1926, 1936,
                                                        urban_female_fraction_1936);

        /*
         * Продвижка населения для целых лет с декабря 1926 по декабрь 1936 
         */
        CombinedMortalityTable mt = mt1926; 
        PopulationByLocality p = p1926;
        int year = 1926;
        double yfraction = 1.0;
        for (;;)
        {
            year++;
            
            if (interpolateMortalityTable)
                mt = interpolateMortalityTable(year);
            
            p = forward(p, mt, yfraction);

            /*
             * Перераспределить население между городским и сельским, отражая урбанизацию 
             */
            p = urbanize(p, Gender.MALE, urban_male_fraction_yyyy.get(year));
            p = urbanize(p, Gender.FEMALE, urban_female_fraction_yyyy.get(year));

            if (year == 1936)
                break;
        }

        /*
         * Продвижка населения для части года (с 17 декабря 1926 по 6 января 1937)
         */
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date d1936 = df.parse("1936-12-17");
        Date d1937 = df.parse("1937-01-06");
        long ndays = Duration.between(d1936.toInstant(), d1937.toInstant()).toDays();
        yfraction = ndays / 365.0;
        p = forward(p, mt, yfraction);

        show_results(p);
    }

    /*****************************************************************************************/

    private void show_results(PopulationByLocality p) throws Exception
    {
        /*
         * Распечатать суммарные итоги
         */
        Util.out(String
                .format("Ожидаемое население доживающеее от декабря 1926 до января 1937, в возрасте 10+ на январь 1937: %,d ",
                        Math.round(p.sum(Locality.TOTAL, Gender.BOTH, 10, MAX_AGE))));

        Util.out(String.format("Фактическое население в январе 1937 в возрасте 10 и старше: %,d",
                               Math.round(p1937.sum(Locality.TOTAL, Gender.BOTH, 10, MAX_AGE))));
        Util.out("");
        Util.out(String.format("Фактическое население в январе 1937 всех возрастов: %,d",
                               Math.round(p1937.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE))));

        Util.out("");
        Util.out(String.format("Ожидаемое население в январе 1937 в возрасте 0-9: %,d",
                               Math.round(p.sum(Locality.TOTAL, Gender.BOTH, 0, 9))));

        Util.out(String.format("Фактическое население в январе 1937 в возрасте 0-9: %,d",
                               Math.round(p1937.sum(Locality.TOTAL, Gender.BOTH, 0, 9))));

        /*
         * Display overall shortall
         */
        String divider = "............................................................................................................";
        divider += divider;
        Util.out("");
        Util.out(divider);
        Util.out("");
        Util.out("Общий дефицит населения (%-ты указаны относительно ожидаемого населения):");
        Util.out("");
        show_shortfall_header();
        show_shortfall(p, p1937, 0, MAX_AGE);
        show_shortfall(p, p1937, 0, 9);
        show_shortfall(p, p1937, 10, MAX_AGE);

        /*
         * Display shortall by age groups
         */
        Util.out("");
        Util.out(divider);
        Util.out("");
        Util.out("Дефицит населения по возрастным группам:");
        Util.out("");
        show_shortfall_header();
        int lc = 0;
        for (int age = 0; age + 5 <= MAX_AGE; age += 5)
        {
            if (lc++ == 4)
            {
                Util.out("");
                lc = 1;
            }
            show_shortfall(p, p1937, age, age + 4);
        }
        show_shortfall(p, p1937, MAX_AGE, MAX_AGE);
    }
}
