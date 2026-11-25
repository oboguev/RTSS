package rtss.pre1917.eval.regions;

import java.util.Properties;

import rtss.util.Util;

/*
 * Вычисления для Черноморской губернии
 */
public class Chernomor
{
    private static Long population_1896 = null;

    public static synchronized long population_1896() throws Exception
    {
        if (population_1896 == null)
        {
            Properties p = Util.loadProperties("pre1917.props");
            String s = p.getProperty("Черноморская.1896");
            s = Util.despace(s).replace(",", "");
            population_1896 = Long.parseLong(s);
        }

        return population_1896;
    }
}
