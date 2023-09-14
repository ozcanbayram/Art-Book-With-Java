package com.example.javamaps.view;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.example.javamaps.R;
import com.example.javamaps.model.Place;
import com.example.javamaps.roomdb.PlaceDao;
import com.example.javamaps.roomdb.PlaceDatabase;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.javamaps.databinding.ActivityMapsBinding;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener { //GoogleMap.OnMapLongClickListener iplement ederek haritaya uzun tıklanması durumunda bir işlemyapabiliriz.

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    ActivityResultLauncher<String> permissionLauncher; //(izin başlatıcısı)
    LocationManager locationManager; //Konum yöneticisi
    LocationListener locationListener; //Konum dinleyicisi
    SharedPreferences sharedPreferences;
    Boolean info;
    PlaceDatabase db;
    PlaceDao placeDao;
    Double selectedLatitude;
    Double selectedLongitude;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    Place selectedPlace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        registerLauncher(); //

        sharedPreferences = this.getSharedPreferences("package com.example.javamaps",MODE_PRIVATE);
        info = false;


        db = Room.databaseBuilder(getApplicationContext(), PlaceDatabase.class,"Places")
                //  .allowMainThreadQueries()  --> bu tavsiye edilmez ama kayıt işlemini gerçekleştirir. Fakat ön planda dataRoom işlemi yapmak tavsiye edilmez.
                .build();


        placeDao = db.placeDao();

        selectedLatitude=0.0;
        selectedLongitude=0.0;

        binding.saveButton.setEnabled(false);


    }

    @Override
    public void onMapReady(GoogleMap googleMap)  {
        mMap = googleMap;

        mMap.setOnMapLongClickListener(this); //Haritaya uzun tıklanınca listener vermemiz lazım listener de arayüzüe verildiği için this verebiliriz.

        Intent intent = getIntent();
        String intentInfo = intent.getStringExtra("info");

        if (intentInfo.equals("new")){

            binding.saveButton.setVisibility(View.VISIBLE);
            binding.deleteButton.setVisibility(View.GONE);


            // Telefonun konumunu anlayabilmek için bulabilmek için:
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {                                          //Konum dinleyicisi
                @Override
                public void onLocationChanged(@NonNull Location location) {                      // Konum değişince çalışacak metot


                    info = sharedPreferences.getBoolean("info",false); //uygulama açılınca ilk kullanıcının yerini kaydetmek için

                    if (!info){ //info == false demek
                        LatLng userLocation = new LatLng(location.getLatitude(),location.getLongitude()); //Kullanıcının yeni konumunn enlem ve boylamını alarak userLocation değişkeninin içine koyduk. ev aşağıda bunu kullanarak kameranın bu konuma  hareket etmesini sağlayacağız:
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation,15));
                        sharedPreferences.edit().putBoolean("info",true).apply();
                        //Bir defa çalıştıktan sorna true değerini alacak ve sonraki çağrılmalarda çalışmasına gerek kalmayacak.
                    }


                }
                //eğer çalışmayan bir sürüm olursa hata alırsak boş bir şekilde onStatusChanged metotdunu çağırabiliriz.
            };


            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                //Request Permission
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_FINE_LOCATION)){
                    Snackbar.make(binding.getRoot(),"Permission needed for maps", Snackbar.LENGTH_INDEFINITE).setAction("Give Permission", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //request permission
                            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                        }
                    }).show();
                } else {
                    //request permission
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
                }
            }else {
                // Aşağıdaki kod satırında Konum değişikliklerini isteyebiliriz. Son güncellemeleri alabiliriz ama izin gereklidir.
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener); //Bu kodlar izin istenmeden çalışmaz. (0,0 normalde çok fazla enerji tüketir çünkü 0 saniye ve 0 yer değişikliğinde konumu güncelleme komutudur.)

                //Son bilinen konumu alma: LocationManeger'in içieriisnde zaten var.
                //Şimdi kullanıcının son konumunu alalım:
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); //Kullanıcının Son GPS'ini al ve lastLocation değişkenine eşitle.
                if(lastLocation!=null){ //Eğer son konum boş değilse: kullanıcının konumunu tekarar LatLng ile alalım ve bir değişkene atayalım:
                    LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,18)); //Kullanıcının konumuna kamerayı yöneltip 15 zoom yapmak için.
                    //Yokarıdaki son konumu alma kod bloğunu kopyalayıp, aşayıdaki ilk izinin alındığı yere de yapıştıralım.
                }

                mMap.setMyLocationEnabled(true); // mavi imleç ile konumumuzu gösterir. Kullanıcı konumunu görebilir.
            }

        }else{

            mMap.clear();

            selectedPlace = (Place) intent.getSerializableExtra("place");

            LatLng latLng = new LatLng(selectedPlace.latitude,selectedPlace.longitude);

            mMap.addMarker(new MarkerOptions().position(latLng).title(selectedPlace.name));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,15));
            binding.placeNameText.setText(selectedPlace.name);
            binding.saveButton.setVisibility(View.GONE);
            binding.deleteButton.setVisibility(View.VISIBLE);

        }










        // lat--> latitue  (enlem)
        // lon--> longitue (boylam)

        // Add a marker in Sydney and move the camera
        //LatLng sydney = new LatLng(-34, 151); //konum /başlangıçta gösterilen yerin enlem ve boylamı
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney")); // kırmızı işaretleme çubuğu ve başlığı
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        //Örnek oalrak kendi kodlarımızı yazalım: Açılış konumunu eifell tower olarak belirtelim:

       // LatLng eiffel = new LatLng(48.858595232626534, 2.2947063683711746);
       // mMap.addMarker(new MarkerOptions().position(eiffel).title("Eiffel Tower"));
       // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(eiffel,15));
    }

    public void registerLauncher(){
        permissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    //Permission granted (izin verildi)
                    //(locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener);) bu kod hata veriyor olabilir.Bunu garanti hale getirmek için if içieisinde bir kontrol daha yapılır.
                    if(ContextCompat.checkSelfPermission(MapsActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){ //izin verildiyse : konumu aşağıdaki satırda iste.
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,locationListener); //Bu kodlar izin istenmeden çalışmaz. (0,0 normalde çok fazla enerji tüketir çünkü 0 saniye ve 0 yer değişikliğinde konumu güncelleme komutudur.)

                        //Son bilinen konumu alma: LocationManeger'in içieriisnde zaten var.
                        //Şimdi kullanıcının son konumunu alalım:
                        Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER); //Kullanıcının Son GPS'ini al ve lastLocation değişkenine eşitle.
                        if(lastLocation!=null){ //Eğer son konum boş değilse: kullanıcının konumunu tekarar LatLng ile alalım ve bir değişkene atayalım:
                        LatLng lastUserLocation = new LatLng(lastLocation.getLatitude(),lastLocation.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastUserLocation,15)); //Kullanıcının konumuna kamerayı yöneltip 15 zoom yapmak için.
                    }
                }else {//Perrmission Denied (İzin reddedildi)
                    Toast.makeText(MapsActivity.this, "İzin lazım", Toast.LENGTH_SHORT).show();
                }
            }
        };
    });

  }

    @Override
    public void onMapLongClick(@NonNull LatLng latLng) { //GoogleMap.OnMapLongClickListener iplement edildikten sonra alt+enter ile eklenmesi gereken metot.
        mMap.clear(); //her tıklamadan önce bir öncekini temizler

        mMap.addMarker(new MarkerOptions().position(latLng)); // Uzun tıklanan yere marker işaretçisini ekler

        selectedLatitude = latLng.latitude;
        selectedLongitude = latLng.longitude;

        binding.saveButton.setEnabled(true); //kullanıcı yer seçene kadar
    }


    public void save(View view){
        Place place = new Place(binding.placeNameText.getText().toString(),selectedLatitude,selectedLongitude);

        // rxJava ile:
        // threading --> Main (UI), Default (CPU Intensive), IO (network, database)
        // placeDao.insert(place).subscribeOn(Schedulers.io()).subscribe();
        // disposable

        compositeDisposable.add(placeDao.insert(place)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MapsActivity.this::handlerResponse)
        );
    }

    private void handlerResponse(){
        Intent intent = new Intent(MapsActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }


    public void delete(View view){

        if(selectedPlace!=null){
            compositeDisposable.add(placeDao.delete(selectedPlace)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(MapsActivity.this::handlerResponse)
            );
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}