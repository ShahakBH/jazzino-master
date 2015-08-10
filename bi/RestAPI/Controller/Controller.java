import com.google.gson.Gson;
import com.sun.deploy.net.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONObject;
import sun.net.www.http.HttpClient;

import javax.validation.Path;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by shahak.bh on 09/08/2015.
 */

    public class Controller {

    public static JSONObject getJsonMap(String address, String password, String paymentMethod, String device_ip, String phone,
                            String email, String currency, String language, String name, String login,
                            String country, String platformId, String firstName,String lastName)
    {
        Map <String, String > jsonmap=new HashMap<String, String>();
        jsonmap.put("Address", address);
        jsonmap.put("Password", password);
        jsonmap.put("PaymentMethod", paymentMethod);
        jsonmap.put("Device_ip",device_ip);
        jsonmap.put("Phone" , phone);
        jsonmap.put("Email", email);
        jsonmap.put("Currency", currency);
        jsonmap.put("Language" , language);
        jsonmap.put("Name",name);
        jsonmap.put("Login", login);
        jsonmap.put("Country", country);
        jsonmap.put("PlatformID", platformId);
        jsonmap.put("FirstName", firstName);
        jsonmap.put("LastName", lastName);
        JSONObject json = new JSONObject(jsonmap);
        String postUrl="http://smartborest.azurewebsites.net/api/Register";// put in your url
        Gson gson= new Gson();
        HttpPost post = new HttpPost(postUrl);
        StringEntity  postingString = null;//convert your pojo to   json
        try {
            postingString = new StringEntity(gson.toJson(jsonmap));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");
//        HttpResponse  response = HttpClient.execute(post);
        return json;


    }
    public static JSONObject register (Map getJsonMap)
    {
        if (getJsonMap.get("Password")==null|| getJsonMap.get("Email")==null|| getJsonMap.get("Login")==null)
        {
            System.out.println("Password, Email or Login cannot be null, Please fill the fields");

        }
        JSONObject obj=new JSONObject(getJsonMap);
        return obj;
    }
}
