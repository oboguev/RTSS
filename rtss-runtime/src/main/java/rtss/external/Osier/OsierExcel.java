package rtss.external.Osier;

import com.sun.jna.platform.win32.COM.util.Factory;
import com.sun.jna.platform.win32.COM.util.office.excel.ComExcel_Application;
import com.sun.jna.platform.win32.COM.util.office.excel.ComIApplication;
import com.sun.jna.platform.win32.COM.util.office.excel.ComIWorkbook;
import com.sun.jna.platform.win32.COM.util.office.excel.ComIWorksheet;

import rtss.data.bin.Bin;
// import rtss.util.Util;

public class OsierExcel
{
    private CellAddressAllocator allocator = new CellAddressAllocator();

    private CellAddress aBaseHandle;
    private CellAddress aBaseName;
    private CellAddress aBaseObjectType;
    private CellAddressRange aBaseBodyProps;
    private CellAddressRange aBaseBodyValues;
    private CellAddress aBaseTableName;
    private CellAddressRange aBaseTableCols;
    private CellAddressRange aBaseTableValues;

    private ComIApplication msExcel;
    private Factory factory;
    private ComExcel_Application excelObject;
    private ComIWorkbook workbook;
    private ComIWorksheet sheet;

    public void start(boolean visible)
    {
        factory = new Factory();
        excelObject = factory.createObject(ComExcel_Application.class);
        msExcel = excelObject.queryInterface(ComIApplication.class);
        // Util.out("MSExcel version: " + msExcel.getVersion());
        msExcel.setVisible(visible);
        workbook = msExcel.getWorkbooks().Add();
        sheet = workbook.getActiveSheet();
    }

    public void stop()
    {
        if (workbook != null)
        {
            workbook.Close(false);
            workbook = null;
        }

        if (msExcel != null)
        {
            msExcel.Quit();
            msExcel = null;
        }

        if (factory != null)
        {
            factory.disposeAll();
            factory = null;
        }
    }

    public void createBaseMortalityObject(Bin[] bins, String baseName) throws Exception
    {
        aBaseHandle = allocator.one();
        aBaseName = allocator.one();
        aBaseObjectType = allocator.one();
        aBaseBodyProps = allocator.vertical(3);
        aBaseBodyValues = aBaseBodyProps.offset(1, 0);
        aBaseTableName = allocator.one();
        aBaseTableCols = allocator.horizontal(3);
        aBaseTableValues = allocator.block(3, bins.length);

        setCell(aBaseName, baseName);
        setCell(aBaseObjectType, "MORTALITY");

        setCell(aBaseBodyProps.upperLeft, "Population");
        setCell(aBaseBodyValues.upperLeft, "XXX-M");

        setCell(aBaseBodyProps.upperLeft.offset(0, 1), "Date");
        setCell(aBaseBodyValues.upperLeft.offset(0, 1), "20110601");

        setCell(aBaseBodyProps.upperLeft.offset(0, 2), "BuildMethod");
        setCell(aBaseBodyValues.upperLeft.offset(0, 2), "HYBRID_FORCE");

        setCell(aBaseTableName, "Death Rates");

        setCell(aBaseTableCols.upperLeft, "Age");
        setCell(aBaseTableCols.upperLeft.offset(1, 0), "Rate");
        setCell(aBaseTableCols.upperLeft.offset(2, 0), "Use");

        int dy = -1;
        for (Bin bin : bins)
        {
            dy++;
            setCell(aBaseTableValues.upperLeft.offset(0, dy), bin.age_x1);
            setCell(aBaseTableValues.upperLeft.offset(1, dy), bin.avg);
            setCell(aBaseTableValues.upperLeft.offset(2, dy), 1);
        }
        
        autoFiltAll(aBaseTableValues);

        // call return-value-cell function-name args...
        // sb.append("call");
        // sb.append(" " + aBaseHandle);
        // sb.append(" " + "CreateObj");
        // sb.append(" " + aBaseName);
        // sb.append(" " + aBaseObjectType);
        // sb.append(" " + aBaseBodyProps);
        // sb.append(" " + aBaseBodyValues);
        // sb.append(" " + aBaseTableName);
        // sb.append(" " + aBaseTableCols);
        // sb.append(" " + aBaseTableValues);
        // sb.append(nl);
    }

    public void setCell(CellAddress ca, String value)
    {
        sheet.getRange(ca.toString()).setValue(value);
    }

    public void setCell(CellAddress ca, int value)
    {
        sheet.getRange(ca.toString()).setValue(String.format("%d", value));
        sheet.getRange(ca.toString()).setNumberFormat("0");
    }

    public void setCell(CellAddress ca, double value)
    {
        sheet.getRange(ca.toString()).setValue(String.format("%f", value));
        sheet.getRange(ca.toString()).setNumberFormat("0.0000");
    }

    private void autoFiltAll(CellAddressRange last)
    {
        autoFiltAll(last.bottomRight);
    }
    
    private void autoFiltAll(CellAddress last)
    {
        for (char col = 'A'; col <= last.col; col++)
            sheet.getRange("" + col + "1").getEntireColumn().AutoFit();
    }
}
