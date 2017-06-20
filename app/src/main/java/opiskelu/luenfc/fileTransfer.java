package opiskelu.luenfc;

import android.app.Activity;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
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

    private void isFilePresent(final Activity activity, final String filename) {

        // Tarkistaa löytyykö tiettyä tiedostoa muistista, tiedosto poistetaan jos sellainen löytyy ja jos ei niin uusi ladataan tilalle

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),filename);
        if ( file.exists()) {
            Toast.makeText(activity, "File exists, deleting file...", LENGTH_SHORT).show();
            if(!file.delete()) {
                Log.e("FILE", "Couldn't delete file");
            }
        }
        else {
            Toast.makeText(activity, "File doesn't exist, downloaded file will be saved", LENGTH_SHORT).show();
        }
    }
    void downloadFile(final Activity activity, final String filename) {
        final DownloadManager mgr = (DownloadManager) activity.getSystemService(DOWNLOAD_SERVICE);
        final String file_url = serverURL + "files/" + filename;
        Uri uri = Uri.parse(file_url);
        Log.e("DOWNLOAD", file_url);

        isFilePresent(activity, filename);

        final ProgressDialog progress = ProgressDialog.show(activity, activity.getString( R.string.dl_show_local_progress_title ), activity.getString( R.string.dl_show_local_progress_content ), true );



        BroadcastReceiver onComplete = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ( !progress.isShowing() ) {
                    return;
                }
                context.unregisterReceiver( this );

                progress.dismiss();
                long downloadId = intent.getLongExtra( DownloadManager.EXTRA_DOWNLOAD_ID, -1 );
                Cursor c;
                c = mgr.query( new DownloadManager.Query().setFilterById( downloadId ) );

                if ( c.moveToFirst() ) {
                    int status = c.getInt( c.getColumnIndex( DownloadManager.COLUMN_STATUS ) );
                    if ( status == DownloadManager.STATUS_SUCCESSFUL ) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        String type = filename.substring(filename.lastIndexOf(".") + 1);
                        String local_url = Environment.getExternalStorageDirectory() + "/" + Environment.DIRECTORY_DOWNLOADS  + "/" + filename;
                        Log.i("DOWNLOAD", local_url);
                        if(type.equals("xlsx")) {
                            i.setDataAndType(Uri.fromFile( new File(local_url)), "application/vnd.ms-excel");
                        } else if(type.equals("pdf")) {
                            i.setDataAndType(Uri.fromFile( new File(local_url)), "application/pdf");
                            i.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                        }
                        Intent i2 = Intent.createChooser(i, "Open file");
                        try {
                            context.startActivity(i2);
                        }
                        catch (ActivityNotFoundException e) {

                            Toast.makeText(context, "Install a PDF reader to open the PDF", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                c.close();
            }
        };
        activity.registerReceiver( onComplete, new IntentFilter( DownloadManager.ACTION_DOWNLOAD_COMPLETE ) );
        Toast.makeText(activity, "Downloading.", LENGTH_SHORT).show();
        mgr.enqueue(new DownloadManager.Request(uri).setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE).setAllowedOverRoaming(false).setTitle("PrinLab")
                .setDescription("Excel-file")
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename));
    }

    void uploadFile(final Activity activity, String filename) {
        String upLoadServerUri = serverURL + "upload.php";
        HttpURLConnection conn;
        DataOutputStream dos;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;

        int maxBufferSize = 1024 * 1024;

        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),filename);
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
