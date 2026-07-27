package rtss.pre1917.data.migration;

import java.util.Properties;

import rtss.pre1917.LoadData;
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
        
        return v;
    }
    
    public Long saldo_nullable(String tname, int year) throws Exception
    {
        if (year < 1896)
        {
            // ###
            return null;
        }
        
        return innerMigration.saldo(tname, year)
               + immigration.immigrants(tname, year)
               - emigration.emigrants(tname, year);

    }
}
