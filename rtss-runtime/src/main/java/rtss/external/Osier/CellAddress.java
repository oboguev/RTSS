package rtss.external.Osier;

/**
 * Spreadsheet cell address
 */
public class CellAddress
{
    public char col;
    public int row;
    
    public CellAddress(char col, int row)
    {
        this.col = col;
        this.row = row;
    }
    
    public CellAddress(CellAddress x)
    {
        this.col = x.col;
        this.row = x.row;
    }

    public String toString()
    {
        String s = "";

        if (col == '\0')
            s += "?";
        else 
            s += col;
        
        s += row;
        
        return s;
    }
    
    public CellAddress offset(int dx, int dy)
    {
        CellAddress ca = new CellAddress(this);
        this.col += dy;
        this.row += dx;
        return ca;
    }
}
