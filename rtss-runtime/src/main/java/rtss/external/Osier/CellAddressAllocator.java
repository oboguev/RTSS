package rtss.external.Osier;

/**
 * Allocate a range of spreadsheet cell addresses
 */
public class CellAddressAllocator
{
    private int next_row = 1;
    
    public CellAddress one()
    {
        return new CellAddress('A', next_row++);
    }

    public CellAddressRange horizontal(int size)
    {
        CellAddressRange car = new CellAddressRange();
        car.upperLeft = new CellAddress('A', next_row++);
        car.bottomRight = car.upperLeft.offset(size - 1, 0);
        return car;
    }

    public CellAddressRange vertical(int size)
    {
        CellAddressRange car = new CellAddressRange();
        car.upperLeft = new CellAddress('A', next_row++);
        car.bottomRight = car.upperLeft.offset(0, size - 1);
        next_row = car.bottomRight.row + 1;
        return car;
    }

    public CellAddressRange block(int xsize, int ysize)
    {
        CellAddressRange car = new CellAddressRange();
        car.upperLeft = new CellAddress('A', next_row++);
        car.bottomRight = car.upperLeft.offset(xsize - 1, ysize - 1);
        next_row = car.bottomRight.row + 1;
        return car;
    }
}
