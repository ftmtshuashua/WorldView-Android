package com.acap.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.acap.app.widget.MyWorldView;
import com.acap.toolkit.constant.TimeConstants;
import com.acap.toolkit.transform.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MyWorldView worldView = findViewById(R.id.view_WorldView);


        List<MyWorldView.Tab> array = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            long time = getTime(i);
            int ico = getIco();
            double temperature = random(18f, 34f);
            array.add(new MyWorldView.Tab(time, ico, temperature));
        }
        worldView.setData(array);

    }

    private float random(float start, float end) {
        float v = end - start;
        double v1 = Math.random() * v + start;
        return (float) v1;
    }

    private int random(int start, int end) {
        int v = end - start;
        double v1 = Math.random() * v + start;
        return (int) v1;
    }

    private int getIco() {
        int[] res = new int[]{R.mipmap.wlg_bloody_cloudy_day,
                R.mipmap.wlg_bloody_dust,
                R.mipmap.wlg_bloody_heavy_rain,
                R.mipmap.wlg_bloody_light_rain,
                R.mipmap.wlg_bloody_moderate_haze,
                R.mipmap.wlg_bloody_moderate_snow,
                R.mipmap.wlg_bloody_sunny_day,
                R.mipmap.wlg_bloody_thunder_rain
        };
        int random = random(0, res.length - 1);
        return res[random];
    }

    private long getTime(int i) {
        return TimeUtils.getNowMills() + TimeConstants.DAY * i;
    }
}