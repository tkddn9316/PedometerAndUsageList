package app.com.pedometerandusagelist.util;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import app.com.pedometerandusagelist.R;
import app.com.pedometerandusagelist.activity.MainActivity;

public class PedometerService extends Service implements SensorEventListener, AlarmInterface {
    AlarmManager alarmManager;
    // 현재 걸음 수
    public static int mSteps = 0;
    // 이전에 저장된 걸음 수
    int previousStep = 0;
    private IBinder myBinder = new PedometerBinder();
    Intent intent;
    NotificationCompat.Builder builder;
    NotificationManager manager;
    PendingIntent pendingIntent;
    Sensor mCounterSensor;
    SensorManager mSensorManager;
    private StepCallback callback;
    public static final String TAG = "PedometerService123";
    String title, text;

    // LocalBinder 클래스 생성
    public class PedometerBinder extends Binder {
        public PedometerService getService() {
            return PedometerService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

//    @Override
//    public boolean onUnbind(Intent intent) {
//        unRegisterManager();
//        if (callback != null) {
//            callback.onUnbindService();
//        }
//        return super.onUnbind(intent);
//    }

    public void setCallback(StepCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        initStepSensor();
        setAlarmManager();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 노티바 띄우기
        setForegroundNotification();

        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            // TYPE_STEP_COUNTER: 핸드폰이 켜진순간 부터 현재까지 누적된 카운트(디바이스 껐다가 다시 키면 수치가 0으로 초기화)
            if (previousStep < 1) {
                // initial value
                previousStep = (int) sensorEvent.values[0];
            }
            // 현재 걸음 수를 구하려면 이전의 값을 변수에 저장해 두었다가 현재 걸음 수를 구할때 그 변수를 빼주어야 한다.
            mSteps = (int) sensorEvent.values[0] - previousStep;
            if (builder != null) {
                builder.setContentText("테스트 중: " + mSteps  + "회 걸었다!");
                manager.notify(1000, builder.build());
            }
            if (callback != null) {
                callback.onStepCallback(mSteps);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
// digh
    }

    // 센서 초기화
    private void initStepSensor() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mCounterSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        // 리스너 시작
        if (mCounterSensor == null) {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.toast_sensor_not_found), Toast.LENGTH_SHORT).show();
        } else {
            mSensorManager.registerListener(this, mCounterSensor, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }
    // 알람 설정
    public void setAlarmManager() {
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Log.d(TAG, "초기화 ㅇㅋ");
        }
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        intent = new Intent(getApplicationContext(), AlarmReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        // 1일 후로 설정
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        Date date = calendar.getTime();
        SimpleDateFormat format2 = new SimpleDateFormat( "yyyy년 MM월dd일 HH시mm분ss초");
        Log.d(TAG, format2.format(date));

        // 다음날 0시에 맞추기 위해 24시간을 뜻하는 상수인 AlarmManager.INTERVAL_DAY를 더해줌.
        AlarmReceiver receiver = new AlarmReceiver();
        receiver.setListener(PedometerService.this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_DATE_CHANGED);
        registerReceiver(receiver, intentFilter);
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
    }

    @Override
    public void receiveAlarm() {
        mSteps = 0;
        previousStep = 0;
        if (builder != null) {
            builder.setContentText(getResources().getString(R.string.notification_default));
            manager.notify(1000, builder.build());
        }
        setAlarmManager();
        Log.d(TAG, "알람 완료, 다음 알람 세팅");
    }

    // 노티바 설정
    public void setForegroundNotification() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            // 알림 채널을 만든 후에는 알림 동작을 변경할 수 없습니다. 이 시점에서는 사용자가 모든 동작을 제어합니다.
            // 그러나 채널의 이름과 설명은 변경할 수 있습니다.
            NotificationChannel channel = new NotificationChannel("service", "Service", NotificationManager.IMPORTANCE_MIN);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            channel.setShowBadge(false);
//            RemoteViews customLayout = new RemoteViews(getPackageName(), R.layout.notification_expanded);

            manager.createNotificationChannel(channel);
            title = getResources().getString(R.string.app_name);
            text = getResources().getString(R.string.notification_default);
            builder = new NotificationCompat.Builder(this, channel.getId())
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(text)
//                    .setContent(customLayout)
                    .setChannelId("service")
                    .setShowWhen(false)
                    .setContentIntent(pendingIntent);
        } else {
            title = getResources().getString(R.string.app_name);
            text = getResources().getString(R.string.notification_default);
            builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setPriority(-2)
                    .setShowWhen(false)
                    .setContentIntent(pendingIntent);
        }
        startForeground(1000, builder.build());
    }

    // 콜백 해제
    public void unRegisterManager() {
        try {
            mSensorManager.unregisterListener(this);
            mSteps = 0;
            previousStep = 0;

            if (alarmManager != null) {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(),
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unRegisterManager();
        stopForeground(true);
    }
}
