package rtss.external.Osier;

import java.net.URL;

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import rtss.config.Config;
import rtss.types.T2;

/**
 * Client-side stub to execute remote invocation to Osier server (and execute Osier commands there)
 */
public class OsierClient implements OsierCall
{
    private boolean definedStartupScript = false;
    
    @Override
    public void setDefaultStartupScript(boolean visible) throws Exception
    {
        setStartupScript(OsierScript.getDefaultStartupScript(visible));
    }
    
    @Override
    public void setStartupScript(String sc) throws Exception
    {
        RestTemplate restTemplate = makeRestTemplate();
        String url = url("Osier/setStartupScript");
        String res = restTemplate.postForObject(url, sc, String.class);
        if (!res.equals("OK"))
            throw new Exception("Failed to set startup script");
        definedStartupScript = true;
    }
    
    @Override
    public String execute(String s, boolean reuse) throws Exception
    {
        if (!definedStartupScript)
        {
            boolean visible = Config.asBoolean("Osier.excel.visible", false);
            setDefaultStartupScript(visible);
        }
        
        RestTemplate restTemplate = makeRestTemplate();
        String url = url("Osier/execute");
        T2<String,Boolean> x = new T2<String,Boolean>(s, reuse);
        return restTemplate.postForObject(url, x, String.class);
    }

    @Override
    public void stop() throws Exception
    {
        RestTemplate restTemplate = makeRestTemplate();
        String url = url("Osier/stop");
        restTemplate.getForObject(url, Void.class);
    }

    @Override
    public String ping(String tag) throws Exception
    {
        RestTemplate restTemplate = makeRestTemplate();
        String url = String.format("%s?tag={tag}", url("Osier/ping"));
        return restTemplate.getForObject(url, String.class, tag);
    }
    
    public void enableLocalLog(boolean log)
    {
        // this.log = log;
    }
    
    private String url(String uri) throws Exception
    {
        URL url = new URL(Config.asRequiredString("Osier.server.endpoint"));
        url = new URL(url, uri);
        return url.toString();
    }

    private RestTemplate makeRestTemplate()
    {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        return restTemplate;
    }
}
