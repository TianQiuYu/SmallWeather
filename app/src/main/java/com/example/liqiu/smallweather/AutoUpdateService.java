package com.example.liqiu.smallweather;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.example.liqiu.smallweather.gson.Weather;
import com.example.liqiu.smallweather.util.HttpUtil;
import com.example.liqiu.smallweather.util.ParseUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateImage();
        //创建定时任务
        AlarmManager manager= (AlarmManager) getSystemService(ALARM_SERVICE);
        int hource = 8 * 60 * 60 * 1000;
        long time=hource+SystemClock.elapsedRealtime();
        Intent intent1 = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, intent1, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,time,pi);

        return super.onStartCommand(intent, flags, startId);
    }
/*更新每日一图*/
    private void updateImage() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String imageUrl = preferences.getString("bing_ic", null);
        if(imageUrl!=null){
            String address="http://guolin.tech/api/bing_pic";
            HttpUtil.sendOkHttpRequest(address, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String imageURL = response.body().string();
                    if(imageURL!=null){
                        preferences.edit().putString("bing_ic",imageURL).apply();
                    }
                }
            });

        }
    }

    /*更新天气*/
    private void updateWeather() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weather = preferences.getString("weather", null);
        if(weather!=null){
            Weather weather1 = ParseUtil.handleWeatherResponse(weather);
            String weatherId = weather1.basic.weatherId;

            String address="http://guolin.tech/api/weather?cityid="+weatherId+"&key=bc0418b57b2d4918819d3974ac1285d9";
            HttpUtil.sendOkHttpRequest(address, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String weather = response.body().string();
                    Weather weather2 = ParseUtil.handleWeatherResponse(weather);
                    if(weather2!=null&&"ok".equals(weather2.status)){
                        preferences.edit().putString("weather",weather).apply();
                    }
                }
            });
        }

    }

}
