package rtss.pre1917.data.migration;

import rtss.pre1917.LoadData;

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
        return innerMigration.saldo(tname, year)
               + immigration.immigrants(tname, year)
               - emigration.emigrants(tname, year);

    }
}
