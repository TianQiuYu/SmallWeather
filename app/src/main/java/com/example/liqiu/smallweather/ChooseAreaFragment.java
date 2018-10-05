package com.example.liqiu.smallweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.liqiu.smallweather.db.City;
import com.example.liqiu.smallweather.db.Country;
import com.example.liqiu.smallweather.db.Province;
import com.example.liqiu.smallweather.util.HttpUtil;
import com.example.liqiu.smallweather.util.ParseUtil;

import org.litepal.crud.DataSupport;
import org.litepal.crud.LitePalSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {

    private static final int LEVEL_PROVINCE = 0;
    private static final int LEVEL_CITY = 1;
    private static final int LEVEL_COUNTY = 2;
    private TextView areaTitle;
    private Button back;
    private ListView listView;
    private View view;
    private int currentLevel;
    private List<String> dataList = new ArrayList<>();
    private ArrayAdapter adapter;
    private List<Province> provincesList;
    private Province selectedProvince;
    private List<City> cityList;
    private City selectedCity;
    private List<Country> countryList;
    private ProgressDialog progressDialog=null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.choose_area, container, false);
        areaTitle = view.findViewById(R.id.tv_title);
        back = view.findViewById(R.id.button_back);
        listView = view.findViewById(R.id.listview_area);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provincesList.get(position);
                    queryCity();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCountry();
                }else if(currentLevel==LEVEL_COUNTY){
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    Country country = countryList.get(position);
                    String weatherId = country.getWeatherId();
                    intent.putExtra("weather_id",weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCity();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvince();
                }
            }
        });
        //遍历省的数据
        queryProvince();
    }

    private void queryCountry() {
        areaTitle.setText(selectedCity.getCityName());
        back.setVisibility(View.VISIBLE);
        countryList = DataSupport.where("cityId=?", String.valueOf(selectedCity.getId())).find(Country.class);
        if (countryList.size() > 0) {
            dataList.clear();
            for (Country country : countryList) {
                dataList.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryDataFromServer(address, "country");
        }
    }

    private void queryCity() {
        areaTitle.setText(selectedProvince.getProvinceName());
        back.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceId=?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            String address = "http://guolin.tech/api/china/" + selectedProvince.getProvinceCode();
            queryDataFromServer(address, "city");
        }
    }

    /*b遍历所有省的数据，优先从数据库中查询，若果没有在从服务器中查询*/
    private void queryProvince() {
        areaTitle.setText("中国");
        back.setVisibility(View.GONE);
        provincesList = DataSupport.findAll(Province.class);
        if (provincesList.size() > 0) {
            dataList.clear();
            for (Province province : provincesList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            String address = "http://guolin.tech/api/china";
            queryDataFromServer(address, "province");
        }

    }

    /*从服务器获取所有数据*/
    private void queryDataFromServer(String address, final String type) {
        //显示对话框
        showDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        colseDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String string = response.body().string();
                Log.d("onResponse", "onResponse: "+string);
                Boolean result = false;
                if ("province".equals(type)) {
                    result = ParseUtil.handleProvinceData(string);
                } else if ("city".equals(type)) {
                    result = ParseUtil.handleCityData(string, selectedProvince.getId());
                } else if ("country".equals(type)) {
                    result = ParseUtil.handleCountryData(string, selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            colseDialog();
                            if ("province".equals(type)) {
                                queryProvince();
                            } else if ("city".equals(type)) {
                                queryCity();
                            } else if ("country".equals(type)) {
                                queryCountry();
                            }
                        }
                    });
                }
            }
        });
    }



    /*显示对话框*/
    private void showDialog() {
        if(progressDialog==null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载!!!!");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();

    }
    /*关闭对话框*/
    private void colseDialog() {
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }
}
