package rtss.external.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import rtss.config.Config;
import rtss.util.Util;

@SpringBootApplication
public class Server
{
    public static void main(String[] args)
    {
        try
        {
            System.setProperty("server.port", "" + Config.asRequiredInteger("R.server.port"));
            SpringApplication.run(Server.class, args);
        }
        catch (Throwable ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
}
