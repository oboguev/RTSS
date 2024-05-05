package rtss.external.Osier;

import java.util.Map;

import rtss.data.bin.Bin;
import rtss.external.Script;
import rtss.external.ScriptReply;
import rtss.util.Util;

public class OsierScript
{
    private StringBuilder sb = new StringBuilder();
    private static final String nl = "\n";
    public static final String EXEC = "'---execute---\n";
    private CellAddressAllocator allocator = new CellAddressAllocator();

    private CellAddress aBaseHandle;
    private CellAddress aBaseName;
    private CellAddress aBaseObjectType;
    private CellAddressRange aBaseBodyProps;
    private CellAddressRange aBaseBodyValues;
    private CellAddress aBaseTableName;
    private CellAddressRange aBaseTableCols;
    private CellAddressRange aBaseTableValues;

    public String getScript() throws Exception
    {
        String sc = sb.toString();
        if (Util.lastchar(sc) != '\n')
            sc += nl;
        sc += EXEC;
        return sc;
    }

    public void newScript()
    {
        sb.setLength(0);
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
        
        say("test-002");

        sb.append(nl);
        sb.append("'" + nl);
        sb.append("' create base object" + nl);
        sb.append("'" + nl);
        sb.append(nl);

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
        
        say("test-003");
        
        selectCell(aBaseHandle);
        String formula = String.format("=CreateObj(%s,%s,%s,%s,%s,%s,%s)",
                                       aBaseName, aBaseObjectType, aBaseBodyProps, aBaseBodyValues,
                                       aBaseTableName, aBaseTableCols, aBaseTableValues);
        sb.append(String.format("[rng].Formula = \"%s\"" + nl, escape(formula)));
        
        show_value("BaseHandle", aBaseHandle);
    }
    
    public void replyBaseMortalityObject(String reply) throws Exception
    {
        Map<String,String> mss = ScriptReply.keysFromReply(reply, new String[] {"BaseHandle"});
        String baseHandle = mss.get("BaseHandle");
        if (!baseHandle.startsWith("XXX:"))
            throw new Exception("Osier failed to create base handle");
    }

    public void setCell(CellAddress ca, String value)
    {
        selectCell(ca);
        sb.append(String.format("[rng].Value = \"%s\"" + nl, escape(value)));
    }

    public void setCell(CellAddress ca, int value)
    {
        selectCell(ca);
        sb.append(String.format("[rng].Value = \"%d\"" + nl, value));
        sb.append(String.format("[rng].NumberFormat = \"%s\"" + nl, "0"));
    }

    public void setCell(CellAddress ca, double value)
    {
        selectCell(ca);
        sb.append(String.format("[rng].Value = \"%f\"" + nl, value));
        sb.append(String.format("[rng].NumberFormat = \"%s\"" + nl, "0.0000"));
    }

    private void selectCell(CellAddress ca)
    {
        sbnl();
        sb.append(nl);
        sb.append(String.format("set rng = wb.Activesheet.Range(\"%s\")" + nl, ca.toString()));
    }
    
    private String escape(String s)
    {
        return s.replace("\"", "\\\"");
    }

    private void sbnl()
    {
        int len = sb.length();
        if (len != 0 && sb.charAt(len - 1) != '\n')
            sb.append(nl);
    }

    public void start(boolean visible) throws Exception
    {
        sbnl();
        sb.append(Script.script("osier-excel/start-excel.vbs", "visible", visible ? "true" : "false"));
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
        selectCell(ca);
        sb.append(String.format("say \"%s: \" & CStr([rng].Value)" + nl, what));
        sb.append(EXEC);
    }
}
