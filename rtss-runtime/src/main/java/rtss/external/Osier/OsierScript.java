package rtss.external.Osier;

import java.util.Map;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;
import rtss.external.Script;
import rtss.external.ScriptReply;
import rtss.util.Util;

/*
 * Generate VBScript commands for execution by Windows CScript controlling Excel instance
 * that hosts Osier add-in. This module only generates script that has to be executed later
 * by OsierLocal or OsierClient+OsierServer.
 * 
 * OsierScript -> OsierCall -> OsierLocal -> CScript -> Excel -> Osier add-in
 *   
 */
public class OsierScript
{
    private StringBuilder totalScript = new StringBuilder();
    private StringBuilder sb = new StringBuilder();
    private static final String nl = "\n";
    public static final String EXEC = "'---execute---\n";
    private CellAddressAllocator allocator = new CellAddressAllocator();
    private String baseName;

    private CellAddress aBaseHandle;
    private CellAddress aBaseName;
    private CellAddress aBaseObjectType;
    private CellAddressRange aBaseBodyProps;
    private CellAddressRange aBaseBodyValues;
    private CellAddress aBaseTableName;
    private CellAddressRange aBaseTableCols;
    private CellAddressRange aBaseTableValues;

    private CellAddress aModHandle;
    private CellAddress aModRequestedHandle;
    private CellAddress aModBuildMethod;

    private CellAddressRange aFunctionBlock;

    public String getScript() throws Exception
    {
        String sc = sb.toString();
        if (Util.lastchar(sc) != '\n')
            sc += nl;
        sc += EXEC;
        totalScript.append(sc);
        return sc;
    }

    public void newScript()
    {
        sb.setLength(0);
    }

    /* ============================================================================================= */

    public void start(boolean visible) throws Exception
    {
        sbnl();
        sb.append(getDefaultStartupScript(visible));
    }
    
    public static String getDefaultStartupScript(boolean visible) throws Exception
    {
        return Script.script("osier-excel/start-excel.vbs", "visible", visible ? "true" : "false");
    }

    public void stop() throws Exception
    {
        sbnl();
        sb.append(Script.script("osier-excel/stop-excel.vbs"));
    }

    public void say(String text)
    {
        sbnl();
        sb.append(String.format("say \"%s\"" + nl, escape(text)));
        sb.append(EXEC);
    }

    private void show_value(String what, CellAddress ca)
    {
        sbnl();
        sb.append(String.format("showCellValue %s, %s" + nl,
                                enquote(escape(what + ": ")),
                                enquote(ca.toString())));
    }

    public void clear_worksheet() throws Exception
    {
        allocator = new CellAddressAllocator();
        aBaseHandle = null;
        aBaseName = null;
        aBaseObjectType = null;
        aBaseBodyProps = null;
        aBaseBodyValues = null;
        aBaseTableName = null;
        aBaseTableCols = null;
        aBaseTableValues = null;
        aModHandle = null;
        aModRequestedHandle = null;
        aModBuildMethod = null;
        aFunctionBlock = null;

        sbnl();
        sb.append(Script.script("osier-excel/clear-worksheet.vbs"));
    }

    /* ============================================================================================= */

    /*
     * mxData = true if bins contain "mx" values, false for "qx"
     */
    public void createBaseMortalityObject(Bin[] bins, String baseName, boolean mxData) throws Exception
    {
        this.baseName = baseName;

        aBaseHandle = allocator.one();
        aBaseName = allocator.one();
        aBaseObjectType = allocator.one();
        aBaseBodyProps = allocator.vertical(3);
        aBaseBodyValues = aBaseBodyProps.offset(1, 0);
        aBaseTableName = allocator.one();
        aBaseTableCols = allocator.horizontal(3);
        aBaseTableValues = allocator.block(3, bins.length + 1);

        sb.append(nl);
        sb.append("'" + nl);
        sb.append("' create base mortality object" + nl);
        sb.append("'" + nl);
        sb.append(nl);

        setCell(aBaseName, baseName);
        setCell(aBaseObjectType, "MORTALITY");

        setCell(aBaseBodyProps.upperLeft, "Population");
        setCell(aBaseBodyValues.upperLeft, baseName);

        setCell(aBaseBodyProps.upperLeft.offset(0, 1), "Date");
        setCell(aBaseBodyValues.upperLeft.offset(0, 1), "20110601");

        setCell(aBaseBodyProps.upperLeft.offset(0, 2), "BuildMethod");
        setCell(aBaseBodyValues.upperLeft.offset(0, 2), "HYBRID_FORCE");

        if (mxData)
            setCell(aBaseTableName, "DeathRates");
        else
            setCell(aBaseTableName, "DeathProbabilities");

        setCell(aBaseTableCols.upperLeft, "Age");
        if (mxData)
            setCell(aBaseTableCols.upperLeft.offset(1, 0), "Rate");
        else
            setCell(aBaseTableCols.upperLeft.offset(1, 0), "Probability");
        setCell(aBaseTableCols.upperLeft.offset(2, 0), "Use");

        int dy = -1;
        for (Bin bin : bins)
        {
            dy++;
            setCell(aBaseTableValues.upperLeft.offset(0, dy), bin.age_x1);
            setCell(aBaseTableValues.upperLeft.offset(1, dy), bin.avg);
            setCell(aBaseTableValues.upperLeft.offset(2, dy), 1);
        }

        /* 
         * Insert dummy row with Use = 0 to designate the end of last bin and max age
         */
        dy++;
        setCell(aBaseTableValues.upperLeft.offset(0, dy), Bins.lastBin(bins).age_x2 + 1);
        setCell(aBaseTableValues.upperLeft.offset(1, dy), 0.0);
        setCell(aBaseTableValues.upperLeft.offset(2, dy), 0);

        String formula = String.format("=CreateObj(%s,%s,%s,%s,%s,%s,%s)",
                                       aBaseName, aBaseObjectType, aBaseBodyProps, aBaseBodyValues,
                                       aBaseTableName, aBaseTableCols, aBaseTableValues);
        setFormula(aBaseHandle, formula);
        show_value("BaseHandle", aBaseHandle);
    }

    public void replyBaseMortalityObject(String reply) throws Exception
    {
        Map<String, String> mss = ScriptReply.keysFromReply(reply, new String[] { "BaseHandle" });
        String baseHandle = mss.get("BaseHandle");
        if (!baseHandle.startsWith(baseName + ":"))
            throw new Exception("Osier failed to create base object handle");
    }

    /* ============================================================================================= */

    public void modifyBaseMortalityObject(String buildMethodWithParameters) throws Exception
    {
        sb.append(nl);
        sb.append("'" + nl);
        sb.append("' modify base mortality object" + nl);
        sb.append("'" + nl);
        sb.append(nl);

        aModHandle = allocator.one();
        aModRequestedHandle = allocator.one();
        aModBuildMethod = allocator.one();

        setCell(aModRequestedHandle, baseName + "_MODIFIED");
        setCell(aModBuildMethod, buildMethodWithParameters);
        String formula = String.format("=ModifyObj(%s,%s,%s,,%s,,+%s)",
                                       aModRequestedHandle, aBaseHandle, enquote("mortality"),
                                       enquote("buildmethod"), aModBuildMethod);
        setFormula(aModHandle, formula);
        show_value("ModHandle", aModHandle);
    }

    public void replyModifyBaseMortalityObject(String reply) throws Exception
    {
        Map<String, String> mss = ScriptReply.keysFromReply(reply, new String[] { "ModHandle" });
        String modHandle = mss.get("ModHandle");
        if (!modHandle.startsWith(baseName + "_MODIFIED:"))
            throw new Exception("Osier failed to create modified object handle");
    }

    /* ============================================================================================= */
    
    public void createBasePopulationObject(Bin[] bins, String baseName) throws Exception
    {
        this.baseName = baseName;

        aBaseHandle = allocator.one();
        aBaseName = allocator.one();
        aBaseObjectType = allocator.one();
        aBaseBodyProps = allocator.vertical(5);
        aBaseBodyValues = aBaseBodyProps.offset(1, 0);
        aBaseTableName = allocator.one();
        aBaseTableCols = allocator.horizontal(2);
        aBaseTableValues = allocator.block(2, bins.length + 1);

        sb.append(nl);
        sb.append("'" + nl);
        sb.append("' create base population object" + nl);
        sb.append("'" + nl);
        sb.append(nl);

        setCell(aBaseName, baseName);
        setCell(aBaseObjectType, "PCURVE");

        setCell(aBaseBodyProps.upperLeft, "Population");
        setCell(aBaseBodyValues.upperLeft, baseName);

        setCell(aBaseBodyProps.upperLeft.offset(0, 1), "Date");
        setCell(aBaseBodyValues.upperLeft.offset(0, 1), "20110601");

        setCell(aBaseBodyProps.upperLeft.offset(0, 2), "BuildMethod");
        setCell(aBaseBodyValues.upperLeft.offset(0, 2), "CONSTANT_DENSITY");

        setCell(aBaseBodyProps.upperLeft.offset(0, 3), "Source");
        setCell(aBaseBodyValues.upperLeft.offset(0, 3), "other");
        
        setCell(aBaseBodyProps.upperLeft.offset(0, 4), "Comment");
        setCell(aBaseBodyValues.upperLeft.offset(0, 4), "none");
        
        setCell(aBaseTableName, "Numbers");

        setCell(aBaseTableCols.upperLeft, "Age");
        setCell(aBaseTableCols.upperLeft.offset(1, 0), "Number");

        int dy = -1;
        for (Bin bin : bins)
        {
            dy++;
            setCell(aBaseTableValues.upperLeft.offset(0, dy), bin.age_x1);
            setCell(aBaseTableValues.upperLeft.offset(1, dy), bin.avg);
        }

        /* 
         * Insert dummy row with Number = 0 to designate the end of last bin and max age
         */
        dy++;
        setCell(aBaseTableValues.upperLeft.offset(0, dy), Bins.lastBin(bins).age_x2 + 1);
        setCell(aBaseTableValues.upperLeft.offset(1, dy), 0.0);

        String formula = String.format("=CreateObj(%s,%s,%s,%s,%s,%s,%s)",
                                       aBaseName, aBaseObjectType, aBaseBodyProps, aBaseBodyValues,
                                       aBaseTableName, aBaseTableCols, aBaseTableValues);
        setFormula(aBaseHandle, formula);
        show_value("BaseHandle", aBaseHandle);
    }

    public void replyBasePopulationObject(String reply) throws Exception
    {
        Map<String, String> mss = ScriptReply.keysFromReply(reply, new String[] { "BaseHandle" });
        String baseHandle = mss.get("BaseHandle");
        if (!baseHandle.startsWith(baseName + ":"))
            throw new Exception("Osier failed to create base object handle");
    }

    /* ============================================================================================= */
    
    public void modifyBasePopulationObject(String buildMethodWithParameters) throws Exception
    {
        sb.append(nl);
        sb.append("'" + nl);
        sb.append("' modify base population object" + nl);
        sb.append("'" + nl);
        sb.append(nl);

        aModHandle = allocator.one();
        aModRequestedHandle = allocator.one();
        aModBuildMethod = allocator.one();

        setCell(aModRequestedHandle, baseName + "_MODIFIED");
        setCell(aModBuildMethod, buildMethodWithParameters);
        String formula = String.format("=ModifyObj(%s,%s,%s,,%s,,%s)",
                                       aModRequestedHandle, aBaseHandle, enquote("pcurve"),
                                       enquote("buildmethod"), aModBuildMethod);
        setFormula(aModHandle, formula);
        show_value("ModHandle", aModHandle);
    }

    public void replyModifyBasePopulationObject(String reply) throws Exception
    {
        Map<String, String> mss = ScriptReply.keysFromReply(reply, new String[] { "ModHandle" });
        String modHandle = mss.get("ModHandle");
        if (!modHandle.startsWith(baseName + "_MODIFIED:"))
            throw new Exception("Osier failed to create modified object handle");
    }
    
    /* ============================================================================================= */
    
    public void oneArgFunction(String func, double[] x, boolean isIntegerX) throws Exception
    {
        if (aFunctionBlock == null)
            aFunctionBlock = allocator.block(2, Math.max(200, x.length));
        
        CellAddressRange aBlock = aFunctionBlock;
        CellAddress aBase = aBlock.upperLeft;
        
        int nonexec = 0;
        
        for (int k = 0; k < x.length; k++)
        {
            CellAddress aAge = aBase.offset(0, k);
            CellAddress aFunc = aBase.offset(1, k);

            String sx;
            if (isIntegerX)
            {
                int xx = toInteger(x[k]);
                setCell(aAge, xx);
                sx = "" + xx;
            }
            else
            {

                double xx = x[k];
                setCell(aAge, xx);
                sx = Util.f2s(xx);
            }

            String formula = String.format("=%s(%s,%s)",
                                           func, aModHandle, aAge);
            setFormula(aFunc, formula);

            String what = String.format("%s %s", func, sx);

            show_value(what, aFunc);
            nonexec++;

            if ((k % 100) == 0)
            {
                sbnl();
                sb.append(EXEC);
                nonexec = 0;
            }
        }
        
        if (nonexec != 0)
        {
            sbnl();
            sb.append(EXEC);
        }
    }

    public void twoArgFunction(String func, double[] x, boolean isIntegerX, int interval) throws Exception
    {
        if (aFunctionBlock == null)
            aFunctionBlock = allocator.block(2, Math.max(200, x.length));
        
        CellAddressRange aBlock = aFunctionBlock;
        CellAddress aBase = aBlock.upperLeft;
        
        int nonexec = 0;
        
        for (int k = 0; k < x.length; k++)
        {
            CellAddress aAge = aBase.offset(0, k);
            CellAddress aFunc = aBase.offset(1, k);

            String sx;
            if (isIntegerX)
            {
                int xx = toInteger(x[k]);
                setCell(aAge, xx);
                sx = "" + xx;
            }
            else
            {

                double xx = x[k];
                setCell(aAge, xx);
                sx = Util.f2s(xx);
            }

            String formula = String.format("=%s(%s,%s,%d)",
                                           func, aModHandle, aAge, interval);
            setFormula(aFunc, formula);

            String what = String.format("%s %s", func, sx);

            show_value(what, aFunc);
            nonexec++;

            if ((k % 100) == 0)
            {
                sbnl();
                sb.append(EXEC);
                nonexec = 0;
            }
        }
        
        if (nonexec != 0)
        {
            sbnl();
            sb.append(EXEC);
        }
    }
    
    /* ============================================================================================= */

    public void setCell(CellAddress ca, String value)
    {
        sbnl();
        sb.append(String.format("setCellText %s, %s" + nl,
                                enquote(ca.toString()),
                                enquote(escape(value))));
    }

    public void setCell(CellAddress ca, int value)
    {
        sbnl();
        sb.append(String.format("setCellInt %s, %s" + nl,
                                enquote(ca.toString()),
                                enquote(String.format("%d", value))));
    }

    public void setCell(CellAddress ca, double value)
    {
        sbnl();
        sb.append(String.format("setCellDouble %s, %s" + nl,
                                enquote(ca.toString()),
                                enquote(String.format("%f", value))));
    }

    public void setFormula(CellAddress ca, String formula)
    {
        sbnl();
        sb.append(String.format("setCellFormula %s, %s" + nl,
                                enquote(ca.toString()),
                                enquote(escape(formula))));
    }

    @SuppressWarnings("unused")
    private void selectCell(CellAddress ca)
    {
        sbnl();
        sb.append(String.format("set rng = wb.Activesheet.Range(\"%s\")" + nl, ca.toString()));
    }

    private String enquote(String s)
    {
        char quote = '"';
        return quote + s + quote;
    }

    private String escape(String s)
    {
        return s.replace("\"", "\"\"");
    }

    private void sbnl()
    {
        int len = sb.length();
        if (len != 0 && sb.charAt(len - 1) != '\n')
            sb.append(nl);
    }

    private int toInteger(double x)
    {
        return (int) Math.round(x);
    }
}
