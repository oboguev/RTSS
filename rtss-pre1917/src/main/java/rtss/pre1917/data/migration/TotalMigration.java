package rtss.pre1917.data.migration;

import rtss.pre1917.LoadData;
import rtss.pre1917.data.DemographicConstants;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.eval.regions.Chernomor;

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
    private TerritoryDataSet tdsFinland = new LoadData().loadFinland();
    // private Properties p = Util.loadProperties("pre1917.props");

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
        if (Taxon.isFinland(tname))
        {
            if (year >= 1917)
                return null;
            
            Territory t = tdsFinland.get(tname);
            TerritoryYear ty = t.territoryYearOrNull(year); 
            TerritoryYear ty2 = t.territoryYearOrNull(year + 1); 
            long pop_delta = ty2.population.total.both - ty.population.total.both;
            long ngrowth = ty.births.total.both - ty.deaths.total.both;
            long migr = pop_delta - ngrowth;
            return migr;
        }
        
        Long v = null;

        v = innerMigration.saldo(tname, year) + immigration.immigrants(tname, year) - emigration.emigrants(tname, year);

        /*
         * Черноморская губерния образована начиная с 1896 года из Черноморской области входившей в состав Кубанской области.
         * Этот административный откол расчётно равносилен миграции части населения из Кубанской области.
         * Записи населения Черноморской губ. возникают в 1896 году, поэтому out-миграция из Кубанской области 
         * должна быть отнесена на 1895, так чтобы в начале 1895 в ней имелся полынй объём населения,
         * а в начале 1896 уже умееньшенный.   
         */
        if (tname.equals("Кубанская обл.") && year == 1895)
            v -= Chernomor.population_1896();

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
    
    public void fillMigration(TerritoryYear ty) throws Exception
    {
        final String tname = ty.territory.name;
        final int year = ty.year;
        
        if (Taxon.isFinland(tname) && year == 1917)
            return;
        
        if (!(year >= 1881 && year <= 1916))
            throw new IllegalArgumentException();
        
        ty.migration.total.both = saldo_nullable(tname, year);
        ty.emigration.total.both = emigration.emigrants(tname, year);
        ty.immigration.total.both = immigration.immigrants(tname, year);
        ty.inner_migration.total.both = innerMigration.saldo(tname, year);
        
        /*
         * Черноморская губерния образована начиная с 1896 года из Черноморской области входившей в состав Кубанской области.
         * Этот административный откол расчётно равносилен миграции части населения из Кубанской области.
         * Записи населения Черноморской губ. возникают в 1896 году, поэтому out-миграция из Кубанской области 
         * должна быть отнесена на 1895, так чтобы в начале 1895 в ней имелся полынй объём населения,
         * а в начале 1896 уже умееньшенный.   
         */
        if (tname.equals("Кубанская обл.") && year == 1895)
            ty.inner_migration.total.both -= Chernomor.population_1896();
        
        /* 
         * В 1894 году территория Варшавской губернии существенно увеличилась. К ней были присоединены два уезда: 
         * Плонский уезд (переданный из соседней Плоцкой губернии) 
         * и Пултусский уезд (переданный из Ломжинской губернии). 
         */
        if (year == 1894)
        {
            if (tname.equals("Ломжинская"))
            {
                ty.inner_migration.total.both -= DemographicConstants.population_Ломжинская_Пултусский_уезд_1894;
            }
            else if (tname.equals("Плоцкая"))
            {
                ty.inner_migration.total.both -= DemographicConstants.population_Плоцкая_Плонский_уезд_1894;
            }
            else if (tname.equals("Варшавская") || tname.equals("Варшавская с Варшавой"))
            {
                ty.inner_migration.total.both += DemographicConstants.population_Ломжинская_Пултусский_уезд_1894 + DemographicConstants.population_Плоцкая_Плонский_уезд_1894;
            }
        }
    }
    
    /*
     * Компенсировать в результатах слияния tdsTaxon фиктивную миграцию
     * из-за разделения территорий. 
     */
    public void compensateFictiousMigration(Territory tMergedTaxon, TerritoryDataSet tdsTaxon) throws Exception
    {
        if (tdsTaxon.containsKey("Кубанская обл."))
        {
            TerritoryYear ty = tMergedTaxon.territoryYearOrNull(1895);
            if (ty != null)
            {
                long split = Chernomor.population_1896();
                ty.migration.total.both += split;
                ty.inner_migration.total.both += split;
            }
        }
    }
}
