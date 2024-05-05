package rtss.external;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

import rtss.util.Util;

public class ProcessRunner
{
    protected Process process;
    protected InputStream is; // connected to process stdout + stderr
    protected OutputStream os; // connected to process stdin
    protected BufferedReader reader;
    protected StopProcessHook stopHook;
    
    protected void start(String command) throws Exception
    {
        command = Util.despace(command);
        ProcessBuilder pb = new ProcessBuilder(command.split(" ")).redirectErrorStream(true);
        process = pb.start();
        stopHook = new StopProcessHook(process);
        stopHook.addHook();
        is = process.getInputStream();
        reader = new BufferedReader(new InputStreamReader(is));
        os = process.getOutputStream();
    }    
    
    public void stop() throws Exception
    {
        safeClose(reader);
        reader = null;

        safeClose(is);
        is = null;

        safeClose(os);
        os = null;

        if (process != null)
        {
            destroy(process);
            process = null;
        }

        if (stopHook != null)
        {
            stopHook.removeHook();
            stopHook = null;
        }
    }

    private void safeClose(Closeable c)
    {
        try
        {
            if (c != null)
                c.close();
        }
        catch (Throwable ex)
        {
            // ignore
        }
    }
    
    public void destroy(Process process)
    {
        List<ProcessHandle> descendants = process.toHandle().descendants().toList();
        
        process.destroy();
        try
        {
            process.waitFor(3, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex)
        {
            // ignore
        }
        
        for (ProcessHandle ph : descendants)
            ph.destroy();
    }

    public class StopProcessHook extends Thread
    {
        private Process process;

        public StopProcessHook(Process process)
        {
            this.process = process;
        }

        @Override
        public void run()
        {
            ProcessRunner.this.destroy(process);
            process.destroy();
        }

        public void addHook()
        {
            Runtime.getRuntime().addShutdownHook(this);
        }

        public void removeHook()
        {
            try
            {
                Runtime.getRuntime().removeShutdownHook(this);
            }
            catch (IllegalStateException ex)
            {
                // ignore, in shutdown already
            }
        }
    }
}
