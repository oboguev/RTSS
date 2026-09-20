package rtss.pre1917.export;

import java.io.File;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.math.algorithms.MathUtil;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;
import rtss.util.excel.Excel;

public class ExportCharts
{
    private static final String OUT_PATH = "c:\\@\\rtss-pre1917\\charts";

    private final TerritoryDataSet tdsElementaryPopulation;
    private final TerritoryDataSet tdsCompositeTaxonsPopulation;
    private final TerritoryDataSet tdsCompositeTaxonsVitalRates;

    private final int startYear = 1881;

    private String currentTname;

    public ExportCharts(TerritoryDataSet tdsElementaryPopulation, TerritoryDataSet tdsCompositeTaxonsPopulation,
            TerritoryDataSet tdsCompositeTaxonsVitalRates)
    {
        this.tdsElementaryPopulation = tdsElementaryPopulation;
        this.tdsCompositeTaxonsPopulation = tdsCompositeTaxonsPopulation;
        this.tdsCompositeTaxonsVitalRates = tdsCompositeTaxonsVitalRates;
    }

    public void export() throws Exception
    {
        for (String tname : Util.sort(tdsElementaryPopulation.keySet()))
        {
            Territory t = tdsElementaryPopulation.get(tname);
            exportElementaryTerritory(tname, t);
        }

        if (tdsCompositeTaxonsPopulation.keySet().size() != tdsCompositeTaxonsVitalRates.keySet().size())
            throw new IllegalArgumentException();

        for (String tname : Util.sort(tdsCompositeTaxonsPopulation.keySet()))
        {
            Territory tPopulation = tdsCompositeTaxonsPopulation.get(tname);
            Territory tVitalRates = tdsCompositeTaxonsVitalRates.get(tname);
            exportTaxon(tname, tPopulation, tVitalRates);
        }
    }

    /* =========================================================================================== */

    private void exportElementaryTerritory(String tname, Territory t) throws Exception
    {
        currentTname = tname;
        int maxYear = t.maxYear(-1);
        // Util.out(String.format("exportElementary: %s %d", tname, maxYear));

        if (tname.equals("Выборгская"))
        {
            if (maxYear < 1915)
                throw new IllegalArgumentException();
            // последний год со сведениями о движении
            maxYear = 1914;
        }
        else if (Taxon.isPoland(tname))
        {
            if (maxYear < 1914)
                throw new IllegalArgumentException();
            // последний год со сведениями о движении
            maxYear = 1913;
        }
        else
        {
            if (maxYear < 1915)
                throw new IllegalArgumentException();
            // последний год со сведениями о движении
            maxYear = 1914;
        }

        boolean calcGrowth = false;
        switch (tname)
        {
        case Taxon.Астраханская_кочевники:
        case "Самаркандская обл.":
            // calc прирост (2 колонки) и еп
            calcGrowth = true;
            break;
        }

        String template = "excel-templates/elementary-territory-1881-" + maxYear + ".xlsx";
        XSSFWorkbook wb = Excel.loadWorkbook(template);
        XSSFSheet sheet = wb.getSheet("data");
        setText(sheet, 0, 0, tname);

        if (t.hasValidVitalRate && t.name.equals("Сахалин"))
        {
            setText(sheet, 1, 0, "Территория включена в учёт естественного движения, но после 1902 верны только числа рождений и смертей, а не данные о численности населения, рождаемости и смертности");
        }
        else if (t.hasValidVitalRate)
        {
            setText(sheet, 1, 0, "Территория включена в учёт естественного движения");
        }
        else
        {
            setText(sheet, 1, 0, "Территория НЕ включена в учёт естественного движения");
        }

        for (int year = startYear; year <= maxYear + 1; year++)
        {
            int nr = (year - 1881) + (6 - 1);
            long popm;
            final Double nullDouble = null;
            final Long nullLong = null;

            if (t.territoryYearOrNull(year) == null)
            {
                if (tname.equals("Черноморская") && year < 1896)
                {
                    // allow missing year
                }
                else
                {
                    throw new IllegalArgumentException();
                }

                setNumber(sheet, nr, 1, nullLong);
                setNumber(sheet, nr, 2, nullLong);
                setNumber(sheet, nr, 3, nullLong);
                setNumber(sheet, nr, 4, nullLong);
                setNumber(sheet, nr, 5, nullLong);
                setNumber(sheet, nr, 6, nullDouble);
                setNumber(sheet, nr, 7, nullDouble);
                continue;
            }

            Long pop = t.territoryYearOrNull(year).progressive_population.total.both;
            setNumber(sheet, nr, 1, pop);
            if (year <= maxYear)
            {
                /* next year start */
                Long pop2 = t.territoryYearOrNull(year + 1).progressive_population.total.both;
                /* mid-year */
                popm = MathUtil.log_average(pop, pop2);
                setNumber(sheet, nr, 2, popm);

                Long births = t.territoryYearOrNull(year).births.total.both;
                Long deaths = t.territoryYearOrNull(year).deaths.total.both;
                Long migr = t.territoryYearOrNull(year).migration.total.both;
                Long emigr = t.territoryYearOrNull(year).emigration.total.both;
                Long immigr = t.territoryYearOrNull(year).immigration.total.both;
                Long inner_migr = t.territoryYearOrNull(year).inner_migration.total.both;

                setNumber(sheet, nr, 3, births);
                setNumber(sheet, nr, 4, deaths);
                setNumber(sheet, nr, 5, migr);

                Double cbr = births == null ? null : (1000.0 * births) / popm;
                Double cdr = deaths == null ? null : (1000.0 * deaths) / popm;

                setNumber(sheet, nr, 6, round(cbr, 3));
                setNumber(sheet, nr, 7, round(cdr, 3));

                setNumber(sheet, nr, 9, emigr);
                setNumber(sheet, nr, 10, immigr);
                setNumber(sheet, nr, 11, inner_migr);

                if (calcGrowth)
                {
                    Long pop_total_incr = pop2 - pop;
                    Long pop_natural_incr = pop_total_incr;
                    if (migr != null)
                        pop_natural_incr -= migr;

                    setNumber(sheet, nr, 13, pop_natural_incr);
                    setNumber(sheet, nr, 14, pop_total_incr);
                    
                    Double ngr = (1000.0 * pop_natural_incr) / popm;
                    setNumber(sheet, nr, 8, round(ngr, 3));
                }
            }
            else
            {
                setNumber(sheet, nr, 2, nullLong);
                setNumber(sheet, nr, 3, nullLong);
                setNumber(sheet, nr, 4, nullLong);
                setNumber(sheet, nr, 5, nullLong);
                setNumber(sheet, nr, 6, nullDouble);
                setNumber(sheet, nr, 7, nullDouble);

                setNumber(sheet, nr, 9, nullLong);
                setNumber(sheet, nr, 10, nullLong);
                setNumber(sheet, nr, 11, nullLong);
            }
        }

        if (tname.equals(Taxon.Астраханская_кочевники))
        {
            // blank verify
            blank_elementary(sheet, 16, 1881, 1914);
        }

        if (tname.equals("Выборгская"))
        {
            // blank иммиграция внутр. миграция
            blank_elementary(sheet, 10, 1881, 1914);
            blank_elementary(sheet, 11, 1881, 1914);
        }

        if (tname.equals("Черноморская"))
        {
            // blank еп прирост (2 колонки) verify 
            blank_elementary(sheet, 8, 1881, 1895);
            blank_elementary(sheet, 13, 1881, 1895);
            blank_elementary(sheet, 14, 1881, 1895);
            blank_elementary(sheet, 16, 1881, 1895);
        }

        saveFile("final-elementary-territories", tname, wb);
    }

    /* =========================================================================================== */

    private void exportTaxon(String tname, Territory tPopulation, Territory tVitalRates) throws Exception
    {
        currentTname = tname;

        // последний год со сведениями о движении
        int maxYear = 1914;
        switch (tname)
        {
        case "Империя":
        case "привислинские губернии":
            maxYear = 1913;
        }

        String template = "excel-templates/composite-taxon-1881-" + maxYear + ".xlsx";
        XSSFWorkbook wb = Excel.loadWorkbook(template);
        XSSFSheet sheet = wb.getSheet("data");
        setText(sheet, 0, 0, "Составная территория: " + tname);

        for (int year = startYear; year <= maxYear + 1; year++)
        {
            int nr = (year - 1881) + (5 - 1);
            final Double nullDouble = null;
            final Long nullLong = null;

            Long pop = tPopulation.territoryYearOrNull(year).progressive_population.total.both;
            Long popmOverall = null;
            setNumber(sheet, nr, 1, pop);
            if (year <= maxYear)
            {
                /* next year start */
                Long pop2 = tPopulation.territoryYearOrNull(year + 1).progressive_population.total.both;
                /* mid-year */
                popmOverall = MathUtil.log_average(pop, pop2);
                setNumber(sheet, nr, 2, popmOverall);

                Long migr = tPopulation.territoryYearOrNull(year).migration.total.both;
                Long emigr = tPopulation.territoryYearOrNull(year).emigration.total.both;
                Long immigr = tPopulation.territoryYearOrNull(year).immigration.total.both;
                Long inner_migr = tPopulation.territoryYearOrNull(year).inner_migration.total.both;

                setNumber(sheet, nr, 3, migr);
                setNumber(sheet, nr, 4, emigr);
                setNumber(sheet, nr, 5, immigr);
                setNumber(sheet, nr, 6, inner_migr);
            }
            else
            {
                setNumber(sheet, nr, 2, nullLong);
                setNumber(sheet, nr, 3, nullLong);
                setNumber(sheet, nr, 4, nullLong);
                setNumber(sheet, nr, 5, nullLong);
                setNumber(sheet, nr, 6, nullLong);
            }

            pop = tVitalRates.territoryYearOrNull(year).progressive_population.total.both;
            setNumber(sheet, nr, 3, pop);
            if (year <= maxYear)
            {
                /* next year start */
                Long pop2 = tVitalRates.territoryYearOrNull(year + 1).progressive_population.total.both;
                /* mid-year */
                double popmVital = MathUtil.log_average(pop, pop2);
                setNumber(sheet, nr, 8, popmVital);

                Long births = tVitalRates.territoryYearOrNull(year).births.total.both;
                Long deaths = tVitalRates.territoryYearOrNull(year).deaths.total.both;
                Long migr = tVitalRates.territoryYearOrNull(year).migration.total.both;
                Long emigr = tVitalRates.territoryYearOrNull(year).emigration.total.both;
                Long immigr = tVitalRates.territoryYearOrNull(year).immigration.total.both;
                Long inner_migr = tVitalRates.territoryYearOrNull(year).inner_migration.total.both;

                setNumber(sheet, nr, 9, births);
                setNumber(sheet, nr, 10, deaths);
                setNumber(sheet, nr, 11, migr);

                Double cbr = (1000.0 * births) / popmVital;
                Double cdr = (1000.0 * deaths) / popmVital;

                setNumber(sheet, nr, 12, round(cbr, 3));
                setNumber(sheet, nr, 13, round(cdr, 3));
                // 14 = естественный прирост

                Double pctVital = (100.0 * popmVital) / popmOverall;
                setNumber(sheet, nr, 15, round(pctVital, 2));

                setNumber(sheet, nr, 16, emigr);
                setNumber(sheet, nr, 17, immigr);
                setNumber(sheet, nr, 18, inner_migr);
            }
            else
            {
                setNumber(sheet, nr, 8, nullLong);
                setNumber(sheet, nr, 9, nullLong);
                setNumber(sheet, nr, 10, nullLong);
                setNumber(sheet, nr, 11, nullLong);

                setNumber(sheet, nr, 12, nullDouble);
                setNumber(sheet, nr, 13, nullDouble);
                setNumber(sheet, nr, 14, nullDouble);
                setNumber(sheet, nr, 15, nullDouble);

                setNumber(sheet, nr, 16, nullLong);
                setNumber(sheet, nr, 17, nullLong);
                setNumber(sheet, nr, 18, nullLong);
            }
        }

        saveFile("final-composite-taxons", tname, wb);
    }

    /* =========================================================================================== */

    private void setNumber(XSSFSheet sheet, int nr, int nc, Long value)
    {
        Row row = sheet.getRow(nr);
        setNumber(row, nc, value);
    }

    private void setNumber(Row row, int nc, Long value)
    {
        Cell cell = row.getCell(nc);

        if (cell == null)
            cell = row.createCell(nc);

        if (value == null)
        {
            cell.setBlank();
        }
        else
        {
            cell.setBlank();
            cell.setCellValue(value);
        }

        if (Util.False && currentTname.equals("Империя"))
            Util.out(String.format("Filling cell %d %d => %s", row.getRowNum(), nc, value));
    }

    private void setNumber(XSSFSheet sheet, int nr, int nc, Double value)
    {
        Row row = sheet.getRow(nr);
        setNumber(row, nc, value);
    }

    private void setNumber(Row row, int nc, Double value)
    {
        Cell cell = row.getCell(nc);

        if (cell == null)
            cell = row.createCell(nc);

        if (value == null)
        {
            cell.setBlank();
        }
        else
        {
            cell.setBlank();
            cell.setCellValue(value);
        }

        if (Util.False && currentTname.equals("Империя"))
            Util.out(String.format("Filling cell %d %d => %s", row.getRowNum(), nc, value));
    }

    private void setText(XSSFSheet sheet, int nr, int nc, String value)
    {
        Row row = sheet.getRow(nr);
        setText(row, nc, value);
    }

    private void setText(Row row, int nc, String value)
    {
        Cell cell = row.getCell(nc);

        if (cell == null)
            cell = row.createCell(nc);

        if (value == null)
            cell.setBlank();
        else
            cell.setCellValue(value);
    }

    private void blank_elementary(XSSFSheet sheet, int nc, int y1, int y2)
    {
        for (int year = y1; year <= y2; year++)
        {
            int nr = (year - 1881) + (6 - 1);
            Row row = sheet.getRow(nr);
            Cell cell = row.getCell(nc);
            if (cell == null)
                cell = row.createCell(nc);
            cell.setBlank();
        }
    }

    /* =========================================================================================== */

    private Double round(Double v, int places)
    {
        if (v == null)
            return null;

        if (places < 0)
            throw new IllegalArgumentException("places must be non-negative");

        return BigDecimal.valueOf(v)
                .setScale(places, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private void saveFile(String subdir, String tname, XSSFWorkbook wb) throws Exception
    {
        XSSFFormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
        evaluator.evaluateAll();
        wb.setForceFormulaRecalculation(true);

        while (tname.endsWith("."))
            tname = Util.stripTail(tname, ".");
        // tname = tname.replace(" ", "-");

        File dir = new File(OUT_PATH);
        dir = new File(dir, subdir);
        dir.mkdirs();

        File fp = new File(dir, tname + ".xlsx");
        try (OutputStream out = Files.newOutputStream(fp.toPath()))
        {
            wb.write(out);
        }
    }
}
