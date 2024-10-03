package rtss.pre1917.tools;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.InnerMigration;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;

public class RSFSR_1991_MigrationBalance
{
    private final InnerMigration innerMigration = new LoadData().loadInnerMigration();
    private final TerritoryDataSet tds = new LoadData().loadUGVI(LoadOptions.DONT_VERIFY);

    public static void main(String[] args)
    {
        try
        {
            new RSFSR_1991_MigrationBalance().calc();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    public RSFSR_1991_MigrationBalance() throws Exception
    {
    }

    private void calc() throws Exception
    {
        Util.out("Миграционное сальдо РСФСР-1991 и других областей");
        Util.out("");

        for (int year = 1896; year <= 1916; year++)
        {
            double rsfsr_saldo = 0; 
            double other_saldo = 0; 
            
            Taxon tx = Taxon.of("РСФСР-1991", year, tds);
            tx = tx.flatten(tds, year);
            
            for (String tname : tds.keySet())
            {
                Double rsfsr_weight = tx.territories.get(tname);
                if (rsfsr_weight == null)
                    rsfsr_weight = 0.0;
                long saldo = innerMigration.saldo(tname, year);
                rsfsr_saldo += saldo * rsfsr_weight;
                other_saldo += saldo * (1.0 - rsfsr_weight);
            }
            
            Util.out(String.format("%d %,d %,d", year, Math.round(rsfsr_saldo), Math.round(other_saldo)));
        }
    }
}
