import org.json.JSONException;
import org.json.JSONObject;
import sun.net.www.protocol.http.HttpURLConnection;
import java.io.*;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by sivan.k on 8/9/2015.
 */
public class ControllerSivan
{
    static String SERVER_URL= "http://smartborest.azurewebsites.net/api/";
    static HttpURLConnection urlConnection;

    public static void register(String address, String password, String paymentMethod, String deviceIp,
                                String phone, String email, String currency, String language, String name,
                                String login, String country, String platformId,String firstName, String lastName)
    {
        Map<String,String> mapString= new HashMap<String,String>();
        mapString.put("Address",address);
        String passMD5=getMD5(password);
        mapString.put("Password",passMD5);
        mapString.put("PaymentMethod",paymentMethod);
        mapString.put("Device_ip",deviceIp);
        mapString.put("Phone",phone);
        mapString.put("Email",email);
        mapString.put("Currency",currency);
        mapString.put("Language",language);
        mapString.put("Name",name);
        mapString.put("Login",login);
        mapString.put("Country",country);
        mapString.put("PlatformID",platformId);
        mapString.put("FirstName",firstName);
        mapString.put("LastName",lastName);

//        if(mapString.get(1)==null | mapString.get(5)==null | mapString.get(9)==null)
//        {
//            System.out.print("One of the parameters (login, email , pass) are null");
//        }

        JSONObject jsonObject=createJsonObject(mapString);

        String urlMethod="Register";
        JSONObject response= sendPostRequest(jsonObject,urlMethod);
        System.out.println(response);
    }

    public static void login(String login, String password, String platformID)
    {
        Map<String,String> mapString= new HashMap<String,String>();
        mapString.put("Login",login);
        String passMD5=getMD5(password);
        mapString.put("Password",passMD5);
        mapString.put("PlatformID",platformID);
        JSONObject jsonObject=createJsonObject(mapString);
        String urlMethod="Login";
        JSONObject response= sendPostRequest(jsonObject,urlMethod);
        System.out.println(response);
    }

    public static void transaction(String transactionID,String clientId, String ipAdress, String lottoAction, String lattitude,
                                   String longitude, String platformID, String webPageUrl, String function, String details, String amount)
    {
        Map<String,String> mapString= new HashMap<String,String>();
        mapString.put("TransactionID",transactionID);
        mapString.put("ClientID",clientId);
        mapString.put("IPAddress",ipAdress);
        mapString.put("LottoAction",lottoAction);
        mapString.put("Lattitude",lattitude);
        mapString.put("Longitude",longitude);
        mapString.put("PlatformID",platformID);
        mapString.put("WebPageURL",webPageUrl);
        mapString.put("Function",function);
        mapString.put("Details",details);
        mapString.put("Amount",amount);
        JSONObject jsonObject=createJsonObject(mapString);
        String urlMethod="Transaction";
        JSONObject response= sendPostRequest(jsonObject,urlMethod);
        System.out.print(response);
    }

    public static JSONObject createJsonObject(Map<String,String> mapString)
    {
        JSONObject jsonObject = new JSONObject();

        try
        {
            for (Map.Entry<String, String> entry : mapString.entrySet())
            {
                jsonObject.put(entry.getKey(), entry.getValue());
            }
        }
        catch(JSONException e)
        {
            e.printStackTrace();
        }
        return jsonObject;
    }


    //Sends post request to server
    public static JSONObject sendPostRequest(JSONObject jsonObject, String urlMethod)
    {
        JSONObject response=null;
        try
        {
            getConnection(urlMethod);

            DataOutputStream out = new DataOutputStream(urlConnection.getOutputStream());
            out.writeBytes(jsonObject.toString());

            System.out.print((urlConnection.getResponseCode()));

            if(urlConnection.getResponseCode()== 200)
            {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(),"UTF-8"),8);
                StringBuilder stringBuilder = new StringBuilder();
                String msg;
                while ((msg = bufferedReader.readLine()) != null)
                {
                    stringBuilder.append(msg);
                }
                response=new JSONObject(stringBuilder.toString());
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        finally
        {
            urlConnection.disconnect();
        }
        return response;
    }

    //Get connection to server
    public static void getConnection(String urlMethod)
    {
        try
        {
            URL url = new URL (SERVER_URL+urlMethod);
            urlConnection=(HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setRequestProperty("Content-Type", "application/json");
            urlConnection.setDoOutput(true);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static String getMD5(String input)
    {
        String hashtext=null;
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger number = new BigInteger(1, messageDigest);
            hashtext = number.toString(16);
            // Now we need to zero pad it if you actually want the full 32 chars.
            while (hashtext.length() < 32)
            {
                hashtext = "0" + hashtext;
            }
        }
        catch(NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        return hashtext;
    }
}

