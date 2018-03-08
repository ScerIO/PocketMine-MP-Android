package com.frago9876543210.pocketmine_mp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.*;
import java.nio.charset.Charset;

@SuppressWarnings("WeakerAccess")
public class ServerUtils {
    @SuppressLint("StaticFieldLeak")
    public static Context context;
    private static OutputStream stdin;
    private static InputStream stdout;
    static Process process;

    public static void setContext(Context context) {
        ServerUtils.context = context;
    }

    public static File getDataDirectory() {
        return new File(Environment.getExternalStorageDirectory().getPath() + "/PocketMine-MP");
    }

    public static File getPhar() {
        return new File(getDataDirectory(), "PocketMine-MP.phar");
    }

    public static boolean isInstalled() {
        return getPhar().exists();
    }

    public static File getAppDirectory() {
        return new File(context.getApplicationInfo().dataDir);
    }

    public static File getPhp() {
        return new File(getAppDirectory(), "php");
    }

    public static File getKiller() {
        return new File(getAppDirectory(), "killall");
    }

    public static File getSettingsFile() {
        return new File(getDataDirectory(), "php.ini");
    }

    public static void run() {
        if (!isInstalled()) {
            Toast.makeText(context, "Could not find file PocketMine-MP.phar!", Toast.LENGTH_LONG).show();
        }
        ProcessBuilder builder = new ProcessBuilder(getPhp().toString(), "-c", getSettingsFile().toString(), getPhar().toString(), "--no-wizard");
        builder.redirectErrorStream(true);
        builder.directory(getDataDirectory());
        builder.environment().put("TMPDIR", getDataDirectory() + "/tmp");
        try {
            process = builder.start();
            stdout = process.getInputStream();
            stdin = process.getOutputStream();
            new Thread() {
                public void run() {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(stdout, Charset.forName("UTF-8")));
                    while (isRunning()) {
                        try {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                ConsoleActivity.log(line + "\n");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    MainActivity.stopNotifyService();
                }
            }.start();
        } catch (Exception e) {
            e.printStackTrace();
            MainActivity.stopNotifyService();
            kill();
        }
    }

    public static boolean isRunning() {
        try {
            process.exitValue();
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    public static void sendCommand(String command) {
        try {
            if (!isRunning()) return;
            ConsoleActivity.log("> " + command + "\n");
            stdin.write((command + "\n").getBytes());
            stdin.flush();
        } catch (Exception ignored) {
        }
    }

    public static void kill() {
        execCommand(getKiller() + " php");
    }

    public static void execCommand(String command) {
        try {
            Runtime.getRuntime().exec(command);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}