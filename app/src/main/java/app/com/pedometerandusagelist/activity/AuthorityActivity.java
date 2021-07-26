package app.com.pedometerandusagelist.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import app.com.pedometerandusagelist.R;
import app.com.pedometerandusagelist.databinding.ActivityAuthorityBinding;

public class AuthorityActivity extends AppCompatActivity {
    ActivityAuthorityBinding binding;
    private long backKeyPressedTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_authority);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_authority);

        binding.buttonRecognition.setOnClickListener(view -> {
            if (ContextCompat.checkSelfPermission(AuthorityActivity.this,
                    Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestPermissions(new String[]{Manifest.permission.ACTIVITY_RECOGNITION}, 0);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        checkPermission();
    }

    protected void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {   // 마시멜로우 이상일 때
            String[] PERMISSIONS = new String[]{Manifest.permission.ACTIVITY_RECOGNITION};
            boolean ACTIVITY_RECOGNITION = false;
            for (String PERMISSION : PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, PERMISSION) != PackageManager.PERMISSION_GRANTED) {
                    if (PERMISSION.equals(Manifest.permission.ACTIVITY_RECOGNITION)) {
                        ACTIVITY_RECOGNITION = true;
                    }
                }
            }
            if (!ACTIVITY_RECOGNITION) {
                Intent intent = new Intent(this, PedometerActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
            backKeyPressedTime = System.currentTimeMillis();
            Toast.makeText(AuthorityActivity.this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
            finish();
        }
    }
}