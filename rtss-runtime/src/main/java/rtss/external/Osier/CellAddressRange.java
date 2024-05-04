package rtss.external.Osier;

/**
 * Spreadsheet cell range address
 */
public class CellAddressRange
{
    public CellAddress upperLeft;
    public CellAddress bottomRight;
    
    public CellAddressRange()
    {
    }

    public CellAddressRange(CellAddressRange x)
    {
        upperLeft = new CellAddress(x.upperLeft);
        bottomRight = new CellAddress(x.bottomRight);
    }
    
    public CellAddressRange offset(int dx, int dy)
    {
        CellAddressRange car = new CellAddressRange();
        car.upperLeft = upperLeft.offset(dx, dy);
        car.bottomRight = bottomRight .offset(dx, dy);
        return car;
    }
    
    public String toString() 
    {
        return upperLeft.toString() + ":" + bottomRight.toString();
    }
}
