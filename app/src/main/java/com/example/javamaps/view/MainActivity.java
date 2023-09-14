package com.example.javamaps.view;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.javamaps.PlaceAdapter;
import com.example.javamaps.R;
import com.example.javamaps.databinding.ActivityMainBinding;
import com.example.javamaps.model.Place;
import com.example.javamaps.roomdb.PlaceDao;
import com.example.javamaps.roomdb.PlaceDatabase;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    PlaceDatabase db;
    PlaceDao placeDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // setContentView(R.layout.activity_main);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        db = Room.databaseBuilder(getApplicationContext(), PlaceDatabase.class,"Places").build();
        placeDao = db.placeDao();

        compositeDisposable.add(placeDao.getAll()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(MainActivity.this::handlerResponse)
        );
    }


    private void handlerResponse(List<Place> placeList){
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PlaceAdapter  placeAdapter= new PlaceAdapter(placeList);
        binding.recyclerView.setAdapter(placeAdapter);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) { //menüyü ekleme metodu
        MenuInflater menuInflater = getMenuInflater(); //Menu bağlandı inflater kodunu layout ile aktiviteyi birbirine bağlayınca kullanırız.
        menuInflater.inflate(R.menu.travel_menu,menu); //menu yeri belirtildi ve bağlandı

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) { // menüden bir şey seçilirse ne olacak.
        if(item.getItemId() == R.id.add_place){ //add_place' ye mi tıklandı kontrolü.
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);

            intent.putExtra("info","new");

            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        compositeDisposable.clear();
    }
}