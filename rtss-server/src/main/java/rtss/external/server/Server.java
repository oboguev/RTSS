package rtss.external.server;

import java.net.URL;

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
            URL url = new URL(Config.asRequiredString("server.endpoint"));
            int port = url.getPort();
            if (port == -1)
                port = url.getDefaultPort();
            if (port == -1)
                throw new Exception("Server port is not defined in configuration property server.endpoint");
            
            System.setProperty("server.port", "" + port);
            SpringApplication.run(Server.class, args);
        }
        catch (Throwable ex)
        {
            Util.err("Exception: " + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }
}
