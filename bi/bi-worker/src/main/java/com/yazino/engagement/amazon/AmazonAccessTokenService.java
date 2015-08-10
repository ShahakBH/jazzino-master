package com.yazino.engagement.amazon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

@Service
public class AmazonAccessTokenService {

    public static final String UTF_8 = "UTF-8";
    public static final String AMAZON_URL = "https://api.amazon.com/auth/O2/token";

    @SuppressWarnings("unchecked")
    public AmazonAccessToken getAuthToken(String clientId, String clientSecret) throws Exception {
        // Encode the body of your request, including your clientID and clientSecret values.
        String body = String.format("grant_type=%s&scope=%s&client_id=%s&client_secret=%s",
                URLEncoder.encode("client_credentials", UTF_8),
                URLEncoder.encode("messaging:push", UTF_8),
                URLEncoder.encode(clientId, UTF_8),
                URLEncoder.encode(clientSecret, UTF_8));

        // Create a new URL object with the base URL for the access token request.
        URL authUrl = new URL(AMAZON_URL);

        // Generate the HTTPS connection. You cannot make a connection over HTTP.
        HttpsURLConnection con = (HttpsURLConnection) authUrl.openConnection();
        con.setDoOutput(true);
        con.setRequestMethod("POST");

        // Set the Content-Type header.
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setRequestProperty("Charset", UTF_8);
        // Send the encoded parameters on the connection.
        OutputStream os = con.getOutputStream();
        os.write(body.getBytes(UTF_8));
        os.flush();
        con.connect();

        // Convert the response into a String object.
        String responseContent = parseResponse(con.getInputStream());

        // Create a new JSONObject to hold the access token and extract
        // the token from the response.
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JodaModule());
        Map<String, Object> map = mapper.readValue(responseContent, Map.class);

        return getAmazonAccessTokenFromMap(map);
    }

    private AmazonAccessToken getAmazonAccessTokenFromMap(final Map<String, Object> map) {
        String accessToken = (String) map.get("access_token");
        Integer expireIn = (Integer) map.get("expires_in");
        DateTime expireTime = new DateTime().plusSeconds(expireIn);
        String scope = (String) map.get("scope");
        String tokenType = (String) map.get("token_type");

        return new AmazonAccessToken(accessToken, expireTime, scope, tokenType);
    }

    private String parseResponse(InputStream in) throws Exception {
        InputStreamReader inputStream = new InputStreamReader(in, UTF_8);
        BufferedReader buff = new BufferedReader(inputStream);

        StringBuilder sb = new StringBuilder();
        String line = buff.readLine();
        while (line != null) {
            sb.append(line);
            line = buff.readLine();
        }

        return sb.toString();
    }
}
