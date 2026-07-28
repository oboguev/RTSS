package rtss.pre1917.data.migration;

import rtss.pre1917.LoadData;
import rtss.util.Util;

public class MigrationTest
{
    public static void main(String[] args)
    {
        try
        {
            new MigrationTest().test_1();
        }
        catch (Exception ex)
        {
            Util.err("** Exception:");
            ex.printStackTrace();
        }
    }

    private void test_1() throws Exception
    {
        InnerMigration innerMigration = new LoadData().loadInnerMigration();
        Emigration emigration = new LoadData().loadEmigration();
        Immigration immigration = new LoadData().loadImmigration();
        TotalMigration totalMigration = TotalMigration.getTotalMigration();

        String tname = "Самаркандская обл.";
        int year = 1906;

        Long vTotal = totalMigration.saldo_nullable(tname, year);

        Long vImmigrants = immigration.immigrants(tname, year);

        Long vEmigrants = emigration.emigrants(tname, year);

        Long vInnerSaldo = innerMigration.saldo(tname, year);
        Long vInnerInflow = innerMigration.inFlow(tname, year);
        Long vInnerOutflow = innerMigration.outFlow(tname, year);
        
        Util.out(String.format("vTotal = %,d", vTotal));
        Util.out(String.format("vImmigrants = %,d", vImmigrants));
        Util.out(String.format("vEmigrants = %,d", vEmigrants));
        Util.out(String.format("vInnerSaldo = %,d", vInnerSaldo));
        Util.out(String.format("vInnerInflow = %,d", vInnerInflow));
        Util.out(String.format("vInnerOutflow = %,d", vInnerOutflow));
        
        Util.unused(vTotal, vImmigrants, vEmigrants, vInnerSaldo, vInnerInflow, vInnerOutflow);
    }
}
