package app.com.pedometerandusagelist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import app.com.pedometerandusagelist.R;
import app.com.pedometerandusagelist.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        binding.pedometer.setOnClickListener(view -> startActivity(new Intent(MainActivity.this, PedometerActivity.class)));
    }
}