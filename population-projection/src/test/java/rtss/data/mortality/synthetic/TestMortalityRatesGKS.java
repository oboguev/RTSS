package rtss.data.mortality.synthetic;

import rtss.data.mortality.CombinedMortalityTable;
import rtss.data.population.struct.PopulationByLocality;
import rtss.data.selectors.Area;
import rtss.util.Util;

public class TestMortalityRatesGKS
{
    public static void main(String[] args)
    {
        try
        {
            new TestMortalityRatesGKS().do_main();
            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        MortalityTableGKS.UsePrecomputedFiles = false;
        MortalityTableGKS.UseCache = false;

        /* 
         * Население РСФСР по переписи 1979 года используется для hinting-а при декомпозиции
         * таблицы смертности 1978-1979 года. Для этого важны не абсолютные значения
         * численности в возрастах, а их относительное соотношение.
         */
        if (Util.True)
        {
            PopulationByLocality p = PopulationByLocality.census(Area.RSFSR, 1979);
            p = null;
            CombinedMortalityTable cmt = MortalityTableGKS.getMortalityTable(Area.RSFSR, "1978-1979", p);
        }

        /* 
         * Население РСФСР по переписи 1989 года используется для hinting-а при декомпозиции
         * таблицы смертности 1986-1987 года. Для этого важны не абсолютные значения
         * численности в возрастах, а их относительное соотношение.
         */
        if (Util.True)
        {
            PopulationByLocality p = PopulationByLocality.census(Area.RSFSR, 1989);
            p = null;
            CombinedMortalityTable cmt = MortalityTableGKS.getMortalityTable(Area.RSFSR, "1986-1987", p);
        }
    }
}
