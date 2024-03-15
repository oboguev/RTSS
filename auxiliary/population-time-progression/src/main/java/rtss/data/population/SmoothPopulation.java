package rtss.data.population;

public class SmoothPopulation
{
    public static double[] smooth(double[] d) throws Exception
    {
        return smooth(d, "ABC");
    }
    
    public static double[] smooth(double[] d, String phases) throws Exception
    {
        d = d.clone();
        
        if (phases.contains("A"))
        {
            /*
             * Smooth every 5-year point distributing its excess to 2 points before and 2 points after.
             * For 10-year points starting from age 30, distribute to 3 points before and 3 points after.
             */
            for (int k = 5; k < d.length; k +=5)
            {
                double excess10 = 0;
                boolean is10 = false;
                double excess5;
                
                if (k + 2 >= d.length)
                    break;

                if (k >= 30 && (k % 10) == 0 && k + 3 < d.length)
                {
                    is10 = true;
                    excess10 = d[k] - (d[k-3] + d[k-2] + d[k-1] + d[k+1] + d[k+2] + d[k+3]) / 6;
                }

                excess5 = d[k] - (d[k-2] + d[k-1] + d[k+1] + d[k+2]) / 4;
                
                if (is10 && excess10 > excess5)
                {
                    double excess = excess10;
                    if (excess > 0)
                    {
                        d[k] -= excess * (6.0/7.0);
                        d[k-3] += excess / 7;
                        d[k-2] += excess / 7;
                        d[k-1] += excess / 7;
                        d[k+1] += excess / 7;
                        d[k+2] += excess / 7;
                        d[k+3] += excess / 7;
                    }
                } 
                else
                {
                    double excess = excess5;
                    if (excess > 0)
                    {
                        d[k] -= excess * (4.0/5.0);
                        d[k-2] += excess / 5;
                        d[k-1] += excess / 5;
                        d[k+1] += excess / 5;
                        d[k+2] += excess / 5;
                    }
                }
            }
        }

        if (phases.contains("B"))
        {
            /*
             * For every odd age, average with two neighbors
             */
            for (int k = 1; k < d.length; k += 2)
                ave(d, k);
        }

        if (phases.contains("C"))
        {
            /*
             * For every even age, average with two neighbors
             */
            for (int k = 2; k < d.length; k += 2)
                ave(d, k);
        }

        return d;    
    }
    
    private static void ave(double[] d, int k)
    {
        if (k == 0 || k + 1 >= d.length)
            return;
        
        double av = (d[k-1] + d[k+1]) / 2;
        double excess = d[k] - av;

        d[k] -= excess * (2.0/3.0);
        d[k-1] += excess / 3;
        d[k+1] += excess / 3;
    }
}
