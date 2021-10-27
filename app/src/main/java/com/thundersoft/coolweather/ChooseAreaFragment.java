package com.thundersoft.coolweather;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.thundersoft.coolweather.db.City;
import com.thundersoft.coolweather.db.Country;
import com.thundersoft.coolweather.db.Province;
import com.thundersoft.coolweather.util.HttpUtil;
import com.thundersoft.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTRY = 2;

    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 区列表
     */
    private List<Country> countryList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 选中的区
     */
    private Country selectedCountry;

    /**
     * 当前选中的级别
     */
    private int currenrLevel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.choose_area,container,false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                if (currenrLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if(currenrLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currenrLevel == LEVEL_COUNTRY){
                    queryCities();
                }else if(currenrLevel == LEVEL_CITY){
                    queryProvinces();
                }
            }
        });


    }

    /**
     * 查询全国所有的省，优先从数据库中查询，如果没有查询到，再去服务器中查询
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);

        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0){
            dataList.clear();
            for (Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currenrLevel = LEVEL_PROVINCE;
        }else{
            String address = "http://guolin.tecg/api/china";
            queryFromServer(address,"province");
        }
    }

    /**
     * 查询全国所有的市，优先从数据库中查询，如果没有查询到，再去服务器中查询
     */
    private void queryCounties() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);

        cityList = DataSupport.where("provinceid = ?",String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0){
            dataList.clear();
            for (City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currenrLevel = LEVEL_CITY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tecg/api/china/"+provinceCode;
            queryFromServer(address,"city");
        }
    }

    /**
     * 查询全国所有的区，优先从数据库中查询，如果没有查询到，再去服务器中查询
     */
    private void queryCities() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);

        countryList = DataSupport.where("cityid = ?",String.valueOf(selectedCity.getId())).find(Country.class);
        if (countryList.size() > 0){
            dataList.clear();
            for (Country country:countryList){
                dataList.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currenrLevel = LEVEL_COUNTRY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tecg/api/china/"+provinceCode+"/"+cityCode;
            queryFromServer(address,"country");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市区数据
     * @param address
     * @param type
     */
    private void queryFromServer(String address, final String type) {
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if("country".equals(type)){
                    result = Utility.handleCountryResponse(responseText,selectedCity.getId());
                }

                if (result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)){
                                queryProvinces();
                            }else if("city".equals(type)){
                                queryCities();
                            }else if("country".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialod(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
