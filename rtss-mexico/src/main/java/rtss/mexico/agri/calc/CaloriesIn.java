package rtss.mexico.agri.calc;

import rtss.mexico.agri.entities.ArgiConstants;
import rtss.mexico.agri.entities.CultureDefinition;
import rtss.mexico.agri.entities.CultureYear;
import rtss.mexico.agri.entities.RiceKind;
import rtss.mexico.agri.loader.LoadCultureDefinitions;

public class CaloriesIn
{
    /*
     * Калорий в годовом потреблении данной культуры
     */
    public static double in(CultureYear cy) throws Exception
    {
        if (cy.alcohol != null)
            return cy.alcohol * ArgiConstants.CaloriesPerLiterOfAlcohol;
        
        if (cy.culture.name.toLowerCase().contains("arroz"))
        {
            if (cy.rice_kind != RiceKind.WHITE)
                throw new Exception("Not a white rice");
        }
        
        CultureDefinition cd = LoadCultureDefinitions.load().get(cy.culture.name);
        
        // потребление в тоннах
        return cd.kcal_kg * cy.consumption * 1000.0;
    }
}
