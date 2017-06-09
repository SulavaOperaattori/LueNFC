package opiskelu.luenfc;

import android.app.Activity;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static android.content.Context.DOWNLOAD_SERVICE;
import static android.widget.Toast.LENGTH_SHORT;


class fileTransfer  {

    private final String serverURL = "http://193.167.148.46/";

    void downloadFile(final Activity activity) {
        DownloadManager mgr = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
        String file_url = serverURL + "files/test.xlsx";
        Uri uri = Uri.parse(file_url);
        Toast.makeText(activity, "Downloading.", LENGTH_SHORT).show();
        mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE).setAllowedOverRoaming(false).setTitle("PrinLab")
                .setDescription("Excel-file")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "test.xlsx"));
    }

    void uploadFile(final Activity activity) {
        String upLoadServerUri = serverURL + "upload.php";
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
            dos.writeBytes("Content-Disposition: form-data; name=\"fileToUpload\";filename=\"" + file.getName() + "\"" + lineEnd);
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

            // send multipart form data necessary after file data...
            dos.writeBytes(lineEnd);
            dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

            // Responses from the server (code and message)
            int serverResponseCode = conn.getResponseCode();
            String serverResponseMessage = conn.getResponseMessage();
            Log.i("uploadFile", "HTTP Response is : " + serverResponseMessage + ": " + serverResponseCode);
            if(serverResponseCode == 200){
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(activity, "File Upload Complete.", LENGTH_SHORT).show();
                    }
                });
            }

            //close the streams //
            fileInputStream.close();
            dos.flush();
            dos.close();
        }
        catch (MalformedURLException ex) {
            //dialog.dismiss();
            ex.printStackTrace();
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "MalformedURLException", LENGTH_SHORT).show();
                }
            });
            Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
        }
        catch (Exception e) {
            //dialog.dismiss();
            e.printStackTrace();
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    Toast.makeText(activity, "Got Exception : see logcat ", LENGTH_SHORT).show();
                }
            });
        }
        //dialog.dismiss();
        //return serverResponseCode;
    }
}
