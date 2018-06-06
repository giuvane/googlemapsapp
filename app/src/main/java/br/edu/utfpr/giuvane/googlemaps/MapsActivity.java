package br.edu.utfpr.giuvane.googlemaps;

import android.graphics.Color;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Placemark;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapClickListener,
        OnMapLongClickListener {

    // Atributos referentes ao mapa e localização
    private GoogleMap mMap;
    private GoogleApiClient googleApiClient;
    private LatLng latLng;
    private Location localizacaoAtual;
    private LocationRequest mLocationRequest;
    private UiSettings uiSettings;
    private List<LatLng> pontosLinha;
    private List<LatLng> pontosPoligono;
    private Marker ponto;
    private PolylineOptions linhaOptions;
    private Polyline linha;
    private PolygonOptions poligonoOptions;
    private Polygon poligono;
    private double areaTotal = 0;

    // Componentes adicionados no Activity
    private Button btAdicionar;
    private SupportMapFragment mapFragment;
    private RadioGroup radioGroup;
    private RadioButton radioPonto;
    private RadioButton radioLinha;
    private RadioButton radioPoligono;
    private EditText etLatitude;
    private EditText etLongitude;
    private EditText etDescricao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        btAdicionar = (Button) findViewById(R.id.button);
        btAdicionar.setOnClickListener(btAdicionarListener);

        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        radioPonto = (RadioButton) findViewById(R.id.rbPonto);
        radioLinha = (RadioButton) findViewById(R.id.rbLinha);
        radioPoligono = (RadioButton) findViewById(R.id.rbPoligono);

        etLatitude = (EditText) findViewById(R.id.etLatitude);
        etLongitude = (EditText) findViewById(R.id.etLongitude);
        etDescricao = (EditText) findViewById(R.id.etDescricao);

        pontosLinha = new ArrayList<LatLng>();
        pontosPoligono = new ArrayList<LatLng>();

        createLocationRequest();

        // Configurar mapa
        setUpMapIfNeeded();
        //mMap = (GoogleMap) mapFragment.getMap();

        // Configurar localização
        setUpLocationClientIfNeeded();

        GerarKml();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mapa, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.gerarKml)
        {
            Toast.makeText(MapsActivity.this, "Gerar KML", Toast.LENGTH_SHORT).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setUpMapIfNeeded() {
        // Verifica se o mapa necessita ser instanciado
        if (mMap == null) {
            // Obtem o mapa do SupportMapFragment.
            mMap = mapFragment.getMap();

            // Habilita localização do GPS e métodos para controle de cliques no mapa
            //mMap.setMyLocationEnabled(true); // LOCALIZAÇÃO FUNCIONA APENAS NO DISPOSITIVO COM GPS
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            mMap.setOnMapClickListener(this);
            mMap.setOnMapLongClickListener(this);


            // Habilita funções no mapa
            uiSettings = mMap.getUiSettings();
            uiSettings.setZoomControlsEnabled(true);
            uiSettings.setCompassEnabled(true);
            uiSettings.setRotateGesturesEnabled(true);
            //map.getUiSettings().setIndoorLevelPickerEnabled(true);

            //mColorBar.setOnSeekBarChangeListener(this);
            //mAlphaBar.setOnSeekBarChangeListener(this);
            //mWidthBar.setOnSeekBarChangeListener(this);
            // Check if we were successful in obtaining the map.

        }
    }

    private void setUpLocationClientIfNeeded() {
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    protected void createLocationRequest() {
        /* CONFIGURAÇÃO DE LOCALIZAÇÃO FUNCIONA APENAS NO DISPOSITIVO COM GPS
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(16);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        */
    }

    protected void startLocationUpdates() {
        /*
        // CONFIGURAÇÃO FUNCIONA APENAS NO DISPOSTIVO COM GPS
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient, mLocationRequest, this);
        */
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng utfpr = new LatLng(-24.846434, -54.343223);
        mMap.addMarker(new MarkerOptions().position(utfpr).title("UTFPR-SH"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(utfpr));
    }

    @Override
    public void onLocationChanged(Location location) {
        //localizacaoAtual = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        localizacaoAtual = location;

    }


    @Override
    public void onConnected(Bundle bundle) {
        startLocationUpdates();

        try {
            latLng = new LatLng(LocationServices.FusedLocationApi.getLastLocation(googleApiClient).getLatitude(), LocationServices.FusedLocationApi.getLastLocation(googleApiClient).getLongitude());

            // Move a câmera para Framework System com zoom.
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 20));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(17), 2000, null);
        } catch (Exception ex){
            // CONFIGURAÇÃO FUNCIONA APENAS NO DISPOSITIVO
            //Toast.makeText(this, "O GPS está desconectado", Toast.LENGTH_LONG).show();
            //Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            //startActivityForResult(intent, 1);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onMapClick(LatLng latLng) {
        //Toast.makeText(this, "Lat: " + latLng.latitude + " Long: " + latLng.longitude, Toast.LENGTH_SHORT).show();

        etLatitude.setText(String.valueOf(latLng.latitude));
        etLongitude.setText(String.valueOf(latLng.longitude));
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        // Define o tipo de dado geográfico a ser coletado
        if (radioPonto.isChecked())
        {
            adicionarPonto(latLng);


        }
        else if (radioLinha.isChecked())
        {
            adicionarLinha(latLng);


        }
        else
        {
            adicionarPoligono(latLng);
        }
    }

    // Adicionar Latitude e Longitude informada
    private View.OnClickListener btAdicionarListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
        // Define o tipo de dado geográfico a ser coletado
        if (radioPonto.isChecked())
        {
            // Atualiza Ponto
            latLng = new LatLng(Double.valueOf(etLatitude.getText().toString()), Double.valueOf(etLongitude.getText().toString()));
            adicionarPonto(latLng);

            //ponto = mMap.addMarker(new MarkerOptions().position(latLng).title(etDescricao.getText().toString()));
        }
        else if (radioLinha.isChecked())
        {
            // Adiciona um ponto a linha
            latLng = new LatLng(Double.valueOf(etLatitude.getText().toString()), Double.valueOf(etLongitude.getText().toString()));

            adicionarLinha(latLng);

            /*
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(etDescricao.getText().toString()));

            // Controla linhas adicionadas ao mapa
            linhaOptions = new PolylineOptions();
            pontosLinha.add(latLng = new LatLng(Double.valueOf(etLatitude.getText().toString()), Double.valueOf(etLongitude.getText().toString())));

            linhaOptions.addAll(pontosLinha).geodesic(true);

            // Controla a cor do layer conforme SeekBar
            int color = Color.HSVToColor(
                    255, new float[] {0, 0, 0});
            linha = mMap.addPolyline(linhaOptions
                    .color(color)
                    .width(5));
                    */
        }
        else
        {
            // Adiciona marcador no último ponto tocado no mapa
            latLng = new LatLng(Double.valueOf(etLatitude.getText().toString()), Double.valueOf(etLongitude.getText().toString()));

            adicionarPoligono(latLng);

            /*
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(etDescricao.getText().toString()));

            // Controla polígonos adicionados ao mapa
            poligonoOptions = new PolygonOptions();
            pontosPoligono.add(latLng = new LatLng(Double.valueOf(etLatitude.getText().toString()), Double.valueOf(etLongitude.getText().toString())));

            poligonoOptions.addAll(pontosPoligono).geodesic(true);

            // Controla a cor do layer conforme SeekBar
            int fillColor = Color.HSVToColor(
                    255, new float[] {0, 0, 0});
            poligono = mMap.addPolygon(poligonoOptions
                    .strokeWidth(5)
                    .strokeColor(Color.BLACK)
                    .fillColor(fillColor));
                    */
        }

        }

    };

    private void adicionarPonto(LatLng latLng)
    {
        // Atualiza Ponto
        //this.latLng = latLng;
        ponto = mMap.addMarker(new MarkerOptions().position(latLng).title(etDescricao.getText().toString()));
    }

    public void adicionarLinha(LatLng latLng)
    {
        mMap.clear();

        // Adiciona um ponto a linha
        //this.latLng = new LatLng(latLng.latitude, latLng.longitude);
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(etDescricao.getText().toString()));

        // Controla linhas adicionadas ao mapa
        linhaOptions = new PolylineOptions();
        pontosLinha.add(latLng);

        linhaOptions.addAll(pontosLinha).geodesic(true);

        // Controla a cor do layer conforme SeekBar
        int color = Color.HSVToColor(
                255, new float[] {0, 0, 0});
        linha = mMap.addPolyline(linhaOptions
                .color(color)
                .width(5));
    }

    public void adicionarPoligono(LatLng latLng)
    {
        // Limpa o mapa
        mMap.clear();

        // Adiciona marcador no último ponto tocado no mapa
        //this.latLng = new LatLng(latLng.latitude, latLng.longitude);
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title(etDescricao.getText().toString()));

        // Controla polígonos adicionados ao mapa
        poligonoOptions = new PolygonOptions();
        pontosPoligono.add(latLng);

        poligonoOptions.addAll(pontosPoligono).geodesic(true);

        // Controla a cor do layer conforme SeekBar
        int fillColor = Color.HSVToColor(
                255, new float[] {0, 0, 0});
        poligono = mMap.addPolygon(poligonoOptions
                .strokeWidth(5)
                .strokeColor(Color.BLACK)
                .fillColor(fillColor));
    }

    private void GerarKml()
    {
        /*
        Kml kml = KmlFactory.createKml();

        //Document document = new Document();

        Placemark placemark = KmlFactory.createPlacemark();
        placemark.setName("Teste");
        placemark.setDescription("Teste");
        */
    }
}