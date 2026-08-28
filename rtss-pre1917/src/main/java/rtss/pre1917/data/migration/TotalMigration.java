package rtss.pre1917.data.migration;

import java.util.Properties;

import rtss.pre1917.LoadData;
import rtss.pre1917.data.DemographicConstants;
import rtss.util.Util;

/*
 * Сумимарное механическое движение (внутри страны + эмиграция + иммиграция)
 * по губерниям и годам.
 * 
 * Части иммиграции не включены и должны прилагаться непосредственно к крупным таксонам.
 */
public class TotalMigration
{
    private static TotalMigration _instance;

    private InnerMigration innerMigration = new LoadData().loadInnerMigration();
    private Emigration emigration = new LoadData().loadEmigration();
    private Immigration immigration = new LoadData().loadImmigration();
    private Properties p = Util.loadProperties("pre1917.props");

    private TotalMigration() throws Exception
    {
    }

    public static synchronized TotalMigration getTotalMigration() throws Exception
    {
        if (_instance == null)
            _instance = new TotalMigration();

        return _instance;
    }

    public long saldo(String tname, int year) throws Exception
    {
        Long v = saldo_nullable(tname, year);
        if (v == null)
            v = 0L;
        return v;
    }

    public Long saldo_nullable(String tname, int year) throws Exception
    {
        Long v = null;

        v = innerMigration.saldo(tname, year) + immigration.immigrants(tname, year) - emigration.emigrants(tname, year);

        /*
         * Черноморская губерния образована начиная с 1896 года из Черноморской области входившей в состав Кубанской области.
         * Этот административный откол расчётно равносилен миграции части населения из Кубанской области.
         */
        if (tname.equals("Кубанская обл.") && year == 1895)
        {
            String s = p.getProperty("Черноморская.1896");
            s = Util.despace(s).replace(",", "");
            long split = Long.parseLong(s);
            v -= split;
        }

        /* 
         * В 1894 году территория Варшавской губернии существенно увеличилась. К ней были присоединены два уезда: 
         * Плонский уезд (переданный из соседней Плоцкой губернии) 
         * и Пултусский уезд (переданный из Ломжинской губернии). 
         */
        if (year == 1894)
        {
            if (tname.equals("Ломжинская"))
            {
                v -= DemographicConstants.population_Ломжинская_Пултусский_уезд_1894;
            }
            else if (tname.equals("Плоцкая"))
            {
                v -= DemographicConstants.population_Плоцкая_Плонский_уезд_1894;
            }
            else if (tname.equals("Варшавская") || tname.equals("Варшавская с Варшавой"))
            {
                v += DemographicConstants.population_Ломжинская_Пултусский_уезд_1894 + DemographicConstants.population_Плоцкая_Плонский_уезд_1894;
            }
        }

        if (v == 0)
            v = null;

        return v;
    }
}
