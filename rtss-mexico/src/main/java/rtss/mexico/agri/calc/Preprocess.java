package rtss.mexico.agri.calc;

import rtss.mexico.agri.entities.ArgiConstants;
import rtss.mexico.agri.entities.Culture;
import rtss.mexico.agri.entities.CultureDefinition;
import rtss.mexico.agri.entities.CultureSet;
import rtss.mexico.agri.entities.CultureYear;
import rtss.mexico.agri.loader.CultureDefinitions;
import rtss.mexico.agri.loader.LoadCultureDefinitions;
import rtss.util.Util;

public class Preprocess
{
    private CultureDefinitions cds = LoadCultureDefinitions.load();
    
    public Preprocess() throws Exception
    {
    }
    
    private CultureSet cs;

    public void preprocess(CultureSet cs) throws Exception
    {
        this.cs = cs;
        
        /*
         * Вычислить потребление (там, где оно ещё не вычислено)
         */
        for (Culture c : cs.cultures())
        {
            for (CultureYear cy : c.cultureYears())
            {
                if (cy.consumption == null)
                    cy.consumption = cy.production + denull(cy.importAmount) - denull(cy.exportAmount); 
            }
        }
        
        /* ========================================================================================== */
        
        /*
         * Согласно набору SARH, в 1971-1982 гг. в среднем 64% урожаев бараньего гороха (Garbanzo) шло на фуражные цели, 
         * хотя эта величина сильно изменялась год от года между 38 и 88%. Мы предположим, что и в предшествующие годы 
         * та же доля (64%) шла на фураж, оставляя на пищевое потребление 36% урожая бараньего гороха. 
         */
        Culture c1 = cs.get("Garbanzo Grano");
        Culture c2 = cs.get("Garbanzo Para Consumo Humano");
        for (int year : c1.years())
        {
            if (c2.cultureYear(year) != null)
                break;
            CultureYear cy = c2.dupYear(c1.cultureYear(year));
            double fodder = cy.production * 0.64;
            cy.production -= fodder;
            cy.consumption -= fodder;
            cy.perCapita = null;
        }
        
        // оставить только Garbanzo Para Consumo Humano
        cs.remove(c1);
        
        /* ========================================================================================== */
        
        // remove cultures with no year data
        for (Culture c : cs.cultures())
        {
            if (c.years().size() == 0)
                cs.remove(c);
        }

        // remove cultures with no calories (фуражные и стимуляторы -- кофе)
        // кроме сахарного тростника
        String sSugarCane = cds.get("sugar cane").name;
        for (Culture c : cs.cultures())
        {
            CultureDefinition cd = cds.get(c.name);
            if (c.name.equals(sSugarCane))
                continue;
            if (cd.kcal_kg == null || cd.kcal_kg == 0)
                cs.remove(c);
        }

        /* ========================================================================================== */

        /*
         * Сахарный тростник употреблялся для производства сахара и алкоголя. 
         * В 1928-1938 гг. средний весовой выход сахара был 6.70% от урожая тростника (с годовыми колебаниями от 6.0 до 7.2%), 
         * а алкоголя 0.57% от урожая тростника (колебания от 0.40 до 0.77%). 
         * Мы прилагаем эти переводные коэфициенты для более ранних лет, в которые производство сахара и алкоголя 
         * не отражено непосредственными сведениями, и имеются только сведения об урожае тростника.
         */
        Culture cSugar = cs.get("sugar");
        Culture cSugarCane = cs.get("sugar cane");
        
        for (CultureYear cy: cSugarCane.cultureYears())
        {
            if (cy.alcohol != null)
                break;
            cy.alcohol = cy.production * ArgiConstants.SugarCaneToAlcohol;
        }
        
        for (CultureYear cy: cSugarCane.cultureYears())
        {
            if (cSugar.cultureYear(cy.year) != null)
                break;
            CultureYear cySugar = cSugar.makeCultureYear(cy.year);
            cySugar.consumption = cySugar.production = cy.production * ArgiConstants.SugarCaneToSugar;
        }

        /* ========================================================================================== */
        
        /*
         * Приближение данных о внешней тороговле для периода 1897-1908 гг. для лет и позиций,
         * по которым не имеется сведений 
         */
        approximateEarlyExport("jitomate", 45.0);
        approximateEarlyExport("frijol", 2.5);
        approximateEarlyExport("chile verde", 12.0);
        approximateEarlyExport("arroz", 8.0);

        approximateEarlyImport("trigo", 12.0);
        approximateEarlyImport("maize", 0.8);
        
        Util.noop();

        // ### roll negative consumption values backwards
        // ### apply export import factor when not listed : prod -> consumption for 1927-1930   
    }
    
    private void approximateEarlyExport(String cname, double pct) throws Exception
    {
        Culture c = cs.get(cname);

        for (CultureYear cy : c.cultureYears())
        {
            if (cy.year >= 1909)
                break;
            if (cy.exportAmount != null || cy.importAmount != null)
                continue;
            double amount = cy.production * pct / 100.0;
            cy.exportAmount = amount;
            cy.consumption -= amount;
            cy.perCapita = null;
        }
    }
    
    private void approximateEarlyImport(String cname, double pct) throws Exception
    {
        Culture c = cs.get(cname);

        for (CultureYear cy : c.cultureYears())
        {
            if (cy.year >= 1909)
                break;
            if (cy.exportAmount != null || cy.importAmount != null)
                continue;
            double amount = cy.production * pct / 100.0;
            cy.importAmount = amount;
            cy.consumption += amount;
            cy.perCapita = null;
        }
    }
    
    private double denull(Double d)
    {
        return d == null ? 0 : d;
    }
}
