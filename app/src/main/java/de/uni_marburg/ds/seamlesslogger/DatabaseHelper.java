package de.uni_marburg.ds.seamlesslogger;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.orm.SchemaGenerator;
import com.orm.SugarContext;
import com.orm.SugarDb;
import com.orm.SugarRecord;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

class DatabaseHelper extends AsyncTask<Void, Integer, Boolean>{
    private WeakReference<MainActivity> mainActivityReference;
    private ProgressDialog dialog;
    private ProgressDialog retryDialog;
    private String dbPath;

    DatabaseHelper(MainActivity mainActivity) {
        this.mainActivityReference = new WeakReference<>(mainActivity);
        initializeDatabase();
    }

    private DatabaseHelper(DatabaseHelper that) {
        this.mainActivityReference = that.mainActivityReference;
        this.dbPath = that.dbPath;
    }

    @Override
    protected void onPreExecute() {
        MainActivity mainActivity = mainActivityReference.get();

        dialog = new ProgressDialog(mainActivity);
        dialog.setMessage("...");
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.show();

        retryDialog = new ProgressDialog(mainActivity);
        retryDialog.setMessage("The upload failed. (Is the network still available?)");
        retryDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Delete Database", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetDatabase();
                unlockWriting();
                retryDialog.dismiss();
            }
        });
        retryDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                retryDialog.dismiss();

                new DatabaseHelper(DatabaseHelper.this).execute();
            }
        });
    }

    @Override
    protected Boolean doInBackground(Void[] params) {
        publishProgress(-2);

        MainActivity mainActivity = mainActivityReference.get();
        if (mainActivity == null ) {
            return false;
        }

        String deviceId = mainActivity.deviceId;
        String experimentTimestamp = mainActivity.experimentTimestamp;

        try {
            while (mainActivity.loggingServiceIsRunning())
                Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        publishProgress(-1);
        lockWriting();

        publishProgress(0);

        DataOutputStream outputStream = null;
        FileInputStream fileInputStream = null;

        boolean returnCode = false;

        try {
            HttpURLConnection connection;

            int chunkSize, remainingSize, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1024;

            File uploadFile = new File(dbPath);

            if (uploadFile.isFile()) {

                String uploadServerBaseURL = "http://ds.mathematik.uni-marburg.de/seamless-upload/upload/";
                String mkcolURL = deviceId + "/";
                String fileURL = experimentTimestamp + ".db";
                String uploadURL = uploadServerBaseURL + mkcolURL + fileURL;

                // open a URL connection to the Servlet
                fileInputStream = new FileInputStream(uploadFile);
                BufferedInputStream streamFileBufferedInputStream = new BufferedInputStream(fileInputStream);

                int totalSize = streamFileBufferedInputStream.available();

                URL url = new URL(uploadURL);

                // Open a HTTP connection to the URL
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setFixedLengthStreamingMode(totalSize);
                connection.setRequestMethod("PUT");

                outputStream = new DataOutputStream(connection.getOutputStream());

                // create a buffer of maximum size
                remainingSize = streamFileBufferedInputStream.available();

                bufferSize = Math.min(remainingSize, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                chunkSize = streamFileBufferedInputStream.read(buffer, 0, bufferSize);

                int dataSent = totalSize - remainingSize;
                float progress;
                while (chunkSize > 0) {
                    progress = (float) dataSent / (float) totalSize * 100;
                    publishProgress(Math.round(progress));

                    outputStream.write(buffer, 0, bufferSize);
                    remainingSize = streamFileBufferedInputStream.available();
                    bufferSize = Math.min(remainingSize, maxBufferSize);
                    chunkSize = streamFileBufferedInputStream.read(buffer, 0, bufferSize);

                    dataSent = totalSize - remainingSize;
                }

                outputStream.flush();

                // Responses from the server (code and message)
                int serverResponseCode = connection.getResponseCode();

                if (serverResponseCode == 201) {
                    returnCode = true;
                }
            }

        } catch (MalformedURLException e) {
            Log.e("SEAMLESS", "The URL is malformed.");
            e.printStackTrace();
            publishProgress(-3);
        } catch (FileNotFoundException e) {
            Log.e("SEAMLESS", "File not found.");
            e.printStackTrace();
            publishProgress(-3);
        } catch (IOException e) {
            Log.e("SEAMLESS", "An IOException occurred.");
            e.printStackTrace();
            publishProgress(-3);
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }

                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                Log.e("SEAMLESS", "An IOException occurred at cleanup.");
                e.printStackTrace();
            }
        }

        return returnCode;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        if (progress[0] == -3){
            dialog.dismiss();
            retryDialog.show();
        } else if (progress[0] == -2){
            dialog.setMessage("Waiting for background service to finish...");
        } else if (progress[0] == -1) {
            dialog.setMessage("Dumping data to local database...");
        } else if (progress[0] == 0) {
            dialog.setMessage("Uploading local database to server...");
            dialog.setIndeterminate(false);
            dialog.setProgress(progress[0]);
        } else {
            dialog.setProgress(progress[0]);
        }
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            dialog.dismiss();
            resetDatabase();
            unlockWriting();

            MainActivity mainActivity = mainActivityReference.get();
            if (mainActivity != null ) {
                Toast.makeText(mainActivity, "Upload finished successfully!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Database generation / SugarORM related
    void initializeDatabase() {
        MainActivity mainActivity = mainActivityReference.get();
        if (mainActivity != null ) {

            SugarContext.init(mainActivity.getApplicationContext());
            SchemaGenerator schemaGenerator = new SchemaGenerator(mainActivity);
            SQLiteDatabase database = new SugarDb(mainActivity).getDB();
            schemaGenerator.createDatabase(database);

            dbPath = database.getPath();
        }
    }

    void resetDatabase() {
        if (dbPath != null) {
            File dbFile = new File(dbPath);
            dbFile.delete();
        }

        initializeDatabase();
    }

    // Methods for async dump of Data elements
    static private final int EVENT_DUMP_COUNT = 1000;
    static List<SugarRecord> newEventsList = new ArrayList<>(EVENT_DUMP_COUNT);
    private static boolean writeLocked = false;

    static void addRecord(SugarRecord record) {
        if ( (!writeLocked) && (newEventsList.size() >= EVENT_DUMP_COUNT)) {
            dumpRecords();
        }

        newEventsList.add(record);
    }

    static void dumpRecords() {
        final List<SugarRecord> dumpEventsList = newEventsList;
        newEventsList = new ArrayList<>(EVENT_DUMP_COUNT);

        SugarRecord.saveInTx(dumpEventsList);
    }

    static void lockWriting() {
        dumpRecords();
        writeLocked = true;
    }

    static void unlockWriting() {
        writeLocked = false;
        if (newEventsList.size() >= EVENT_DUMP_COUNT) {
            dumpRecords();
        }
    }
}
