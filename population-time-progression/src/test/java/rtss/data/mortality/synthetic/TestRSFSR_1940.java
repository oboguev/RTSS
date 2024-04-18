package rtss.data.mortality.synthetic;

import rtss.data.mortality.CombinedMortalityTable;

public class TestRSFSR_1940
{
    public static void main(String[] args)
    {
        try
        {
            CombinedMortalityTable mt = new RSFSR_1940();

            String outputDir = "P:\\@@\\RSFSR_1940"; 
            String comment = String.format("Таблица построена модулем %s по данным в АДХ-Россия", RSFSR_1940.class.getCanonicalName());
            
            mt.saveTable(outputDir, comment);
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
}
