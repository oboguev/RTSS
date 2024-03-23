package rtss.math.interpolate;

import static java.lang.Math.abs;

import rtss.data.bin.Bin;
import rtss.data.bin.Bins;

/**
 * Defines a precision to which an iterative process should be performed
 */
public class TargetPrecision
{
    public Double eachBinAbsoluteDifference;
    public Double eachBinRelativeDifference;

    public TargetPrecision()
    {
    }

    public TargetPrecision eachBinAbsoluteDifference(Double eachBinAbsoluteDifference)
    {
        this.eachBinAbsoluteDifference = eachBinAbsoluteDifference;
        return this;
    }

    public TargetPrecision eachBinRelativeDifference(Double eachBinRelativeDifference)
    {
        this.eachBinRelativeDifference = eachBinRelativeDifference;
        return this;
    }

    public boolean achieved(Bin[] bins, double[] yy)
    {
        return achieved(Bins.midpoint_y(bins), yy);
    }

    public boolean achieved(double[] bins, double[] yy)
    {
        if (eachBinAbsoluteDifference != null)
        {
            for (int k = 0; k < yy.length; k++)
            {
                if (abs(bins[k] - yy[k]) > eachBinAbsoluteDifference)
                    return false;
            }
        }

        if (eachBinRelativeDifference != null)
        {
            for (int k = 0; k < yy.length; k++)
            {
                double diff = abs(bins[k] - yy[k]) / abs(bins[k]);

                if (diff > eachBinRelativeDifference)
                    return false;
            }
        }

        return true;
    }
}
