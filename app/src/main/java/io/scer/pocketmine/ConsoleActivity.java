package io.scer.pocketmine;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

public class ConsoleActivity extends AppCompatActivity implements Handler.Callback {

    ImageButton send;
    EditText edit_command;
    @SuppressLint("StaticFieldLeak")
    public static ConsoleActivity instance;
    public TextView label_log;
    public ScrollView scrollView;
    public static String currentLog = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_console);

        instance = this;

        send = findViewById(R.id.send);
        edit_command = findViewById(R.id.edit_command);
        label_log = findViewById(R.id.label_log);
        scrollView = findViewById(R.id.scroll);

        label_log.setTextSize(MainActivity.fontSize);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ServerUtils.sendCommand(edit_command.getText().toString());
                edit_command.setText("");
            }
        });

        label_log.setText(currentLog);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.console, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.clear:
                label_log.setText("");
                currentLog = "";
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void log(final String text) {
        currentLog += text;
        if (instance != null) {
            instance.runOnUiThread(new Runnable() {
                public void run() {
                    instance.label_log.append(text);
                    instance.scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    }

    @Override
    public boolean handleMessage(Message message) {
        return false;
    }
}