package rtss.pre1917.data.migration;

import rtss.pre1917.LoadData;
import rtss.pre1917.data.migration.ImmigrationYear.LumpImmigration;
import rtss.util.Util;

public class ImmigrationShowSum
{
    public static void main(String[] args)
    {
        try
        {
            new ImmigrationShowSum().do_main();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }

    private void do_main() throws Exception
    {
        Util.out("Иммиграция в Империю по годам: легальная, ясное размещение -- легальная, неясное размещение -- нелегальная");
        Util.out("Легальная (документированная) иммиграция в Империю по годам");
        Util.out("");

        Immigration immigration = new LoadData().loadImmigration();

        for (int year = 1881; year <= 1915; year++)
        {
            long immmigrants = immigration.legalImmigrationForYear(year);
            LumpImmigration lump = immigration.lumpImmigrationForYear(year);
            final double TurkeyFactor = (year >= 1897 && year <= 1913) ? 2.35 : 1.0;
            long legaLumpSum = lump.persia + lump.turkey + lump.china + lump.japan;
            long illegaLumpSum = Math.round(lump.turkey * (TurkeyFactor - 1));
            Util.out(String.format("%d %,d %,d %,d", year, immmigrants, legaLumpSum, illegaLumpSum));
        }
    }
}
