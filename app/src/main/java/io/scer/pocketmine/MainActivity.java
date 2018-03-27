package io.scer.pocketmine;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@SuppressLint("StaticFieldLeak")
@SuppressWarnings("ResultOfMethodCallIgnored")
public class MainActivity extends AppCompatActivity implements Handler.Callback, View.OnClickListener {

    public static MainActivity instance;
    public static Intent service;
    public static Button start;
    public static Button stop;
    public static int fontSize = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;
        ServerUtils.setContext(getApplicationContext());

        String arch = System.getProperty("os.arch");
        if (!arch.contains("aarch64") && !arch.contains("8")) {
            new AlertDialog.Builder(this).setMessage(getString(R.string.error_32bit)).setCancelable(false).setNegativeButton(getString(R.string.exit), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            }).create().show();
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            init();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET}, 1);
        }

        try {
            String[] files = {"php", "killall"};
            for (String path : files) {
                if (!new File(ServerUtils.getAppDirectory() + "/" + path).exists()) {
                    File file = copyAsset(path);
                    file.setExecutable(true, true);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        start = findViewById(R.id.start);
        stop = findViewById(R.id.stop);

        boolean isStarted = ServerUtils.process != null;
        stop.setEnabled(isStarted);
        start.setEnabled(!isStarted);

        start.setOnClickListener(this);
        stop.setOnClickListener(this);
    }

    private void init() {
        new File(ServerUtils.getDataDirectory(), "tmp").mkdirs();
        File ini = ServerUtils.getSettingsFile();
        if (!ini.exists()) {
            try {
                ini.createNewFile();
                FileOutputStream stream = new FileOutputStream(ini);
                stream.write(("date.timezone=UTC\nshort_open_tag=0\nasp_tags=0\nphar.readonly=0\nphar.require_hash=1\nigbinary.compact_strings=0\nzend.assertions=-1\nerror_reporting=-1\ndisplay_errors=1\ndisplay_startup_errors=1\n").getBytes("UTF8"));
                stream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!ServerUtils.isInstalled()) {
            downloadPM();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    init();
                } else {
                    Toast.makeText(this, "To make the application work correctly, you need grant access to the memory of your device!", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "SameParameterValue", "TrustAllX509TrustManager", "BadHostnameVerifier"})
    public void downloadFile(final String url, final File file) {
        if (file.exists()) file.delete();
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage(getString(R.string.downloading).replace("%name%", file.getName()));
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgressNumberFormat(null);
        dialog.show();
        new Thread(new Runnable() {
            public void run() {
                OutputStream output;
                InputStream input;
                try {
                    if (file.exists()) file.delete();
                    final SSLContext ssl = SSLContext.getInstance("SSL");
                    ssl.init(null, new TrustManager[]{
                            new X509TrustManager() {
                                @Override
                                public void checkClientTrusted(X509Certificate[] p1, String p2) {
                                }

                                @Override
                                public void checkServerTrusted(X509Certificate[] p1, String p2) {
                                }

                                @Override
                                public X509Certificate[] getAcceptedIssuers() {
                                    return null;
                                }
                            }
                    }, new java.security.SecureRandom());
                    HttpsURLConnection.setDefaultSSLSocketFactory(ssl.getSocketFactory());
                    HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    });
                    URL req = new URL(url);
                    URLConnection connection = req.openConnection();
                    connection.connect();
                    input = new BufferedInputStream(connection.getInputStream());
                    output = new FileOutputStream(file);
                    int count;
                    long read = 0;
                    final long max = connection.getContentLength();
                    runOnUiThread(new Runnable() {
                        public void run() {
                            dialog.setMax((int) max / 1024);
                        }
                    });
                    byte[] buffer = new byte[4096];
                    while ((count = input.read(buffer)) >= 0) {
                        output.write(buffer, 0, count);
                        read += count;
                        final int temp = (int) (read / 1000);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                dialog.setProgress(temp);
                            }
                        });
                    }
                    output.close();
                    input.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    }
                });
            }
        }).start();
    }

    @Override
    public boolean handleMessage(Message message) {
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.start:
                start.setEnabled(false);
                stop.setEnabled(true);
                startService(service = new Intent(this, ServerService.class));
                break;
            case R.id.stop:
                start.setEnabled(true);
                stop.setEnabled(false);
                ServerUtils.sendCommand("stop");
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.console:
                startActivity(new Intent(this, ConsoleActivity.class));
                break;
            case R.id.download:
                downloadPM();
                break;
            case R.id.kill:
                ServerUtils.kill();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private static void copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        int cou;
        byte[] buffer = new byte[8192];
        while ((cou = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, cou);
        }
        inputStream.close();
        outputStream.close();
    }

    @SuppressWarnings("SameParameterValue")
    private File copyAsset(String path) throws IOException {
        File file = new File(ServerUtils.getAppDirectory() + "/" + path);
        copyStream(MainActivity.instance.getApplicationContext().getAssets().open(path), new FileOutputStream(file));
        return file;
    }

    private void downloadPM() {
        downloadFile("https://jenkins.pmmp.io/job/PocketMine-MP/lastSuccessfulBuild/artifact/PocketMine-MP.phar", ServerUtils.getPhar());
    }

    public static void stopNotifyService() {
        instance.runOnUiThread(new Runnable() {
            public void run() {
                instance.stopService(service);
                start.setEnabled(true);
                stop.setEnabled(false);
            }
        });
    }
}