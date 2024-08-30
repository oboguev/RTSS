package rtss.pre1917.util;

public class WeightedAverage
{
    private double value_weight_sum = 0;
    private double weight_sum = 0;
    int count = 0;
    
    public void reset()
    {
        value_weight_sum = 0;
        weight_sum = 0;
        count = 0;
    }
    
    public int count()
    {
        return count;
    }

    public void add(double value, double weight)
    {
        value_weight_sum += value * weight;
        weight_sum += weight;
        count++;
    }
    
    public Double doubleResult()
    {
        if (count == 0)
            return null;
        else
            return value_weight_sum / weight_sum; 
    }

    public Long longResult()
    {
        if (count == 0)
            return null;
        else
            return Math.round(value_weight_sum / weight_sum); 
    }
}
