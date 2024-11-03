package rtss.mexico.agri.fbs;

import java.util.Map;

import rtss.mexico.population.MexPopulationCombineEstimates;
import rtss.util.Util;
import rtss.util.excel.ExcelRow;
import rtss.util.excel.ExcelSheet;
import rtss.util.excel.ExcelWorkbook;

public class FoodBalanceSheets
{
    public static void main(String[] args)
    {
        try
        {
            new FoodBalanceSheets().show();
            Util.out("** Done");
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }

    private FoodBalanceSheets() throws Exception
    {
    }

    private Map<Integer, Long> population = MexPopulationCombineEstimates.result();

    private void show() throws Exception
    {
        org.apache.poi.util.IOUtils.setByteArrayMaxOverride(300_000_000);
        ExcelWorkbook wb = ExcelWorkbook
                .load("agriculture/FAOSTAT/FoodBalanceSheetsHistoric_E_All_Data/MexOnly - FoodBalanceSheetsHistoric_E_All_Data_NOFLAG.xlsx");
        ExcelSheet sheet = wb.getTheOnlySheet();

        ExcelRow rYear = findRow(sheet, "Population", "Total Population - Both sexes");
        ExcelRow rTotal = findRow(sheet, "Grand Total", "Food supply (kcal/capita/day)");
        ExcelRow rVegetal = findRow(sheet, "Vegetal Products", "Food supply (kcal/capita/day)");
        ExcelRow rAnimal = findRow(sheet, "Animal Products", "Food supply (kcal/capita/day)");

        Util.out("Kcal per person per day");
        Util.out("year vegetal animal total, %vegetal");
        Util.out("");

        for (int year = 1961; year <= 2013; year++)
        {
            String index = "Y" + year;
            double faoPopulation = rYear.asDouble(index) * 1000;
            double mexPopulation = (population.get(year) + population.get(year + 1)) / 2;
            double factor = faoPopulation / mexPopulation;

            double calTotal = rTotal.asDouble(index);
            double calVegetal = rVegetal.asDouble(index);
            double calAnimal = rAnimal.asDouble(index);

            calTotal *= factor;
            calVegetal *= factor;
            calAnimal *= factor;
            
            Util.out(String.format("%4d %4.0f %4.0f %4.0f %2.1f", 
                                   year, calVegetal, calAnimal, calTotal, 100.0 * calVegetal/calTotal));
        }
    }

    private ExcelRow findRow(ExcelSheet sheet, String item, String element) throws Exception
    {
        for (ExcelRow row : sheet.getRows())
        {
            if (!row.asDespacedString("Area").equals("Mexico"))
                continue;
            if (row.asDespacedString("Item").equals(item) && row.asDespacedString("Element").equals(element))
                return row;
        }

        throw new Exception("Cannot find requested row");
    }
}
