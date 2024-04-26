package rtss.external.R;

import rtss.util.Util;

public class TestR
{
    public static void main(String[] args)
    {
        try
        {
            new TestR().do_main();
            Util.out("*** Completed");
        }
        catch (Throwable ex)
        {
            ex.printStackTrace();
        }
    }
    
    private void do_main() throws Exception
    {
        ping("111");
        ping("222");
        ping("333");
        R.stop();
    }
    
    private void ping(String tag) throws Exception
    {
        String script = Script.script("r-scripts/test.r", "arg", tag);
        String reply = R.execute(script, true);
        if (!reply.equals(tag))
            throw new Exception("R reply was unexpected: " + tag);
        Util.out("Received expected R response: " + tag);
    }
}
