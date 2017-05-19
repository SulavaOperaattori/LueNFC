package opiskelu.luenfc;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * Created by c6sila00 on 19.5.2017.
 */

public class SigningActivity extends AsyncTask<String, Void, String> {
    private TextView statusField,roleField;
    private Context context;
    private int byGetOrPost = 0;

    public SigningActivity(Context context, TextView statusField, TextView roleField, int flag) {
        this.context = context;
        this.statusField = statusField;
        this.roleField = roleField;
        byGetOrPost = flag;
    }

    protected void onPreExecute(){
    }

    @Override
    protected String doInBackground(String... arg0) {
        if(byGetOrPost == 0){ //means by Get Method

            try{
                String id= (String)arg0[0];
                String link = "http://192.168.137.1/sqlandroidup.php?id_number="+id;

                URL url = new URL(link);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setDoInput(true); // Allow Inputs

                InputStream in = new BufferedInputStream(urlConnection.getInputStream());



                StringBuffer sb = new StringBuffer("");
                String line="";

                while ((line = in.readLine()) != null) {
                    sb.append(line);
                    break;
                }

                in.close();
                return sb.toString();
            } catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }
        } else{
            try{
                String username = (String)arg0[0];
                String password = (String)arg0[1];

                String link="http://myphpmysqlweb.hostei.com/loginpost.php";
                String data  = URLEncoder.encode("username", "UTF-8") + "=" +
                        URLEncoder.encode(username, "UTF-8");
                data += "&" + URLEncoder.encode("password", "UTF-8") + "=" +
                        URLEncoder.encode(password, "UTF-8");

                URL url = new URL(link);
                URLConnection conn = url.openConnection();

                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());

                wr.write( data );
                wr.flush();

                BufferedReader reader = new BufferedReader(new
                        InputStreamReader(conn.getInputStream()));

                StringBuilder sb = new StringBuilder();
                String line = null;

                // Read Server Response
                while((line = reader.readLine()) != null) {
                    sb.append(line);
                    break;
                }

                return sb.toString();
            } catch(Exception e){
                return new String("Exception: " + e.getMessage());
            }
        }
    }

    @Override
    protected void onPostExecute(String result){
        this.statusField.setText("Login Successful");
        this.roleField.setText(result);
}

//testplss4334
