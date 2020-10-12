package com.alecodelab.alexchuchev;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.DocumentsContract;
import android.util.JsonReader;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.Certificate;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

import static android.content.Context.MODE_PRIVATE;
import static java.nio.charset.StandardCharsets.UTF_8;

public class MainActivity extends AppCompatActivity {
    Button buttonGet;
    Button buttonAdd;
    LinearLayout mainLayout;
    ProgressBar progressBar;
    SharedPreferences sharedPreferences;
    static String session;
    MainHandler mainHandler;
    NewData newData;
    private int get_session = 1;
    private int get_entries = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        buttonGet = findViewById(R.id.btnGet);
        buttonAdd = findViewById(R.id.btnAdd);
        mainLayout = findViewById(R.id.mainLayout);
        progressBar = findViewById(R.id.progress_bar);

        sharedPreferences = getSharedPreferences("userSession", MODE_PRIVATE);
        mainHandler = new MainHandler(MainActivity.this, mainLayout, progressBar, sharedPreferences);
        newData = new NewData(sharedPreferences, mainHandler);

        // если есть сохранённая сессия, то читаем
        if (sharedPreferences.contains("session")) {
            session = sharedPreferences.getString("session", "");

        } else {
            // получаем сессию и записываем её
            Toast.makeText(MainActivity.this, "Получение новой сессии", Toast.LENGTH_SHORT).show();
            // запрос на получение сессии
            String request = "------123456789\n" + "Content-Disposition: form-data; name=\"a\"\n\n" +
                    "new_session" + "\n" +
                    "------123456789--";
            // отправка запроса
            newData.getNewData(request, get_session);
        }


        // кнопка вывода записей
        buttonGet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressBar.setVisibility(View.VISIBLE);

                // создаем запрос на получение записей
                String request = "------123456789\n" + "Content-Disposition: form-data; name=\"a\"\n\n" +
                        "get_entries" + "\n" +
                        "------123456789\n" +
                        "Content-Disposition: form-data; name=\"session\"\n\n" +
                        session + "\n" +
                        "------123456789--";
                // отправка запроса
                newData.getNewData(request, get_entries);


            }
        });

        // кнопка перехода в активити добавления записи
        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, EntryActivity.class);
                startActivity(intent);

            }
        });


    }


}

class MainHandler extends Handler {
    private Context context;
    private LinearLayout linearLayout;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private int get_entries = 2;

    MainHandler(Context context, LinearLayout linearLayout, ProgressBar progressBar, SharedPreferences sharedPreferences) {
        this.context = context;
        this.linearLayout = linearLayout;
        this.progressBar = progressBar;
        this.sharedPreferences = sharedPreferences;
    }


    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what) {
            case 0:
                // при отсутствии соединения с сервером
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage("Проверьте наличие интернета и нажмите Обновить");
                builder.setPositiveButton("Обновить", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String request = "------123456789\n" + "Content-Disposition: form-data; name=\"a\"\n\n" +
                                "get_entries" + "\n" +
                                "------123456789\n" +
                                "Content-Disposition: form-data; name=\"session\"\n\n" +
                                MainActivity.session + "\n" +
                                "------123456789--";
                        NewData newData = new NewData(sharedPreferences, MainHandler.this);
                        newData.getNewData(request, get_entries);
                    }
                });

                builder.show();


                break;

            case 2:
                String response = msg.getData().getString("response");
                linearLayout.removeAllViews();
                // вывод полученных записей на экран
                parsePrintEntries(response);
                break;


            case 3:
                // вывод ошибки сервера
                String error = msg.getData().getString("responseCode");
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                break;

        }


    }

    private void parsePrintEntries(String response) {
// распарсивание полученных данных и вывод на экран

        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("data");
            JSONArray jsonArray2 = jsonArray.getJSONArray(0);

            for (int i = 0; i < jsonArray2.length(); i++) {
                String info = jsonArray2.getString(i);
                JSONObject jsonObject2 = new JSONObject(info);

                String entry = jsonObject2.getString("body");

                if (entry.length() > 200) {
                   entry = entry.substring(0, 200);
               }

                String da = jsonObject2.getString("da");
                String dm = jsonObject2.getString("dm");

                // перевод даты
                long daMilliseconds = Long.parseLong(da) * 1000;
                long dmMilliseconds = Long.parseLong(dm) * 1000;
                String daDate = DateFormat.getDateTimeInstance().format(daMilliseconds);
                String dmDate = DateFormat.getDateTimeInstance().format(dmMilliseconds);

                // создание и настройка лэйаутов
                LinearLayout entryLayout = new LinearLayout(context);
                entryLayout.setOrientation(LinearLayout.VERTICAL);
                entryLayout.setBackgroundResource(R.drawable.entryshape);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.setMargins(10, 5, 10, 5);
                entryLayout.setLayoutParams(layoutParams);
                // лэйаут даты создания
                LinearLayout llDa = new LinearLayout(context);
                llDa.setOrientation(LinearLayout.HORIZONTAL);
                TextView textViewDa = new TextView(context);
                TextView textViewDaTarget = new TextView(context);
                textViewDa.setGravity(Gravity.CENTER);
                textViewDa.setPadding(10, 0, 0, 0);
                textViewDa.setText("Дата создания записи :    ");
                textViewDa.setTextSize(12);
                textViewDaTarget.setTextSize(12);
                textViewDaTarget.setGravity(Gravity.CENTER);
                textViewDaTarget.setText(daDate);
                llDa.addView(textViewDa);
                llDa.addView(textViewDaTarget);

                // если даты модификации и создания не совпадают, то выводим дату модификации
                if (!daDate.equals(dmDate)) {
                    // лэйаут даты модификации
                    LinearLayout llDm = new LinearLayout(context);
                    llDm.setOrientation(LinearLayout.HORIZONTAL);
                    TextView textViewDm = new TextView(context);
                    TextView textViewDmTarget = new TextView(context);
                    textViewDm.setTextSize(12);
                    textViewDmTarget.setTextSize(12);
                    textViewDm.setText("Дата модификации записи :    ");
                    textViewDmTarget.setText(dmDate);
                    llDm.addView(textViewDm);
                    llDm.addView(textViewDmTarget);
                    entryLayout.addView(llDm);
                }

                // textView записи
                TextView textViewEntry = new TextView(context);
                textViewEntry.setPadding(10, 0, 10, 0);
                textViewEntry.setTextSize(17);
                textViewEntry.setText(entry);

                // добавление лэйаутов в главный лэйаут
                entryLayout.addView(llDa);
                entryLayout.addView(textViewEntry);
                progressBar.setVisibility(View.GONE);
                linearLayout.addView(entryLayout);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


}


class NewData {
    private Map<String, String> map;
    private SharedPreferences sharedPreferences;
    private MainHandler handler;
    private int get_session = 1;
    private int get_entries = 2;

    NewData(SharedPreferences sharedPreferences, MainHandler handler) {
        this.sharedPreferences = sharedPreferences;
        this.handler = handler;
    }


    void getNewData(final String request, final int action) {


        map = new HashMap<>();
        map.put("headerToken1", "token");
        map.put("headerToken2", "1Ek0Byt-Ee-EJCSxay");
        map.put("headerEntries1", "Content-Type");
        map.put("headerEntries2", "multipart/form-data; boundary=----123456789");
        map.put("getSession", "new_session");
        map.put("getEntries", "get_entries");

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // соединение и получение данных с сервера

                HttpsURLConnection urlConnection = null;
                try {
                    URL url = new URL("https://bnet.i-partner.ru/testAPI/");
                    urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setDoInput(true);
                    urlConnection.setDoOutput(false);
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty(map.get("headerToken1"), map.get("headerToken2"));
                    urlConnection.setRequestProperty(map.get("headerEntries1"), map.get("headerEntries2"));
                    urlConnection.connect();
                    OutputStream outputStream = new BufferedOutputStream(urlConnection.getOutputStream());
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                    writer.write(request);
                    writer.flush();
                    writer.close();
                    outputStream.close();

                    // проверка кода ответа сервера
                    if (HttpsURLConnection.HTTP_OK == urlConnection.getResponseCode()) {
                        // читаем ответ  сервера
                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(urlConnection.getInputStream()));
                        String responseSERVER = in.readLine();

                        if (responseSERVER.length() > 0) {
                            // получаем сессию и записываем её
                            if (action == get_session) {

                                JSONObject jsonObject = new JSONObject(responseSERVER);
                                JSONObject data = jsonObject.getJSONObject("data");
                                String newSession = data.getString("session");
                                MainActivity.session = newSession;

                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("session", newSession);
                                editor.apply();
                            }

                            if (action == get_entries) {
                                // получаем записи и отправляем в handler
                                Message message = new Message();
                                Bundle bundle = new Bundle();
                                bundle.putString("response", responseSERVER);
                                message.setData(bundle);
                                message.what = action;
                                handler.sendMessage(message);
                            }
                        }
                    } else {
                        // если ошибка сервера
                        Message message = new Message();
                        Bundle bundle = new Bundle();
                        bundle.putString("responseCode", "Ошибка сервера " + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage());
                        message.setData(bundle);
                        message.what = 3;
                        handler.sendMessage(message);

                    }
                } catch (Exception e) {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    handler.sendEmptyMessage(0);
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}



