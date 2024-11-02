package rtss.mexico.agri.calc;

import rtss.mexico.agri.entities.CultureSet;
import rtss.mexico.agri.loader.LoadAE;
import rtss.mexico.agri.loader.LoadEH;
import rtss.mexico.agri.loader.LoadSARH;

public class MergeCultureSets
{
    private CultureSet csAE = LoadAE.load();
    private CultureSet csEH = LoadEH.load();
    private CultureSet csSARH = LoadSARH.load();
    
    public MergeCultureSets() throws Exception
    {
        CultureSet cs = csSARH.dup();
        cs.deleteYearRange(0, 1924);
        cs.deleteYearRange(1983, 9999);
        
        // ### EH 1897-1908

        // ### слить 3 набора (по 1982 год)
        // ### доля фуражного (с 1971) - ранее
        // ### слить плантаны
        // ### вычислить consumption (if null)
        // ### roll negative consumption values backwards
        // ### с ... по 1996 искл. войну
        // ### cana de azucar в EH - что с ней делать? sugar & alcohol
        // ### apply export import factor when not listed : prod -> consumption for 1927-1930  
    }
}
