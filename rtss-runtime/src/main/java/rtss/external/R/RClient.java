package rtss.external.R;

import java.net.URL;

import org.springframework.web.client.RestTemplate;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import rtss.config.Config;
import rtss.types.T2;

/**
 * Client-side stub to execute remote invocation to RServer (and execute R programs there)
 */
public class RClient implements RCall
{
    @Override
    public String execute(String s, boolean reuse) throws Exception
    {
        RestTemplate restTemplate = makeRestTemplate();
        String url = url("R/execute");
        T2<String,Boolean> x = new T2<String,Boolean>(s, reuse);
        return restTemplate.postForObject(url, x, String.class);
    }

    @Override
    public void stop() throws Exception
    {
        RestTemplate restTemplate = makeRestTemplate();
        String url = url("R/stop");
        restTemplate.getForObject(url, Void.class);
    }

    @Override
    public String ping(String tag) throws Exception
    {
        RestTemplate restTemplate = makeRestTemplate();
        String url = String.format("%s?tag={tag}", url("R/ping"));
        return restTemplate.getForObject(url, String.class, tag);
    }
    
    private String url(String uri) throws Exception
    {
        URL url = new URL(Config.asRequiredString("R.server.endpoint"));
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
