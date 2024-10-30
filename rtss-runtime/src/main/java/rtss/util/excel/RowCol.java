package rtss.util.excel;

public class RowCol
{
    public final int row;
    public final int col;
    
    public RowCol(int row, int col)
    {
        this.row = row;
        this.col = col;
    }
    
    public String toString()
    {
        int c1 = 'A' + col;
        String excelColID = "";
        
        if (c1 <= 'Z')
        {
            excelColID += (char) c1;
        }
        else
        {
            c1 = col / 26;
            int c2 = col % 26;
            excelColID += (char) ('A' + c1 - 1);
            excelColID += (char) ('A' + c2);
        }
        
        return excelColID;
    }
}
