package com.example.liqiu.smallweather.gson;

import com.google.gson.annotations.SerializedName;

public class Forcast {
    public String date;
    @SerializedName("tmp")
    public Temperatrue temperatrue;
    @SerializedName("cond")
    public More more;

    public class Temperatrue{
        @SerializedName("max")
        public String maxTemp;
        @SerializedName("min")
        public String minTemp;
    }

    public class More{
        @SerializedName("txt_d")
        public String info;
    }
}
