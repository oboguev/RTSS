package rtss.pre1917.calc;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.pre1917.data.TerritoryYear;
import rtss.pre1917.eval.MergeTaxon;
import rtss.pre1917.eval.MergeTaxon.WhichYears;
import rtss.util.Util;

/*
 * Вычислить расхождение между публиковавшимся ЦСК естественным приростом по 50 губерниям
 * и годовым изменением численности их населения 
 */
public class EvalDivergence_CSK_50
{
    public static void main(String[] args)
    {
        try
        {
            new EvalDivergence_CSK_50().calc();
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }
    
    @SuppressWarnings("unused")
    private void calc() throws Exception
    {
        TerritoryDataSet tdsPOP = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        TerritoryDataSet tdsEP = new LoadData().loadEvroChast(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        
        Territory tmPOP50 = MergeTaxon.mergeTaxon(tdsPOP, "50 губерний Европейской России", WhichYears.AllSetYears);
        Territory tmPOP_EMPIRE = MergeTaxon.mergeTaxon(tdsPOP, "Империя", WhichYears.AllSetYears);
        Territory tmEP50 = MergeTaxon.mergeTaxon(tdsEP, "50 губерний Европейской России", WhichYears.AllSetYears);
        
        for (int year = 1904; year <= 1914; year++)
        {
            TerritoryYear tyPOP50 = tmPOP50.territoryYear(year);
            TerritoryYear tyPOP50_next = tmPOP50.territoryYear(year + 1);
            long incrPOP50 = tyPOP50_next.population.total.both - tyPOP50.population.total.both; 

            TerritoryYear tyPOP_EMPIRE = tmPOP_EMPIRE.territoryYear(year);
            TerritoryYear tyPOP_EMPIRE_next = tmPOP_EMPIRE.territoryYear(year + 1);
            long incrPOP_EMPIRE = tyPOP_EMPIRE_next.population.total.both - tyPOP_EMPIRE.population.total.both; 
            
            TerritoryYear tyEP50 = tmEP50.territoryYear(year);
            long incrEP50 = tyEP50.births.total.both - tyEP50.deaths.total.both;
            
            Util.out(String.format("%d %,d %,d %,d", year, incrEP50, incrPOP50, incrPOP_EMPIRE));
        }
    }

    @SuppressWarnings("unused")
    private void calc_2() throws Exception
    {
        TerritoryDataSet tdsPOP = new LoadData().loadEzhegodnikRossii(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        TerritoryDataSet tdsEP = new LoadData().loadEvroChast(LoadOptions.DONT_VERIFY, LoadOptions.MERGE_CITIES);
        
        Territory tmPOP50 = MergeTaxon.mergeTaxon(tdsPOP, "50 губерний Европейской России", WhichYears.AllSetYears);
        Territory tmPOP_EMPIRE = MergeTaxon.mergeTaxon(tdsPOP, "Империя", WhichYears.AllSetYears);
        Territory tmEP50 = MergeTaxon.mergeTaxon(tdsEP, "50 губерний Европейской России", WhichYears.AllSetYears);
        
        for (int year = 1904; year <= 1915; year++)
        {
            TerritoryYear tyPOP50 = tmPOP50.territoryYear(year);
            
            Util.out(String.format("%d %,d", year, tyPOP50.population.total.both));
        }
    }
}
