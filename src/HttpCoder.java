import org.apache.http.Consts;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class HttpCoder implements AutoCloseable {

    static private final boolean DEBUG = true;

    static private final Pattern csrfPattern = Pattern.compile("data-csrf='(.*)'");

    private CookieStore cookieStore;
    private RequestConfig requestConfig = RequestConfig.custom()
            .setCookieSpec(CookieSpecs.DEFAULT)
            .build();
    private HttpClientContext context = HttpClientContext.create();
    private String cookiePath = "/home/smart/working/cookies.ser";
    private String handle = "";
    private boolean logged;
    private UserInfo self;

    HttpCoder() throws IOException {
        loadCookies();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setDefaultCookieStore(cookieStore)
                .build();
        HttpResponse response = httpClient.execute(
                new HttpGet("http://codeforces.com/"), context);
        String html = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        if (logged = !html.contains("<a href=\"/register\">Register</a>")) {
            Document doc = Jsoup.parse(html);
            handle = doc.select("div.avatar > a").first().attr("href").substring(9);
            self = UserInfo.fromServer(handle);
            if (DEBUG)
                System.out.printf("Logged in as %s.\n", handle);
        } else if (DEBUG) {
            System.out.println("Not logged in.");
            self = UserInfo.anonymous();
        }
        saveCookies();
    }

    String getHandle() {
        return handle;
    }

    void setHandle(String handle) {
        this.handle = handle;
    }

    private void setCookiePath(String cookiePath) {
        this.cookiePath = cookiePath;
    }

    private void loadCookies() {
        if (!Files.exists(Paths.get(cookiePath))) {
            cookieStore = new BasicCookieStore();
        } else {
            try (FileInputStream fileInputStream = new FileInputStream(cookiePath)) {
                ObjectInputStream inputStream = new ObjectInputStream(fileInputStream);
                cookieStore = (BasicCookieStore) inputStream.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        context.setCookieStore(cookieStore);
        if (DEBUG)
            System.out.println("init cookie done.");
    }

    private void saveCookies() {
        if (cookieStore != null) {
            try (FileOutputStream fileOutputStream = new FileOutputStream(cookiePath)) {
                ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream);
                outputStream.writeObject(cookieStore);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void clearCookies() {
        cookieStore = new BasicCookieStore();
        context.setCookieStore(cookieStore);
        logged = false;
        saveCookies();
    }

    void loginAs(String handle, String password) throws IOException {
        if (logged)
            clearCookies();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setDefaultCookieStore(cookieStore)
                .build();
        CloseableHttpResponse response = httpClient.execute(
                new HttpGet("http://codeforces.com/"), context);
        assert response.getStatusLine().getStatusCode() == 200;
        String html = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        String csrf = findCsrf(html);
        assert !csrf.equals("");
        HttpPost httpPost = new HttpPost("http://codeforces.com/enter");
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("csrf_token", csrf));
        nameValuePairs.add(new BasicNameValuePair("handle", handle));
        nameValuePairs.add(new BasicNameValuePair("password", password));
        nameValuePairs.add(new BasicNameValuePair("action", "enter"));
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8));
        response = httpClient.execute(httpPost, context);
        System.out.println("enter: " + response.getStatusLine());
        assert response.getStatusLine().getStatusCode() == 302;
        logged = true;
    }

    void submitSource(String problem, String source) throws IOException {
        if (!logged) {
            throw new NotImplementedException();
        }
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setDefaultCookieStore(cookieStore)
                .build();
        CloseableHttpResponse response = httpClient.execute(
                new HttpGet("http://codeforces.com/problemset/submit"), context);
        assert response.getStatusLine().getStatusCode() == 200;
        String html = EntityUtils.toString(response.getEntity());
        EntityUtils.consume(response.getEntity());
        String csrf = findCsrf(html);
        HttpPost httpPost = new HttpPost(
                "http://codeforces.com/problemset/submit?csrf_token=" + csrf);
        List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("csrf_token", csrf));
        nameValuePairs.add(new BasicNameValuePair(
                "action", "submitSolutionFormSubmitted"));
        nameValuePairs.add(new BasicNameValuePair("submittedProblemCode", problem));
        nameValuePairs.add(new BasicNameValuePair("programTypeId", "31"));
        nameValuePairs.add(new BasicNameValuePair("source", source));
        httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs, Consts.UTF_8));
        response = httpClient.execute(httpPost, context);
        System.out.println("submit: " + response.getStatusLine());
        assert response.getStatusLine().getStatusCode() == 302;
    }

    /* TODO: logout method. */
    //void logout() {throw new NotImplementedException();}

    @Override
    public void close() throws Exception {
        saveCookies();
    }

    boolean isLogged() {
        return logged;
    }

    private String findCsrf(String html) {
        Matcher matcher = csrfPattern.matcher(html);
        return matcher.find() ? matcher.group(1) : "";
    }
}
