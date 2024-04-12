package rtss.forward_1926_193x;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.Map;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.PopulationByLocality;
import rtss.data.population.forward.PopulationForwardingContext;
import rtss.data.selectors.Area;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class ForwardPopulation_1926_1939 extends ForwardPopulation_1926
{
    public final boolean DoSmoothPopulation = true;

    private PopulationByLocality p1926 = PopulationByLocality.census(Area.USSR, 1926).smooth(DoSmoothPopulation);
    private PopulationByLocality p1939 = PopulationByLocality.census(Area.USSR, 1939).smooth(DoSmoothPopulation);;

    private Map<Integer, Double> urban_male_fraction_yyyy;
    private Map<Integer, Double> urban_female_fraction_yyyy;

    public ForwardPopulation_1926_1939() throws Exception
    {
    }

    public void forward() throws Exception
    {
        /*
         * Вычислить рождаемость городского и сельского населения в 1926 году 
         */
        calcBirthRates();

        /*
         * Вычислить оценку доли городского населения для каждого года между 1926 и 1939
         * посредством интерполяции между переписями декабря 1926 и января 1939 гг. 
         * 
         * перепись 1926 года была 1926-12-17
         * перепись 1939 года была 1939-01-17, почти в конце 1938 
         */
        double urban_female_fraction_1926 = urban_fraction(p1926, null, Gender.FEMALE);
        double urban_female_fraction_1939 = urban_fraction(p1939, null, Gender.FEMALE);

        double urban_male_fraction_1926 = urban_fraction(p1926, null, Gender.MALE);
        double urban_male_fraction_1939 = urban_fraction(p1939, null, Gender.MALE);

        urban_male_fraction_yyyy = interpolate_linear(1926, urban_male_fraction_1926, 1938,
                                                      urban_male_fraction_1939);
        urban_female_fraction_yyyy = interpolate_linear(1926, urban_female_fraction_1926, 1938,
                                                        urban_female_fraction_1939);

        /*
         * Продвижка населения для целых лет с декабря 1926 по декабрь 1938 
         */
        CombinedMortalityTable mt = mt1926;
        PopulationByLocality p = p1926;
        int year = 1926;
        double yfraction = 1.0;

        PopulationForwardingContext fctx = new PopulationForwardingContext();
        p = fctx.begin(p);

        for (;;)
        {
            year++;

            mt = interpolateMortalityTable(year);
            p = forward(p, fctx, mt, yfraction);

            /*
             * Перераспределить население между городским и сельским, отражая урбанизацию 
             */
            p = urbanize(p, fctx, Gender.MALE, urban_male_fraction_yyyy.get(year));
            p = urbanize(p, fctx, Gender.FEMALE, urban_female_fraction_yyyy.get(year));

            if (year == 1938)
                break;
        }

        /*
         * Продвижка населения для части года (с 17 декабря 1938 по 17 января 1939)
         */
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        Date d1938 = df.parse("1938-12-17");
        Date d1939 = df.parse("1939-01-17");
        long ndays = Duration.between(d1938.toInstant(), d1939.toInstant()).toDays();
        yfraction = ndays / 365.0;
        p = forward(p, fctx, mt, yfraction);

        p = fctx.end(p);

        show_results(p);
    }

    /*****************************************************************************************/

    private void show_results(PopulationByLocality p) throws Exception
    {
        /*
         * Распечатать суммарные итоги
         */
        Util.out(String
                .format("Ожидаемое население доживающеее от декабря 1926 до января 1939, в возрасте 12+ на январь 1939: %,d ",
                        Math.round(p.sum(Locality.TOTAL, Gender.BOTH, 12, MAX_AGE))));

        Util.out(String.format("Фактическое население в январе 1939 в возрасте 12 и старше: %,d",
                               Math.round(p1939.sum(Locality.TOTAL, Gender.BOTH, 12, MAX_AGE))));
        Util.out("");
        Util.out(String.format("Фактическое население в январе 1939 всех возрастов: %,d",
                               Math.round(p1939.sum(Locality.TOTAL, Gender.BOTH, 0, MAX_AGE))));

        Util.out("");
        Util.out(String.format("Ожидаемое население в январе 1939 в возрасте 0-11: %,d",
                               Math.round(p.sum(Locality.TOTAL, Gender.BOTH, 0, 11))));

        Util.out(String.format("Фактическое население в январе 1939 в возрасте 0-11: %,d",
                               Math.round(p1939.sum(Locality.TOTAL, Gender.BOTH, 0, 11))));

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
        show_shortfall(p, p1939, 0, MAX_AGE);
        show_shortfall(p, p1939, 0, 11);
        show_shortfall(p, p1939, 12, MAX_AGE);

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
            show_shortfall(p, p1939, age, age + 4);
        }
        show_shortfall(p, p1939, MAX_AGE, MAX_AGE);
    }
}
