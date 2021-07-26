package app.com.pedometerandusagelist.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import java.text.MessageFormat;

import app.com.pedometerandusagelist.R;
import app.com.pedometerandusagelist.databinding.ActivityPedometerBinding;
import app.com.pedometerandusagelist.util.PedometerService;
import app.com.pedometerandusagelist.util.StepCallback;

public class PedometerActivity extends AppCompatActivity {
    ActivityPedometerBinding binding;
    boolean isRunning = false;
    Intent serviceIntent;
    private PedometerService pedometerService;
    public static final String TAG = "PedometerService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_pedometer);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_pedometer);

        binding.backBtn.setOnClickListener(view -> onBackPressed());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // 활동 퍼미션 체크
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_DENIED) {
                // No: 퍼미션 체크 화면 이동
                Intent intent = new Intent(this, AuthorityActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } else {
                // Yes: 서비스 관련 작업 수행
                startService();
            }
        } else {
            startService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isServiceRunningCheck() && serviceIntent != null) {
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isServiceRunningCheck()) {
            // 액티비티를 벗어났을 땐 서비스와 액티비티의 바인드를 끊어줌: 백그라운드에서 UI를 건들지 않게 하기 위함
            unbindService(serviceConnection);
        }
    }

    public void startService() {
        serviceIntent = new Intent(PedometerActivity.this, PedometerService.class);
        if (!isServiceRunningCheck()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                startService(serviceIntent);
            } else {
                startForegroundService(serviceIntent);
            }
            bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        int num = PedometerService.mSteps;
        binding.pedometerSteps.setText(MessageFormat.format("{0} / 5,000", num));
        binding.pedometerProgress.setProgress(num);
    }

    // 서비스 내부로 Set 되어 스텝카운트의 변화와 Unbind의 결과를 전달하는 콜백 객체의 구현체
    private StepCallback stepCallback = new StepCallback() {
        @Override
        public void onStepCallback(int step) {
            binding.pedometerSteps.setText(MessageFormat.format("{0} / 5,000", step));
            binding.pedometerProgress.setProgress(step);
        }

        @Override
        public void onUnbindService() {
            isRunning = false;
            Log.d(TAG, "해제됨");
        }
    };

    // 서비스 바인드를 담당하는 객체의 구현체
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PedometerService.PedometerBinder mb = (PedometerService.PedometerBinder) iBinder;
            pedometerService = mb.getService();
            pedometerService.setCallback(stepCallback);
            isRunning = true;
            Log.d(TAG, "serviceConnection 연결됨");
        }

        // 요거는 사실상 서비스가 킬되거나 아예 죽임 당했을 때만 호출된다고 보면 됨
        // stopService 또는 unBindService 때 호출되지 않음.
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isRunning = false;
            Log.d(TAG, "serviceConnection 해제됨");
        }
    };

    public boolean isServiceRunningCheck() {
        ActivityManager manager = (ActivityManager) this.getSystemService(Activity.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("app.com.pedometerandusagelist.util.PedometerService".equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}