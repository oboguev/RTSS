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
        if (!R.ping("000").equals("000"))
            throw new Exception("Server ping failed");
        
        rping("111");
        rping("222");
        rping("333");
        
        R.stop();
        
        rping("444");
        rping("555", false);
        rping("666");
        rping("777");
        
        R.stop();
    }
    
    private void rping(String tag) throws Exception
    {
        rping(tag, true);
    }
    
    private void rping(String tag, boolean reuse) throws Exception
    {
        String script = Script.script("r-scripts/test.r", "arg", tag);
        String reply = R.execute(script, reuse);
        if (!reply.equals(String.format("%s-1\n%s-2", tag, tag)))
            throw new Exception("R reply was unexpected: " + tag);
        Util.out("Received expected R response for tag " + tag);
    }
}
