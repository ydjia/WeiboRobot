package weibo.service;

/**
 * Created with IntelliJ IDEA.
 * User: jia
 * Date: 13-7-10
 * Time: 下午1:25
 * To change this template use File | Settings | File Templates.
 */
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.impl.cookie.BrowserCompatSpec;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

public class HttpConnectionManager {

    public static HttpClient getHttpClient() {
        HttpParams params = new BasicHttpParams();
         /* 连接超时 */
        HttpConnectionParams.setConnectionTimeout(params, 3000);
         /* 请求超时 */
        HttpConnectionParams.setSoTimeout(params, 10000);
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", 80, PlainSocketFactory
                .getSocketFactory()));
        schemeRegistry.register(new Scheme("https", 443, SSLSocketFactory
                .getSocketFactory()));

        PoolingClientConnectionManager cm = new PoolingClientConnectionManager(
                schemeRegistry);
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);
        // Increase max connections for localhost:80 to 50
        HttpHost localhost = new HttpHost("locahost", 80);
        cm.setMaxPerRoute(new HttpRoute(localhost), 50);

        DefaultHttpClient httpClient = new DefaultHttpClient(cm, params);
        // 自定义cookies*****************
        CookieSpecFactory csf = new CookieSpecFactory() {
            public CookieSpec newInstance(HttpParams params) {
                return new BrowserCompatSpec() {
                    @Override
                    public void validate(Cookie cookie, CookieOrigin origin)
                            throws MalformedCookieException {
                        // Oh, I am easy
                    }
                };
            }
        };
        httpClient.getCookieSpecs().register("easy", csf);
        httpClient.getParams().setParameter(ClientPNames.COOKIE_POLICY, "easy");
        // 自定义cookies*****************
        return httpClient;
    }

}
