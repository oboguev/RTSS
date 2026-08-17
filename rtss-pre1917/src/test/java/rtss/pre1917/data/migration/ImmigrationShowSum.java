package rtss.pre1917.data.migration;

import rtss.pre1917.LoadData;
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
        Util.out("Легальная (документированная) иммиграция в Империю по годам");
        Util.out("");

        Immigration immigration = new LoadData().loadImmigration();

        for (int year = 1881; year <= 1915; year++)
        {
            long immmigrants = immigration.legalImmigrationForYear(year);
            // LumpImmigration lump = immigration.lumpImmigrationForYear(year);
            // final double TurkeyFactor = (year >= 1896) ? 2.33 : 1.0;
            // long lumpSum = lump.european + lump.persia + Math.round(lump.turkey * TurkeyFactor) + lump.china + lump.japan;
            Util.out(String.format("%d %,d", year, immmigrants));
        }
    }
}
