package my;

public class LinearUtil
{
    public static class AB
    {
        double a;
        double b;
        
        public AB(double a, double b)
        {
            this.a = a;
            this.b = b;
        }
    }
    
    /*
     * Solve equations:
     * 
     *    a * xa1 + b * xb1 = c1
     *    a * xa2 + b * xb2 = c2
     */
    public static AB solve(double xa1, double xb1, double c1, double xa2, double xb2, double c2)
    {
        double b = (c2 * xa1 - c1 * xa2) / (xb2 * xa1 - xb1 * xa2);
        double a = (c1 - b * xb1) / xa1;
                
        return new AB(a, b);
    }
}
