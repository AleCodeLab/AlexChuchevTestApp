package com.alecodelab.alexchuchev;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;

public class EntryActivity extends AppCompatActivity {
    Button btnSave;
    Button btnCancel;
    EditText editText;
    NewHandler newHandler;
    String headerEntries1 = "Content-Type";
    String headerEntries2 = "multipart/form-data; boundary=----123456789";
    String token = "1Ek0Byt-Ee-EJCSxay";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entry);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        editText = findViewById(R.id.editText);

        newHandler = new NewHandler(EntryActivity.this, editText);

        // кнопка Сохранить запись
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = editText.getText().toString();
                if (message.length() > 0) {
                    // отправка записи
                    String request = "------123456789\n" +
                            "Content-Disposition: form-data; name=\"a\"\n\n" +
                            "add_entry\n" +
                            "------123456789\n" +
                            "Content-Disposition: form-data; name=\"session\"\n\n" +
                            MainActivity.session + "\n" +
                            "------123456789\n" +
                            "Content-Disposition: form-data; name=\"body\"\n\n" +
                            message + "\n" +
                            "------123456789--";
                    sendData(headerEntries1, headerEntries2, request);
                } else
                    // если текст не введен
                    Toast.makeText(EntryActivity.this, "Не введен текст", Toast.LENGTH_SHORT).show();
            }
        });


        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    void sendData(final String header1, final String header2, final String req) {
        // соединение с сервером и отправка записи
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                HttpsURLConnection urlConnection = null;
                try {
                    URL url = new URL("https://bnet.i-partner.ru/testAPI/");
                    urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(false);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("token", token);
                    urlConnection.setRequestProperty(header1, header2);
                    urlConnection.connect();
                    OutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream());
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                    writer.write(req);
                    writer.flush();
                    writer.close();
                    outputStream.close();
                    if (HttpsURLConnection.HTTP_OK == urlConnection.getResponseCode()) {

                        BufferedReader inputStream = new BufferedReader(
                                new InputStreamReader(urlConnection.getInputStream()));
                        String response = inputStream.readLine();
                        if (response != null) {
                            JSONObject jsonObject = new JSONObject(response);
                            String status = jsonObject.getString("status");
                            if (status.equals("1")) {
                                newHandler.sendEmptyMessage(1);
                            }
                        }
                    } else {
                        String error = "Ошибка сервера :" + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage();
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("error", error);
                        message.setData(bundle);
                        message.what = 2;
                        newHandler.sendMessage(message);
                    }


                } catch (Exception e) {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    newHandler.sendEmptyMessage(0);
                }
            }
        });
        thread.start();
    }


}

class NewHandler extends Handler {
    private Context context;
    private EditText editText;

    NewHandler(Context context, EditText editText) {
        this.context = context;
        this.editText = editText;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {

        switch (msg.what) {

            case 0:
                // если нет соединения с сервером
                Toast.makeText(context, "Запись не сохранена. Проверьте наличие интернета", Toast.LENGTH_SHORT).show();
                break;

            case 1:
                // если запись доставлена не сервер
                Toast.makeText(context, "Запись сохранена", Toast.LENGTH_SHORT).show();
                editText.getText().clear();
                break;

            case 2:
                // если ошибка сервера
                String error = msg.getData().getString("error");
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
