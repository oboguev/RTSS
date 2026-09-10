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
            exportElementary(tname, t);
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

    private void exportElementary(String tname, Territory t) throws Exception
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

        String template = "excel-templates/elementary-territory-1881-" + maxYear + ".xlsx";
        XSSFWorkbook wb = Excel.loadWorkbook(template);
        XSSFSheet sheet = wb.getSheet("data");
        setText(sheet, 0, 0, tname);

        if (t.hasValidVitalRate)
            setText(sheet, 1, 0, "Территория включена в учёт естественного движения");
        else
            setText(sheet, 1, 0, "Территория НЕ включена в учёт естественного движения");

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
                
                setNumber(sheet, nr, 3, births);
                setNumber(sheet, nr, 4, deaths);
                setNumber(sheet, nr, 5, migr);

                Double cbr = births == null ? null : (1000.0 * births) / popm;
                Double cdr = deaths == null ? null : (1000.0 * deaths) / popm;

                setNumber(sheet, nr, 6, round(cbr, 3));
                setNumber(sheet, nr, 7, round(cdr, 3));
            }
            else
            {
                setNumber(sheet, nr, 2, nullLong);
                setNumber(sheet, nr, 3, nullLong);
                setNumber(sheet, nr, 4, nullLong);
                setNumber(sheet, nr, 5, nullLong);
                setNumber(sheet, nr, 6, nullDouble);
                setNumber(sheet, nr, 7, nullDouble);
            }
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
            long popm;
            final Double nullDouble = null;
            final Long nullLong = null;

            Long pop = tPopulation.territoryYearOrNull(year).progressive_population.total.both;
            setNumber(sheet, nr, 1, pop);
            if (year <= maxYear)
            {
                /* next year start */
                Long pop2 = tPopulation.territoryYearOrNull(year + 1).progressive_population.total.both;
                /* mid-year */
                popm = MathUtil.log_average(pop, pop2);
                setNumber(sheet, nr, 2, popm);
            }
            else
            {
                setNumber(sheet, nr, 2, nullLong);
            }

            pop = tVitalRates.territoryYearOrNull(year).progressive_population.total.both;
            setNumber(sheet, nr, 3, pop);
            if (year <= maxYear)
            {
                /* next year start */
                Long pop2 = tVitalRates.territoryYearOrNull(year + 1).progressive_population.total.both;
                /* mid-year */
                popm = MathUtil.log_average(pop, pop2);
                setNumber(sheet, nr, 4, popm);

                Long births = tVitalRates.territoryYearOrNull(year).births.total.both;
                Long deaths = tVitalRates.territoryYearOrNull(year).deaths.total.both;
                Long migr = tVitalRates.territoryYearOrNull(year).migration.total.both;

                setNumber(sheet, nr, 5, births);
                setNumber(sheet, nr, 6, deaths);
                setNumber(sheet, nr, 7, migr);

                Double cbr = (1000.0 * births) / popm;
                Double cdr = (1000.0 * deaths) / popm;

                setNumber(sheet, nr, 8, round(cbr, 3));
                setNumber(sheet, nr, 9, round(cdr, 3));
            }
            else
            {
                setNumber(sheet, nr, 4, nullLong);
                setNumber(sheet, nr, 5, nullLong);
                setNumber(sheet, nr, 6, nullLong);
                setNumber(sheet, nr, 7, nullLong);

                setNumber(sheet, nr, 8, nullDouble);
                setNumber(sheet, nr, 9, nullDouble);
                setNumber(sheet, nr, 10, nullDouble);
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
