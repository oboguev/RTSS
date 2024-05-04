package rtss.external.Osier;

import rtss.data.bin.Bin;

public class OsierScript
{
    private StringBuilder sb = new StringBuilder();
    private static final String nl = "\n";
    private CellAddressAllocator allocator = new CellAddressAllocator();
    
    private CellAddress aBaseHandle;
    private CellAddress aBaseName;
    private CellAddress aBaseObjectType;
    private CellAddressRange aBaseBodyProps;
    private CellAddressRange aBaseBodyValues;
    private CellAddress aBaseTableName;
    private CellAddressRange aBaseTableCols;
    private CellAddressRange aBaseTableValues;
    
    public String getScript()
    {
        return sb.toString();
    }
    
    public void newScript()
    {
        sb.setLength(0);
    }

    public void clearSheet()
    {
        sb.append("clear-sheet" + nl);
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
        
        sb.append(nl);
        sb.append("#" + nl);
        sb.append("# create base object" + nl);
        sb.append("#" + nl);
        sb.append(nl);
        
        setCell(aBaseName, baseName);
        setCell(aBaseObjectType, "MORTALITY");
        
        setCell(aBaseBodyProps.upperLeft, "Population");
        setCell(aBaseBodyValues.upperLeft, "XXX-M");
        
        setCell(aBaseBodyProps.upperLeft.offset(0, 1), "Date");
        setCell(aBaseBodyValues.upperLeft.offset(0, 1), "20110601");
        
        setCell(aBaseBodyProps.upperLeft.offset(0, 2), "BuildMethod");
        setCell(aBaseBodyValues.upperLeft.offset(0, 2), "HYBRID_FORCE");
        
        setCell(aBaseTableName , "Death Rates");
        
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
        
        // call return-value-cell function-name args...
        sb.append("call");
        sb.append(" " + aBaseHandle);
        sb.append(" " + "CreateObj");
        sb.append(" " + aBaseName);
        sb.append(" " + aBaseObjectType);
        sb.append(" " + aBaseBodyProps);
        sb.append(" " + aBaseBodyValues);
        sb.append(" " + aBaseTableName);
        sb.append(" " + aBaseTableCols);
        sb.append(" " + aBaseTableValues);
        sb.append(nl);
    }
    
    public void setCell(CellAddress ca, String value)
    {
        sb.append(String.format("set-cell-string %s %s" + nl, ca.toString(), value));
    }

    public void setCell(CellAddress ca, int value)
    {
        sb.append(String.format("set-cell-integer %s %d" + nl, ca.toString(), value));
    }

    public void setCell(CellAddress ca, double value)
    {
        sb.append(String.format("set-cell-double %s %f" + nl, ca.toString(), value));
    }
}
