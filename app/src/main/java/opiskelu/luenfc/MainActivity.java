package opiskelu.luenfc;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity{
    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";

    private NfcAdapter mNfcAdapter;
    private TextView mTextView;

    private Button button1, button2, uploadButton, downloadButton;
    // näkyykö
    private String[] splitString;
    final String link = "www.oamk.fi/hankkeet/prinlab/equipment/index.php?page=";
    int serverResponseCode = 0;
    ProgressDialog dialog = null;
    DownloadManager mgr;
    String upLoadServerUri = null;
    File file;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = (TextView) findViewById(R.id.textView_explanation);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        button1 = (Button) findViewById(R.id.button1);
        button2 = (Button) findViewById(R.id.button2);
        uploadButton = (Button) findViewById(R.id.upload);
        downloadButton = (Button) findViewById(R.id.download);

        upLoadServerUri = "http://192.168.137.1/uploadToServer.php";

        if (mNfcAdapter == null) {
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!mNfcAdapter.isEnabled()) {
            mTextView.setText(R.string.disabled);
        } else {
            mTextView.setText(R.string.enabled);
        }




    }

    public void downloadClicked(View view) {

        mgr = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        String file_url = "http://192.168.137.1/exceltest.xlsx";

        Uri uri=Uri.parse(file_url);
        Toast.makeText(MainActivity.this, "Downloading.",
                Toast.LENGTH_SHORT).show();
        mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                .setTitle(" Database File")
                .setDescription("Please wait....test file downloading."));

    }

    public void uploadClicked(View view) {

        dialog = ProgressDialog.show(MainActivity.this, "", "Uploading file...", true);
        new Thread(new Runnable() {
            public void run() {

                uploadFile();

            }
        }).start();
    }

    public void uploadFile() {
        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;

        file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),"test.xlsx");

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
            conn.setRequestProperty("uploaded_file", file.getName());

            dos = new DataOutputStream(conn.getOutputStream());

            dos.writeBytes(twoHyphens + boundary + lineEnd);
            dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
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

            Log.i("uploadFile", "HTTP Response is : "
                    + serverResponseMessage + ": " + serverResponseCode);

            if(serverResponseCode == 200){

                runOnUiThread(new Runnable() {
                    public void run() {

                        String msg = "File Upload Completed.\n\n See uploaded file here : \n\n"
                                +" http://www.androidexample.com/media/uploads/"
                                +file.getName();

                        mTextView.setText(msg);
                        Toast.makeText(MainActivity.this, "File Upload Complete.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }

            //close the streams //
            fileInputStream.close();
            dos.flush();
            dos.close();

        } catch (MalformedURLException ex) {

            dialog.dismiss();
            ex.printStackTrace();

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "MalformedURLException",
                            Toast.LENGTH_SHORT).show();
                }
            });

            Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
        } catch (Exception e) {

            dialog.dismiss();
            e.printStackTrace();

            runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(MainActivity.this, "Got Exception : see logcat ",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
        dialog.dismiss();
        //return serverResponseCode;

    }

    @Override
    protected void onResume() {
        super.onResume();

        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown. 
         */
        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter);

        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
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
    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */

    public void nappi_1_painettu(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(splitString[0]));
        startActivity(browserIntent);
    }

    public void nappi_2_painettu(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(splitString[1]));
        startActivity(browserIntent);
    }

    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
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
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                //mTextView.setText("Read content: " + result);
                splitString = result.split("\\s+");
                button1.setVisibility(View.VISIBLE);
                button2.setVisibility(View.VISIBLE);
                uploadButton.setVisibility(View.VISIBLE);
                downloadButton.setVisibility(View.VISIBLE);
            }
        }
    }
}

