package rtss.pre1917.data.migration;

import rtss.pre1917.LoadData;

public class TotalMigration
{
    private static TotalMigration _instance;
    
    private InnerMigration innerMigration = new LoadData().loadInnerMigration();
    private Emigration emigration = new LoadData().loadEmigration();
    
    private TotalMigration() throws Exception
    {
    }
    
    public static synchronized TotalMigration getTotalMigration() throws Exception
    {
        if (_instance == null)
            _instance = new TotalMigration();

        return _instance;
    }
    
    public long saldo(String tname, int year)
    {
        return innerMigration.saldo(tname, year) - emigration.emigrants(tname, year);
    }
}
