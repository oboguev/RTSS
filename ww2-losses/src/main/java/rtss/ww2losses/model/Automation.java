package rtss.ww2losses.model;

public class Automation
{
    private static boolean isAutomated = false;
    
    public static boolean isAutomated()
    {
        return isAutomated;
    }
    
    public static void setAutomated(boolean v)
    {
        isAutomated = v;
    }
}
