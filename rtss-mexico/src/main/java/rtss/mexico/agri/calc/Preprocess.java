package rtss.mexico.agri.calc;

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

    public void preprocess(CultureSet cs) throws Exception
    {
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
        
        // remove culture with no year data
        for (Culture c : cs.cultures())
        {
            if (c.years().size() == 0)
                cs.remove(c);
        }

        // remove cultures with no calories (фуражные и стимуляторы -- кофе)
        for (Culture c : cs.cultures())
        {
            CultureDefinition cd = cds.get(c.name);
            if (cd.kcal_kg == null || cd.kcal_kg == 0)
                cs.remove(c);
        }

        Util.noop();

        // ### roll negative consumption values backwards
        // ### cana de azucar в EH - что с ней делать? sugar & alcohol
        // ### apply export import factor when not listed : prod -> consumption for 1927-1930   
    }
    
    private double denull(Double d)
    {
        return d == null ? 0 : d;
    }
}
