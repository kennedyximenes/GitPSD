package com.example.kennedy.expsd;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener {

    private String tipoRede = "";
    private String lat = "";
    private String lon = "";
    private String texto = "";
    private String responseServer = "";
    private String ip = "";

    private TextView msg;
    private TextView tvCoodenadas;
    private EditText etTexto;
    private EditText etIP;

    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private ImageView imageView;
    private Uri file;

    private static final int CAMERA_CAPTURE_IMAGE_REQUEST_CODE = 100;
    public static final int MEDIA_TYPE_IMAGE = 1;
    private Uri fileUri;

    private ProgressBar progressBar;
    private String filePath = null;
    private TextView txtPercentage;
    long totalSize = 0;

    String responseString = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        msg = (TextView)findViewById(R.id.tvMensagem);

        if( wifiOk() || redeOk() ){
            msg.setText("Exercício PSD Digital (" + tipoRede + ")");
        }else{
            msg.setText("Exercício PSD Digital (SEM INTERNET)");
        }

        tvCoodenadas = (TextView)findViewById(R.id.tvCoord);
        etTexto = (EditText)findViewById(R.id.etTextoEnviar);
        etIP = (EditText)findViewById(R.id.etIPServer);
        callConnection();

        imageView = (ImageView) findViewById(R.id.ivFoto);
        txtPercentage = (TextView) findViewById(R.id.txtPercentage);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);


        if (!isDeviceSupportCamera()) {
            Toast.makeText(getApplicationContext(),
                    "O seu dispositivo não possui câmera!",
                    Toast.LENGTH_LONG).show();
        }
    }

    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            return true;
        } else {
            return false;
        }
    }


    public void onClickTirarFoto (View view){

        imageView.setVisibility(View.VISIBLE);
        captureImage();

    }


    public void onClickEnviar(View view){

        if( filePath == null){

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage("Deseja enviar os dados sem foto?");

            alertDialogBuilder.setPositiveButton("Sim", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    fazerEnvio();
                }
            });

            alertDialogBuilder.setNegativeButton("Não",new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                }
            });

            AlertDialog alertDialog = alertDialogBuilder.create();
            alertDialog.show();

        }else{
            fazerEnvio();
        }
    }


    private void fazerEnvio(){

        txtPercentage.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        texto = etTexto.getText().toString();
        ip = etIP.getText().toString().trim();
        AsyncT asyncT = new AsyncT();
        asyncT.execute();

    }


    private void captureImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        fileUri = getOutputMediaFileUri(MEDIA_TYPE_IMAGE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        startActivityForResult(intent, CAMERA_CAPTURE_IMAGE_REQUEST_CODE);
    }

    public Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    private static File getOutputMediaFile(int type) {

        File mediaStorageDir = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                Config.IMAGE_DIRECTORY_NAME);

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.i("ksx", "Falha ao criar "
                        + Config.IMAGE_DIRECTORY_NAME + " directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",
                Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator
                    + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("file_uri", fileUri);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        fileUri = savedInstanceState.getParcelable("file_uri");
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_CAPTURE_IMAGE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 8;
                filePath = fileUri.getPath();
                final Bitmap bitmap = BitmapFactory.decodeFile(filePath, options);
                imageView.setImageBitmap(bitmap);

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(), "Captura de imagem cancelada!", Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(getApplicationContext(),
                        "Falha ao capturar imagem!", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }


    class AsyncT extends AsyncTask<Void, Integer, String> {

        @Override
        protected void onPreExecute() {
            progressBar.setProgress(0);
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(progress[0]);
            txtPercentage.setText(String.valueOf(progress[0]) + "%");
        }


        @Override
        protected String doInBackground(Void... voids) {

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(ip + "/json");

            try {
                JSONObject jsonobj = new JSONObject();

                jsonobj.put("latitude", lat);
                jsonobj.put("longitude", lon);
                jsonobj.put("texto", texto);

                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("req", jsonobj.toString()));

                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse response = httpclient.execute(httppost);
                InputStream inputStream = response.getEntity().getContent();
                InputStreamToStringExample str = new InputStreamToStringExample();
                responseServer = str.getStringFromInputStream(inputStream);

            } catch (Exception e) {
                e.printStackTrace();
            }


            if( filePath != null ){

                try{

                    httppost = new HttpPost(ip + "/foto");
                    AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
                            new AndroidMultiPartEntity.ProgressListener() {

                                @Override
                                public void transferred(long num) {
                                    publishProgress((int) ((num / (float) totalSize) * 100));
                                }
                            });

                    File sourceFile = new File(filePath);
                    entity.addPart("image", new FileBody(sourceFile));
                    totalSize = entity.getContentLength();
                    httppost.setEntity(entity);

                    HttpResponse response = httpclient.execute(httppost);
                    HttpEntity r_entity = response.getEntity();

                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode == 200) {
                        responseString = EntityUtils.toString(r_entity);
                    } else {
                        responseString = "Ocorreu um erro! Http Status: "
                                + statusCode;
                    }

                }catch (Exception e){

                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(String aVoid) {
            super.onPostExecute(aVoid);

            String fromServer = "";

            if( responseServer.length() > 200){
                fromServer =  responseServer.substring(0,200);
            }else{
                fromServer = responseServer;
            }

            AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle("Retorno Server");
            alert.setMessage( "ENVIADO COM SUCESSO: " + fromServer );
            alert.setPositiveButton("Ok", null);
            alert.show();

            txtPercentage.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            imageView.setVisibility(View.GONE);
            filePath = null;
        }
    }


    public static class InputStreamToStringExample {

        public static void main(String[] args) throws IOException {

            InputStream is = new ByteArrayInputStream("mensagem".getBytes());
            String result = getStringFromInputStream(is);
            System.out.println(result);
            System.out.println("Concluído");
        }

        private static String getStringFromInputStream(InputStream is) {

            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();
            String line;

            try {
                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return sb.toString();
        }
    }


    private static File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "ExPSD");

        if (!mediaStorageDir.exists()){
            if (!mediaStorageDir.mkdirs()){
                Log.d("ksx", "Erro ao criar diretorio");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        return new File(mediaStorageDir.getPath() + File.separator +
                "IMG_"+ timeStamp + ".jpg");
    }


    private boolean redeOk(){
        ConnectivityManager check = (ConnectivityManager) MainActivity.this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] info = check.getAllNetworkInfo();

        if(info[0].getState().toString().equals("CONNECTED")){
            tipoRede = "3G/4G";
            return true;
        }else{
            tipoRede = "Sem conexão com internet!";
            return false;
        }
    }


    private boolean wifiOk(){

        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            tipoRede = "WiFi";
            return true;
        }else{
            tipoRede = "Sem conexão com internet!";
            return false;
        }

    }


    @Override
    public void onResume(){
        super.onResume();

        if(mGoogleApiClient != null && mGoogleApiClient.isConnected()){
            startLocationUpdate();
        }
    }


    @Override
    public void onPause(){
        super.onPause();

        if(mGoogleApiClient != null){
            startLocationUpdate();
        }
    }

    private synchronized void callConnection(){

        try{
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addOnConnectionFailedListener(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API)
                    .build();

            mGoogleApiClient.connect();

        }catch (Exception e){
            Log.i("ksx", "ERRO GPS: " + e.toString());
        }

    }


    private void initLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    private void startLocationUpdate(){
        initLocationRequest();
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, MainActivity.this);
    }


    @Override
    public void onLocationChanged(Location location) {

        lat = String.valueOf(location.getLatitude());
        lon = String.valueOf(location.getLongitude());
        tvCoodenadas.setText("Sua localização: Lat " + lat + " Long " + lon);
    }


    @Override
    public void onConnected(Bundle bundle) {

        Location l = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (l != null){
            lat = String.valueOf(l.getLatitude());
            lon = String.valueOf(l.getLongitude());
            tvCoodenadas.setText("Sua localização: Lat " + lat + " Long " + lon);
        }

        startLocationUpdate();
    }


    @Override
    public void onConnectionSuspended(int i) {
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

}
