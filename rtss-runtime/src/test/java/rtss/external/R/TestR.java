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
        ping("444");
        ping("555", false);
        ping("666");
        ping("777");
    }
    
    private void ping(String tag) throws Exception
    {
        ping(tag, true);
    }
    
    private void ping(String tag, boolean reuse) throws Exception
    {
        String script = Script.script("r-scripts/test.r", "arg", tag);
        String reply = R.execute(script, reuse);
        if (!reply.equals(String.format("%s-1\n%s-2", tag, tag)))
            throw new Exception("R reply was unexpected: " + tag);
        Util.out("Received expected R response for tag " + tag);
    }
}
