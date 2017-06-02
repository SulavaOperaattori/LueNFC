package opiskelu.luenfc;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Vector;

import static android.widget.Toast.LENGTH_SHORT;


public class MainActivity extends AppCompatActivity{

    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";

    private NfcAdapter mNfcAdapter;
    private TextView mTextView;
    private TextView WiFiStateTextView;

    private Button informationButton, uploadButton, downloadButton;
    private Vector<String> results;


    final String link = "http://www.oamk.fi/hankkeet/prinlab/equipment/index.php?page=";
    String infoLink;

    final String serverURL = "http://193.167.148.46/";
    String upLoadServerUri = null;

    int serverResponseCode = 0;
    //ProgressDialog dialog = null;
    DownloadManager mgr;
    //File file;
    final int REQUEST_WRITE_STORAGE = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        WiFiStateTextView = (TextView) findViewById(R.id.textView_WiFiState);
        Button WiFi = (Button)findViewById(R.id.WiFi);
        WiFi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
                checkWifiOnAndConnected();
            }
        });
      
        results = new Vector<>();
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        mTextView = (TextView) findViewById(R.id.textView_explanation);
        informationButton = (Button) findViewById(R.id.more_info);
        uploadButton = (Button) findViewById(R.id.upload);
        downloadButton = (Button) findViewById(R.id.download);

        upLoadServerUri = serverURL + "upload.php";
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (mNfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }

        if (!mNfcAdapter.isEnabled()) {
            mTextView.setText(R.string.disabled);
        }

        if (mNfcAdapter.isEnabled()) {

            mTextView.setText(R.string.enabled);
        }

        boolean hasPermission = (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE);
        }
        checkWifiOnAndConnected();

}
    private boolean checkWifiOnAndConnected() {
        WifiManager wifiMgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        if (wifiMgr.isWifiEnabled()) { // Wi-Fi adapter is ON
            WiFiStateTextView.setText(R.string.WiFiEnabled);
            WifiInfo wifiInfo = wifiMgr.getConnectionInfo();

            if( wifiInfo.getNetworkId() == -1 ){
                WiFiStateTextView.setText(R.string.WiFiEnabledDisconnected);
                return false; // Not connected to an access point
            }
            String ssid = "You are connected to: " + wifiInfo.getSSID();
            WiFiStateTextView.setText(ssid);
            return true; // Connected to an access point
        }
        else {
            WiFiStateTextView.setText(R.string.WiFiDisabled);
            return false; // Wi-Fi adapter is OFF
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_WRITE_STORAGE: {
                if((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {

                } else {
                    Toast.makeText(this, "The app was not allowed to write to your storage. Hence, it cannot function properly. Please consider granting it this permission", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    public void isFilePresent() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"test.xlsx");
        if ( file.exists()) {
            Toast.makeText(MainActivity.this, "File exists, deleting file...", LENGTH_SHORT).show();
            file.delete();
        }
        else {
            Toast.makeText(MainActivity.this, "File doesn't exist, downloaded file will be saved", LENGTH_SHORT).show();
        }
    }

    public void downloadClicked(View view) {
        isFilePresent();
        mgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        String file_url = serverURL + "files/test.xlsx";
        Uri uri=Uri.parse(file_url);
        Toast.makeText(MainActivity.this, "Downloading.", LENGTH_SHORT).show();
        mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle("PrinLab")
                .setDescription("Excel-file")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "test.xlsx"));

    }

    public void uploadClicked(View view) {

        //dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
        new Thread(new Runnable() {
            public void run() {
                uploadFile();
            }
        }).start();
    }


    public void uploadFile() {
        HttpURLConnection conn;
        DataOutputStream dos;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;

        int maxBufferSize = 1024 * 1024;

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),"test.xlsx");
        try {
            // open a URL connection to the Servlet
            FileInputStream fileInputStream = new FileInputStream(file);
            URL url = new URL(upLoadServerUri);
            // Open a HTTP  connection to  the URL
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true); // Allow Inputs
            conn.setDoOutput(true); // Allow Outputs
            conn.setUseCaches(false); // Don't use a Cached Copy
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("ENCTYPE", "multipart/form-data");
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
            conn.setRequestProperty("fileToUpload", file.getName());
            dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"fileToUpload\";filename=\""
                            + file.getName() + "\"" + lineEnd);
            dos.writeBytes(lineEnd);
            // create a buffer of  maximum size
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            // read file and write it into form...
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
            // send multipart form data necesssary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);
            // Responses from the server (code and message)
            serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();
            Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);
            if(serverResponseCode == 200){
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(MainActivity.this, "File Upload Complete.",
                                LENGTH_SHORT).show();
                    }
                });
            }
            //close the streams //
            fileInputStream.close();
            dos.flush();
            dos.close();
        } catch (MalformedURLException ex) {
            //dialog.dismiss();
            ex.printStackTrace();
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "MalformedURLException",
                            LENGTH_SHORT).show();
                }
            });
            Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
        } catch (Exception e) {
            //dialog.dismiss();
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Got Exception : see logcat ",
                            LENGTH_SHORT).show();
                }
            });
        }
        //dialog.dismiss();
        //return serverResponseCode;
    }
    @Override
    protected void onResume() {
        super.onResume();

        checkWifiOnAndConnected();

        setupForegroundDispatch(this, mNfcAdapter);
    }
    @Override
    protected void onPause() {
        stopForegroundDispatch(this, mNfcAdapter);
        checkWifiOnAndConnected();

        super.onPause();
    }
    @Override
    protected void onNewIntent(Intent intent) {


        checkWifiOnAndConnected();

        handleIntent(intent);
    }
    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);
            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();
            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }
    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    private void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};
        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }
    /**
     * @param activity The corresponding {@link Activity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }


    public void infoClicked(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(infoLink));
        startActivity(browserIntent);
    }

    public void openManual(View view) {
    }


    private class NdefReaderTask extends AsyncTask<Tag, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Tag... params) {
            boolean isOk = false;
            results.clear();
            Tag tag = params[0];
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return false;
            }
            NdefMessage ndefMessage = ndef.getCachedNdefMessage();
            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT  )) {
                    try {
                        results.add(readText(ndefRecord));
                        isOk = true;
                    }
                    catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }
            return isOk;
        }
        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */
            byte[] payload = record.getPayload();
            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
            // Get the Language Code
            int languageCodeLength = payload[0] & 51;
            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"
            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                //mTextView.setText("Read content: " + result);
                //splitString = result.split("\\s+");

                infoLink = link + results.elementAt(1);
              

                Log.i("NFC", "sql haku");
                new SigningActivity().execute(results.elementAt(0));


                informationButton.setEnabled(true);



                uploadButton.setEnabled(true);
                downloadButton.setEnabled(true);
            }
        }
    }
    private class SigningActivity extends AsyncTask<String, Void, Void> {


        protected void onPreExecute() {

        }

        @Override
        protected Void doInBackground(String... arg0) {

            try {
                String sqlLink = serverURL + "sqlandroidup.php";
                String data = URLEncoder.encode("id", "UTF-8") + "=" + URLEncoder.encode(arg0[0], "UTF-8");

                HttpHandler sh = new HttpHandler();

                String response = sh.makeServiceCall(sqlLink, data);
                JSONObject jObj = new JSONObject(response);
                boolean error = jObj.getBoolean("error");

                // Check for error node in json
                if (!error) {

                    //JSONObject device = jObj.getJSONObject("device");
                    //String id = jObj.getString("id");
                    //String name = device.getString("name");
                    //String url = device.getString("url");
                    //infoLink = link + url;
                } else {
                    // Error in login. Get the error message
                    String errorMsg = jObj.getString("error_msg");
                    Toast.makeText(getApplicationContext(),
                            errorMsg, Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                // JSON error
                e.printStackTrace();
                Toast.makeText(getApplicationContext(), "Json error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Exception: " + e.getMessage(), Toast.LENGTH_LONG).show();

            }
            return null;
        }
    }
}

