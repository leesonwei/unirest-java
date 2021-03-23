package kong.unirest.apache.extend;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

/**
 * @program: retrofit-spring-boot-starter
 * @author: LIZONG.WEI
 * @since:
 * @Create on 2021-03-23
 **/
public class HttpGetWithBody extends HttpEntityEnclosingRequestBase {
    public final static String METHOD_NAME = "GET";

    public HttpGetWithBody() {
        super();
    }

    public HttpGetWithBody(final URI uri) {
        super();
        setURI(uri);
    }

    /**
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpGetWithBody(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
