package com.coolweather.app.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.app.R;
import com.coolweather.app.db.CoolWeatherDB;
import com.coolweather.app.model.City;
import com.coolweather.app.model.County;
import com.coolweather.app.model.Province;
import com.coolweather.app.util.HttpCallbackListener;
import com.coolweather.app.util.HttpUtil;
import com.coolweather.app.util.Utility;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by itoysk on 2016/8/31.
 */
public class ChooseAreaActivity extends Activity{

    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private CoolWeatherDB coolWeatherDB;
    private List<String> dataList = new ArrayList<>();

    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;

    private Province selectedProvince;
    private City selectedCity;
    private int currentLevel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.choose_area);
        listView = (ListView) findViewById(R.id.list_view);
        titleText = (TextView) findViewById(R.id.title_text);
        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);
        coolWeatherDB = CoolWeatherDB.getInstance(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(i);
                    queryCities();
                }
                else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(i);
                    queryCounties();
                }
            }
        });
        queryProvinces();//加载省级数据
    }

    /**
     * 查询所有省，优先数据库查询
     */
    private void queryProvinces() {
        provinceList = coolWeatherDB.loadProvinces();
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText("中国");
            currentLevel = LEVEL_PROVINCE;
        }
        else {
            queryFromServer(null,"province");
        }
    }

    /**
     * 查询省内所有市，优先数据库查询
     */
    private void queryCities() {
        cityList = coolWeatherDB.loadCities(selectedProvince.getId());
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedProvince.getProvinceName());
            currentLevel = LEVEL_CITY;
        }
        else {
            queryFromServer(selectedProvince.getProvinceCode(),"city");
        }
    }

    /**
     * 查询市内所有县，优先数据库查询
     */
    private void queryCounties() {
        countyList = coolWeatherDB.loadCounties(selectedCity.getId());
        if (countyList.size() > 0) {
            dataList.clear();
            for (County county : countyList) {
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            titleText.setText(selectedCity.getCityName());
            currentLevel = LEVEL_COUNTY;
        }
        else {
            queryFromServer(selectedCity.getCityCode(),"city");
        }
    }


    /**
     * 根据传入代号和类型从服务器上查询数据
     * @param code
     * @param type
     */
    private void queryFromServer(final String code, final String type) {
        String address;
        if(!TextUtils.isEmpty(code)){
            address =  "http://www.weather.com.cn/data/list3/city" + code +".xml";
        }
        else{
            address = "http://www.weather.com.cn/data/list3/city.xml";
        }
        showProcessDialog();
        HttpUtil.sendHttpRequest(address, new HttpCallbackListener() {
            @Override
            public void onFinish(String response) {
                boolean result = false;
                if("province".equals(type)){
                    result = Utility.handleProvinceResponse(coolWeatherDB,response);
                }
                else if("city".equals(type)){
                    result = Utility.handleCitiesResponse(coolWeatherDB,response,selectedProvince.getId());
                }
                else if("county".equals(type)){
                    result = Utility.handleCountiesResponse(coolWeatherDB,response,selectedCity.getId());
                }

                if(result){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }
                            else if("city".equals(type)){
                                queryCities();
                            }
                            else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(ChooseAreaActivity.this,"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }



    private void showProcessDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage("正在加载......");
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog() {
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }

    /**
     * 捕获Back按键，根据当前级别来判断是返回市列表还是省列表，还是直接退出
     */
    @Override
    public void onBackPressed() {
        if(currentLevel == LEVEL_COUNTY){
            queryCities();
        }
        else if(currentLevel == LEVEL_CITY){
            queryProvinces();
        }
        else{
            finish();
        }
    }
}