package rtss.mexico.agri.calc;

import rtss.mexico.agri.entities.CultureSet;
import rtss.util.Util;

public class EvalCalories
{
    public static void main(String[] args)
    {
        try
        {
            new EvalCalories().eval();
            Util.out("** Done");
        }
        catch (Exception ex)
        {
            Util.err("** Exception");
            ex.printStackTrace();
        }
    }
    
    private void eval() throws Exception
    {
        CultureSet cs = new MergeCultureSets().merge();
        new Preprocess().preprocess(cs);
    }
}
