package com.example.liqiu.smallweather;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.liqiu.smallweather.gson.Forcast;
import com.example.liqiu.smallweather.gson.Weather;
import com.example.liqiu.smallweather.util.HttpUtil;
import com.example.liqiu.smallweather.util.ParseUtil;
import java.io.IOException;
import java.util.List;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private TextView cityName;
    private TextView updataTime;
    private TextView nowTemp;
    private TextView nowWeather;
    private LinearLayout forcaseLayout;
    private TextView aqiText;
    private TextView pm25;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ScrollView weatherLayout;
    private ImageView backgroundImg;
    public SwipeRefreshLayout swipeRefresh;
    private String mWeatherId;
    public DrawerLayout drawerLayout;
    private Button openDrawer;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //将图片和系统状态栏融合在一起
        if(Build.VERSION.SDK_INT>=21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                     | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //找到控件
        weatherLayout = findViewById(R.id.sl_weather_layout);
        backgroundImg = findViewById(R.id.iv_background_img);
        cityName = findViewById(R.id.tv_city_name);
        updataTime = findViewById(R.id.tv_update_time);
        nowTemp = findViewById(R.id.tv_now_temp);
        nowWeather = findViewById(R.id.tv_now_weather_info);
        forcaseLayout = findViewById(R.id.ll_forecast_layout);
        aqiText = findViewById(R.id.tv_aqi_text);
        pm25 = findViewById(R.id.tv_pm25_text);
        comfortText = (TextView) findViewById(R.id.tv_comfort_text);
        carWashText = (TextView) findViewById(R.id.tv_carWash_text);
        sportText = (TextView) findViewById(R.id.tv_sport_text);
        //下拉刷新
        swipeRefresh = findViewById(R.id.srl_refresh);
        swipeRefresh.setColorSchemeColors(R.color.colorPrimary);
        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });
        //手动切换城市
        drawerLayout = findViewById(R.id.drawer_layout);
        openDrawer = findViewById(R.id.bt_open_drawer);
        openDrawer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(this);

        String bing_ic = preferences.getString("bing_ic", null);
        if(bing_ic!=null){
            Glide.with(this).load(bing_ic).into(backgroundImg);
        }else{
            loadBackgroundImage();
        }

        String weather = preferences.getString("weather", null);
        if (weather != null) {
            //有缓存时直接从缓存中读取天气即可
            Weather weather1 = ParseUtil.handleWeatherResponse(weather);
            mWeatherId = weather1.basic.weatherId;
            showWeatherInfo(weather1);
        } else {
            //没有缓存就从服务器中读取天气数据
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);

        }
    }
/*获取图片*/
    private void loadBackgroundImage() {
        String address="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String imageUrl = response.body().string();
                if(imageUrl!=null){
                    //保存到缓存中
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
                    SharedPreferences.Editor edit = preferences.edit();
                    edit.putString("bing_ic",imageUrl);
                    edit.apply();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(imageUrl).into(backgroundImg);
                    }
                });
            }
        });
    }

    /*从服务器中提取数据*/
    public void requestWeather(String weather_id) {
        loadBackgroundImage();
        String address="http://guolin.tech/api/weather?cityid="+weather_id+"&key=bc0418b57b2d4918819d3974ac1285d9";
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                    }
                });
                swipeRefresh.setRefreshing(false);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //转换成weather对象
                final String s = response.body().string();
                final Weather weather = ParseUtil.handleWeatherResponse(s);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather!=null&&"ok".equals(weather.status)){
                            //将天气数据存入到缓存中
                            SharedPreferences.Editor edit = PreferenceManager.getDefaultSharedPreferences
                                    (WeatherActivity.this).edit();
                            edit.putString("weather",s);
                            edit.apply();
                            showWeatherInfo(weather);
                        }else{
                            //获取天气失败
                            Toast.makeText(WeatherActivity.this, "获取天气失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    /*展示weather对象中的天气*/
    private void showWeatherInfo(Weather weather) {
        String cityName2 = weather.basic.cityName;
        String updataTime2 = weather.basic.update.updataTime;
        String temperature = weather.now.temperature+"℃";
        String info = weather.now.more.info;
        cityName.setText(cityName2);
        updataTime.setText(updataTime2);
        nowTemp.setText(temperature);
        nowWeather.setText(info);
        forcaseLayout.removeAllViews();
        List<Forcast> forcastList = weather.forcastList;
        Log.d("SIZE", "showWeatherInfo: "+forcastList.size());
        for (Forcast forcast:forcastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forcast_item, forcaseLayout, false);
            TextView tvData = view.findViewById(R.id.tv_data);
            TextView tvInfo=view.findViewById(R.id.tv_weather_info);
            TextView maxTemp = view.findViewById(R.id.tv_max_temp);
            TextView minTemp = view.findViewById(R.id.tv_min_temp);
            tvData.setText(forcast.date);
            tvInfo.setText(forcast.more.info);
            maxTemp.setText(forcast.temperatrue.maxTemp);
            minTemp.setText(forcast.temperatrue.minTemp);
            forcaseLayout.addView(view);
        }
        if(weather.aqi!=null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25.setText(weather.aqi.city.pm25);
        }
        String comfort ="舒适度："+ weather.suggestion.comfort.info;
        String carWash ="洗车指数："+ weather.suggestion.carWash.info;
        String sportInfo ="运动指数："+ weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sportInfo);
        weatherLayout.setVisibility(View.VISIBLE);
    }
}
