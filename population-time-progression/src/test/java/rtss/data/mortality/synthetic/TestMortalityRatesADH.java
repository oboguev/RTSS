package rtss.data.mortality.synthetic;

import java.io.File;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.selectors.Area;
import rtss.data.selectors.Locality;
import rtss.util.Util;

public class TestMortalityRatesADH
{
    public static void main(String[] args)
    {
        try
        {
            new TestMortalityRatesADH().do_main();
            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        if (Util.False)
        {
            MortalityTableADH.getMortalityTable(Area.RSFSR, 1946);
            return;
            
        }

        for (int year = 1927; year <= 1958; year++)
        {
            if (year >= 1941 && year <= 1945)
                continue;

            CombinedMortalityTable cmt = MortalityTableADH.getMortalityTable(Area.RSFSR, year);

            if (Util.False)
            {
                File rootDir = new File("P:\\@@\\ADH-RSFSR-mt");
                File dir = new File(rootDir, "" + year);
                dir.mkdirs();
                String comment = String.format("Таблица построена по данным АДХ-РСФСР модулем %s", 
                                               MortalityTableADH.class.getCanonicalName());
                cmt.saveTable(dir.getAbsoluteFile().getCanonicalPath(), comment, Locality.TOTAL);
            }
        }
    }
}
