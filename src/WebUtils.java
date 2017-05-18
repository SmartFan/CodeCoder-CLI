import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

class WebUtils {
    static String getString(String urlPath) throws IOException {
        return getString(new URL(urlPath));
    }

    static String getString(URL url) throws IOException {
        InputStream inputStream = url.openConnection().getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String text;
        StringBuilder stringBuilder = new StringBuilder();
        while ((text = reader.readLine()) != null)
            stringBuilder.append(text);
        reader.close();
        return stringBuilder.toString();
    }
}
