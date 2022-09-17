package cn.sunist.project.bililivedanma.service;

import com.google.gson.Gson;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Client {
    private final CloseableHttpClient client = HttpClients.createDefault();

    /**
     * Convert Object to any class
     * @param value the object to convert
     * @return the converted object
     * @param <T> target class
     * @author sunist-c
     */
    @SuppressWarnings("unchecked")
    public static <T> T Convert(Object value) {
        try {
            return (T) value;
        } catch (ClassCastException e) {
            return null;
        }
    }

    /**
     * Executes a post request via http client
     * @param url the url to post
     * @param requestBody the body of the request
     * @param accessToken the access token
     * @return execution result, jsonString
     * @throws Exception exception
     * @author sunist-c
     */
    public String DoPostRequest(String url, Object requestBody, String accessToken) throws Exception{
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Accept", "application/json");
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.addHeader("Authorization", "Bearer " + accessToken);

        Gson gson = new Gson();
        String params = gson.toJson(requestBody);

        String charSet = "UTF-8";
        StringEntity entity = new StringEntity(params, charSet);
        httpPost.setEntity(entity);

        try {
            CloseableHttpResponse response = client.execute(httpPost);
            StatusLine status = response.getStatusLine();
            if (status.getStatusCode() != 200) {
                System.out.println("bad status code: " + status.getStatusCode());
                return null;
            }

            InputStream is = response.getEntity().getContent();
            byte[] bytes = is.readAllBytes();
            is.close();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public CloseableHttpClient getHttpClient() {
        return client;
    }
}
