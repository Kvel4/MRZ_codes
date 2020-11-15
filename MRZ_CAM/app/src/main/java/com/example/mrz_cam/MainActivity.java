package com.example.mrz_cam;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.NameList;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        setResponce("Вас приветствует приложение MRZ_SCAN. Сканирование следует выполнять горизонтально, в случае неудачи рекомендуется повторить сканирование");
        checkFirstStart();

    }

    public void OnClick(View view) {
        dispatchTakePictureIntent();
    }

    private void checkFirstStart() {

        SharedPreferences sp = getSharedPreferences("hasVisited",
                Context.MODE_PRIVATE);
        // проверяем, первый ли раз открывается программа (Если вход первый то вернет false)
        boolean hasVisited = sp.getBoolean("hasVisited", false);

        if (!hasVisited) {
            // Сработает если Вход первый
            saveTextBuffer("");

            //Ставим метку что вход уже был
            SharedPreferences.Editor e = sp.edit();
            e.putBoolean("hasVisited", true);
            e.apply(); //После этого hasVisited будет уже true и будет означать, что вход уже был

            //Ниже запускаем активность которая нужна при первом входе

        } else {
            String current = GetText();
            EditText ed = findViewById(R.id.editText);
            ed.setText(current);
            //Сработает если вход в приложение уже был
            //Ниже запускаем активность которая нужна при последующих входах
        }
    }

    private final static String FILE_NAME = "content.txt";

    public void saveTextBuffer(String obmen) {
        FileOutputStream fos = null;
        try {

            fos = openFileOutput(FILE_NAME, MODE_PRIVATE);
            fos.write(obmen.getBytes());
        } catch (IOException ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public String GetText() {

        FileInputStream fin = null;
        try {
            fin = openFileInput(FILE_NAME);
            byte[] bytes = new byte[fin.available()];
            fin.read(bytes);
            String text = new String(bytes);
            return text;
        } catch (IOException ex) {

            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
            String err = "error";
            return err;
        } finally {

            try {
                if (fin != null)
                    fin.close();
            } catch (IOException ex) {

                Toast.makeText(this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                String err = "error";
                return err;

            }
        }
    }


    String URL = "http://800940a6f913.ngrok.io";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    /*
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (ActivityNotFoundException e) {
            // display error state to the user
        }
    }
    */

    String getCurrentHttps() {
        EditText ed = findViewById(R.id.editText);
        saveTextBuffer(ed.getText().toString());
        return ed.getText().toString();
    }


    String currentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    static final int REQUEST_TAKE_PHOTO = 1;

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            /*
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ImageView img = findViewById(R.id.img);
            img.setImageBitmap(imageBitmap);
            //imageView.setImageBitmap(imageBitmap);
            request(imageBitmap);
            */
            galleryAddPic();
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(currentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        try {

            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), contentUri);
            Bitmap bitmap2 = Bitmap.createScaledBitmap(bitmap, 960, 720, true);
            request(bitmap2);


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getByteArrayfromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, bos);
        return bos.toByteArray();
    }

    void setResponce(String responceString) {
        TextView text = findViewById(R.id.testtext);
        //text.setText(responceString);

        EditText editText = findViewById(R.id.editText);
        //editText.setVisibility(View.GONE);

        Button bt = findViewById(R.id.button);
        //editText.setVisibility(View.GONE);

        List<String> li = new ArrayList<>();
        ListView lv = findViewById(R.id.listview);
        li.add(responceString);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, li) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textView = (TextView) view.findViewById(android.R.id.text1);

                /*YOUR CHOICE OF COLOR*/
                textView.setTextColor(Color.BLACK);
                textView.setTextSize(20);

                return view;
            }
        };
        lv.setAdapter(adapter);
    }

    void setMas(List<String> st) {
        ListView lv = findViewById(R.id.listview);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_list_item_1, st) {

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                TextView textView = (TextView) view.findViewById(android.R.id.text1);

                /*YOUR CHOICE OF COLOR*/
                textView.setTextColor(Color.BLACK);
                textView.setTextSize(20);

                return view;
            }
        };
        lv.setAdapter(adapter);
    }

    void request(Bitmap imageBitmap) {
        try {

            byte[] curImage = getByteArrayfromBitmap(imageBitmap);
            String inputLine = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                inputLine = Base64.getEncoder().encodeToString(curImage);
            }
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            URL = "http://" + getCurrentHttps() + ".ngrok.io";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("image", inputLine);
            //jsonBody.put("Author", "BNK");
            final String requestBody = jsonBody.toString();

            JsonObjectRequest myRequest = new JsonObjectRequest(Request.Method.POST,
                    URL, null,
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            /*
                            Toast toast = Toast.makeText(getApplicationContext(),"YESYES", Toast.LENGTH_SHORT);
                            toast.show();
                            JSONObject jsonObject = response;
                            int here = jsonObject.length();
                            String cock = "";
                            cock+=here;
                            setResponce(cock);
                            //Toast toast = Toast.makeText(getApplicationContext(),cock, Toast.LENGTH_SHORT);
                            //toast.show();
                            */
                            //setResponce(response.toString());
                            //JSONArray r = response.getJSONArray();


                            List<String> li = new ArrayList<>();
                            for (int i = 0; i < response.names().length(); i++) {
                                String cur = "";
                                try {
                                    cur += response.names().getString(i);
                                    //cur+=" ";
                                    String db = "";
                                    db += response.getString(cur);
                                    cur += ": ";
                                    cur += db;
                                    li.add(cur);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                            }
                            setMas(li);
                        }


                    }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {

                    Log.e("VOLLEY", error.toString());
                    int a = error.networkResponse.statusCode;
                    if (a == 418) {
                        String clear = "Приложение не обнаружило MRZ, рекомендуем повторить сканирование горизонтально";
                        setResponce(clear);
                    }
                    if (a != 200 && a != 418) {
                        String clear = "Сервер не отвечает или введен невалидный ключ";
                        setResponce(clear);
                    }

                }

            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }

            };

            myRequest.setRetryPolicy(new DefaultRetryPolicy(
                    20000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(myRequest);
            /*
            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        int here = jsonObject.length();
                        String cock = "";
                        cock+=here;
                        setResponce(cock);
                        Toast toast = Toast.makeText(getApplicationContext(),cock, Toast.LENGTH_SHORT);
                        toast.show();



                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Log.i("VOLLEY", response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    /*
                    Log.e("VOLLEY", error.toString());
                    int a = error.networkResponse.statusCode;
                    if(a!=200) {
                        String clear = "";
                        clear += a;
                        //setResponce(clear);
                    }

                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }


                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                        // setResponce(responseString);
                        // can get more details such as response.headers
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };*/


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


}