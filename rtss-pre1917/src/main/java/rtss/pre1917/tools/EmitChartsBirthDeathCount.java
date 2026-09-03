package rtss.pre1917.tools;

import org.apache.poi.ss.usermodel.Row;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import rtss.pre1917.LoadData;
import rtss.pre1917.LoadData.LoadOptions;
import rtss.pre1917.calc.CorrectTerritories;
import rtss.pre1917.data.Taxon;
import rtss.pre1917.data.Territory;
import rtss.pre1917.data.TerritoryDataSet;
import rtss.util.Util;
import rtss.util.excel.Excel;

public class EmitChartsBirthDeathCount
{
    private static final String OUT_PATH = "c:\\@\\rtss-pre1917\\charts\\birth-death-counts";

    public static enum ChartType
    {
        RawSources, AdjustedIntermediate, AdjustedFinal;

        public boolean isAdjusted()
        {
            switch (this)
            {
            case RawSources:
                return false;

            case AdjustedIntermediate:
            case AdjustedFinal:
                return true;

            default:
                throw new IllegalArgumentException();
            }
        }

        public boolean isAdjustedFinal()
        {
            switch (this)
            {
            case RawSources:
            case AdjustedIntermediate:
                return false;

            case AdjustedFinal:
                return true;

            default:
                throw new IllegalArgumentException();
            }
        }
    }

    public static void main(String[] args)
    {
        try
        {
            Util.out("=== Emitting RawSources charts");
            Util.out("");
            new EmitChartsBirthDeathCount().do_main(ChartType.RawSources);

            Util.out("");
            Util.out("=== Emitting AdjustedIntermediate charts");
            Util.out("");
            new EmitChartsBirthDeathCount().do_main(ChartType.AdjustedIntermediate);

            Util.out("");
            Util.out("=== Emitting AdjustedFinal charts");
            Util.out("");
            new EmitChartsBirthDeathCount().do_main(ChartType.AdjustedFinal);

            Util.out("");
            Util.out("** Done");
        }
        catch (Throwable ex)
        {
            Util.err("** Exception: ");
            ex.printStackTrace();
        }
    }

    private TerritoryDataSet tdsCSKPatched;
    private TerritoryDataSet tdsUGVIPatched;
    private TerritoryDataSet tdsCSKUnpatched;
    private TerritoryDataSet tdsUGVIUnpatched;

    private void init(ChartType chartType) throws Exception
    {
        tdsCSKPatched = new LoadData().loadEvroChast(LoadOptions.MERGE_CITIES,
                                                     LoadOptions.MERGE_POST1897_REGIONS,
                                                     LoadOptions.DONT_VERIFY,
                                                     LoadOptions.APPLY_PATCHES,
                                                     chartType.isAdjusted() ? LoadOptions.ADJUST_FEMALE_BIRTHS
                                                                            : LoadOptions.DONT_ADJUST_FEMALE_BIRTHS,
                                                     chartType.isAdjusted() ? LoadOptions.FILL_MISSING_BD
                                                                            : LoadOptions.DONT_FILL_MISSING_BD);

        tdsUGVIPatched = new LoadData().loadUGVI(LoadOptions.MERGE_CITIES,
                                                 LoadOptions.MERGE_POST1897_REGIONS,
                                                 LoadOptions.DONT_VERIFY,
                                                 LoadOptions.APPLY_PATCHES,
                                                 chartType.isAdjusted() ? LoadOptions.ADJUST_FEMALE_BIRTHS
                                                                        : LoadOptions.DONT_ADJUST_FEMALE_BIRTHS,
                                                 chartType.isAdjusted() ? LoadOptions.FILL_MISSING_BD
                                                                        : LoadOptions.DONT_FILL_MISSING_BD,
                                                 LoadOptions.DONT_EVAL_SPLIT_ASTRAKHAN,
                                                 LoadOptions.EVAL_MERGE_ASTRAKHAN,
                                                 chartType.isAdjustedFinal() ? LoadOptions.EVAL_PROGRESSIVE
                                                                             : LoadOptions.DONT_EVAL_PROGRESSIVE);

        if (chartType == ChartType.RawSources)
        {
            tdsCSKUnpatched = new LoadData().loadEvroChast(LoadOptions.MERGE_CITIES,
                                                           LoadOptions.MERGE_POST1897_REGIONS,
                                                           LoadOptions.DONT_VERIFY,
                                                           LoadOptions.DONT_APPLY_PATCHES,
                                                           chartType.isAdjusted() ? LoadOptions.ADJUST_FEMALE_BIRTHS
                                                                                  : LoadOptions.DONT_ADJUST_FEMALE_BIRTHS,
                                                           chartType.isAdjusted() ? LoadOptions.FILL_MISSING_BD
                                                                                  : LoadOptions.DONT_FILL_MISSING_BD);

            tdsUGVIUnpatched = new LoadData().loadUGVI(LoadOptions.MERGE_CITIES,
                                                       LoadOptions.MERGE_POST1897_REGIONS,
                                                       LoadOptions.DONT_VERIFY,
                                                       LoadOptions.DONT_APPLY_PATCHES,
                                                       chartType.isAdjusted() ? LoadOptions.ADJUST_FEMALE_BIRTHS
                                                                              : LoadOptions.DONT_ADJUST_FEMALE_BIRTHS,
                                                       chartType.isAdjusted() ? LoadOptions.FILL_MISSING_BD
                                                                              : LoadOptions.DONT_FILL_MISSING_BD,
                                                       LoadOptions.DONT_EVAL_SPLIT_ASTRAKHAN,
                                                       LoadOptions.EVAL_MERGE_ASTRAKHAN,
                                                       LoadOptions.DONT_EVAL_PROGRESSIVE);
        }

        if (chartType == ChartType.AdjustedFinal)
        {
            TerritoryDataSet tdsPopulation = tdsUGVIPatched;
            TerritoryDataSet tdsVitalRates = tdsPopulation.dup();
            CorrectTerritories ct = new CorrectTerritories("Империя", 1881, 1914, tdsPopulation, tdsVitalRates);
            ct.corrections();
        }
    }

    private void do_main(ChartType chartType) throws Exception
    {
        init(chartType);

        for (String tname : Util.sort(tdsUGVIPatched.keySet()))
        {
            if (Taxon.isComposite(tname))
                continue;

            Util.out(tname);

            Territory tUGVIPatched = tdsUGVIPatched.get(tname);
            Territory tCSKPatched = tdsCSKPatched.get(tname);

            Territory tUGVIUnpatched = tdsUGVIUnpatched == null ? null : tdsUGVIUnpatched.get(tname);
            Territory tCSKUnpatched = tdsCSKUnpatched == null ? null : tdsCSKUnpatched.get(tname);

            if (tUGVIPatched == null)
                throw new Exception("Нет территоррии");

            if (chartType == ChartType.RawSources && tUGVIUnpatched == null)
                throw new Exception("Нет территоррии");

            if (chartType == ChartType.RawSources && (tCSKPatched == null) != (tCSKUnpatched == null))
                throw new Exception("Нет территоррии");

            boolean hasCSK = (tCSKPatched != null);

            XSSFWorkbook wb = null;
            switch (chartType)
            {
            case RawSources:
                wb = Excel.loadWorkbook(hasCSK ? "excel-templates/birth-death-counts-ugvi-csk-raw-sources.xlsx"
                                               : "excel-templates/birth-death-counts-ugvi-raw-sources.xlsx");
                break;

            case AdjustedIntermediate:
            case AdjustedFinal:
                wb = Excel.loadWorkbook(hasCSK ? "excel-templates/birth-death-counts-ugvi-csk-adjusted.xlsx"
                                               : "excel-templates/birth-death-counts-ugvi-adjusted.xlsx");
                break;
            }

            XSSFSheet sheet = wb.getSheet("data");
            sheet.getRow(0).getCell(5).setCellValue(tname);

            for (int year = 1880; year <= 1914; year++)
            {
                Long birthsUGVIPatched = null;
                Long deathsUGVIPatched = null;

                Long birthsUGVIUnpatched = null;
                Long deathsUGVIUnpatched = null;

                Long birthsCSKPatched = null;
                Long deathsCSKPatched = null;

                Long birthsCSKUnpatched = null;
                Long deathsCSKUnpatched = null;

                if (tUGVIPatched != null && tUGVIPatched.territoryYearOrNull(year) != null)
                {
                    birthsUGVIPatched = tUGVIPatched.territoryYearOrNull(year).births.total.both;
                    deathsUGVIPatched = tUGVIPatched.territoryYearOrNull(year).deaths.total.both;
                }

                if (tUGVIUnpatched != null && tUGVIUnpatched.territoryYearOrNull(year) != null)
                {
                    birthsUGVIUnpatched = tUGVIUnpatched.territoryYearOrNull(year).births.total.both;
                    deathsUGVIUnpatched = tUGVIUnpatched.territoryYearOrNull(year).deaths.total.both;
                }

                if (tCSKPatched != null && tCSKPatched.territoryYearOrNull(year) != null)
                {
                    birthsCSKPatched = tCSKPatched.territoryYearOrNull(year).births.total.both;
                    deathsCSKPatched = tCSKPatched.territoryYearOrNull(year).deaths.total.both;
                }

                if (tCSKUnpatched != null && tCSKUnpatched.territoryYearOrNull(year) != null)
                {
                    birthsCSKUnpatched = tCSKUnpatched.territoryYearOrNull(year).births.total.both;
                    deathsCSKUnpatched = tCSKUnpatched.territoryYearOrNull(year).deaths.total.both;
                }

                int nr = (year - 1880) + (6 - 1);

                if (hasCSK)
                {
                    setNumber(sheet, nr, 1, birthsUGVIPatched);
                    setNumber(sheet, nr, 2, deathsUGVIPatched);
                    setNumber(sheet, nr, 3, birthsCSKPatched);
                    setNumber(sheet, nr, 4, deathsCSKPatched);

                    if (chartType == ChartType.RawSources)
                    {
                        setUnpatched(sheet, nr, 6, birthsUGVIPatched, birthsUGVIUnpatched);
                        setUnpatched(sheet, nr, 7, deathsUGVIPatched, deathsUGVIUnpatched);
                        setUnpatched(sheet, nr, 8, birthsCSKPatched, birthsCSKUnpatched);
                        setUnpatched(sheet, nr, 9, deathsCSKPatched, deathsCSKUnpatched);
                    }
                }
                else
                {
                    setNumber(sheet, nr, 1, birthsUGVIPatched);
                    setNumber(sheet, nr, 2, deathsUGVIPatched);

                    if (chartType == ChartType.RawSources)
                    {
                        setUnpatched(sheet, nr, 4, birthsUGVIPatched, birthsUGVIUnpatched);
                        setUnpatched(sheet, nr, 5, deathsUGVIPatched, deathsUGVIUnpatched);
                    }
                }
            }

            while (tname.endsWith("."))
                tname = Util.stripTail(tname, ".");

            File dir = new File(OUT_PATH);
            switch (chartType)
            {
            case RawSources:
                dir = new File(dir, "raw-sources");
                break;

            case AdjustedIntermediate:
                dir = new File(dir, "adjusted-stage1-intermediate");
                break;

            case AdjustedFinal:
                dir = new File(dir, "adjusted-stage2-final");
                break;
            }
            dir.mkdirs();

            File fp = new File(dir, tname + ".xlsx");
            try (OutputStream out = Files.newOutputStream(fp.toPath()))
            {
                wb.write(out);
            }
        }
    }

    private void setUnpatched(XSSFSheet sheet, int nr, int nc, Long vPatched, Long vUnpatched)
    {
        if (vPatched != null && vUnpatched != null && (long) vPatched == (long) vUnpatched)
        {
            setNumber(sheet, nr, nc, null);
        }
        else if (vUnpatched != null)
        {
            setNumber(sheet, nr, nc, vUnpatched);
        }
        else
        {
            setText(sheet, nr, nc, "ND");
        }
    }

    private static void setNumber(XSSFSheet sheet, int nr, int nc, Long value)
    {
        Row row = sheet.getRow(nr);
        setNumber(row, nc, value);
    }

    private static void setNumber(Row row, int nc, Long value)
    {
        Cell cell = row.getCell(nc);

        if (cell == null)
            cell = row.createCell(nc);

        if (value == null)
            cell.setBlank();
        else
            cell.setCellValue(value);
    }

    private static void setText(XSSFSheet sheet, int nr, int nc, String value)
    {
        Row row = sheet.getRow(nr);
        setText(row, nc, value);
    }

    private static void setText(Row row, int nc, String value)
    {
        Cell cell = row.getCell(nc);

        if (cell == null)
            cell = row.createCell(nc);

        if (value == null)
            cell.setBlank();
        else
            cell.setCellValue(value);
    }
}
