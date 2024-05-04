package my.excel;

import com.sun.jna.platform.win32.COM.util.Factory;
import com.sun.jna.platform.win32.COM.util.office.excel.ComExcel_Application;
// import com.sun.jna.platform.win32.COM.util.office.excel.ComIAppEvents;
import com.sun.jna.platform.win32.COM.util.office.excel.ComIApplication;
import com.sun.jna.platform.win32.COM.util.office.excel.ComIRange;
import com.sun.jna.platform.win32.COM.util.office.excel.ComIWorkbook;
import com.sun.jna.platform.win32.COM.util.office.excel.ComIWorksheet;

import rtss.util.Util;

public class MyExcelTest
{
    public static void main(String[] args)
    {
        try
        {
            new MyExcelTest().main();
        }
        catch (Throwable ex)
        {
            System.err.println("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
    
    private void main() throws Exception
    {
        // https://learn.microsoft.com/en-us/office/vba/api/excel.range(object)
        // https://learn.microsoft.com/en-us/office/vba/api/excel.worksheet
        // https://learn.microsoft.com/en-us/office/vba/api/excel.workbook
        ComIApplication msExcel = null;
        Factory factory = new Factory();
        ComExcel_Application excelObject = factory.createObject(ComExcel_Application.class);
        msExcel = excelObject.queryInterface(ComIApplication.class);
        System.out.println("MSExcel version: " + msExcel.getVersion());
        msExcel.setVisible(true);
        ComIWorkbook workbook = msExcel.getWorkbooks().Add();
        ComIWorksheet sheet = workbook.getActiveSheet();
        sheet.getRange("A1").setValue("Hello from JNA!");
        sheet.getRange("A2").setValue("Hello again from JNA!");
        
        sheet.getRange("A3").setValue("1");
        sheet.getRange("A3").setNumberFormat("0");
        
        sheet.getRange("A4").setValue("1.9");
        sheet.getRange("A4").setNumberFormat("0.000");
        
        @SuppressWarnings("unused")
        String s1 = sheet.getRange("A3").getNumberFormat();
        @SuppressWarnings("unused")
        String s2 = sheet.getRange("A4").getNumberFormat();
        sheet.getRange("A5").setFormula("=A3+A4");
        // sheet.getRange("A5").setValue("=A3+A4");
        sheet.getRange("A5").setNumberFormat("0.000");
        @SuppressWarnings("unused")
        Object o = sheet.getRange("A5").getValue();
        sheet.getRange("A5").getEntireColumn().AutoFit();
        @SuppressWarnings("unused")
        ComIRange range = sheet.getRange("A5");
        workbook.Close(false);
        
        msExcel.Quit();
        factory.disposeAll();
        
        Util.noop();
    }
}
