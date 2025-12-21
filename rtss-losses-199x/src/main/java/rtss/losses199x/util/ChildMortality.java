package rtss.losses199x.util;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.selectors.Gender;
import rtss.data.selectors.Locality;
import rtss.rosbris.RosBrisDeathRates;
import rtss.rosbris.RosBrisTerritory;
import rtss.util.Util;

public class ChildMortality
{
    public void print(int y1, int y2, RosBrisTerritory territory) throws Exception
    {
        RosBrisDeathRates.use2021census(true);
        
        Util.out("");
        Util.out("Детская смертность ");
        Util.out("");
        Util.out("год mx0 смертность-от-0-до-10-%");

        for (int year = y1; year <= y2; year++)
        {
            RosBrisDeathRates rates = RosBrisDeathRates.loadMX(territory, year);
            double mx0 = rates.mx(Locality.TOTAL, Gender.BOTH, 0);
            
            CombinedMortalityTable cmt = rates.toCombinedMortalityTable();
            double lx[] = cmt.getSingleTable(Locality.TOTAL, Gender.BOTH).lx();
            double m10 = 1 - lx[10] / lx[0];
            m10 *= 100.0;
            
            Util.out(String.format("%4d %f %f", year, mx0, m10));
        }
    }
}
