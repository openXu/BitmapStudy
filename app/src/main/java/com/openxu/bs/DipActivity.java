package com.openxu.bs;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DipActivity extends AppCompatActivity {
Activity
    private String TAG = "DipActivity";

    @Bind(R.id.btn_dip)
    Button btn_dip;

    @Bind(R.id.btn_px)
    Button btn_px;
    @Bind(R.id.tv_fbl)
    TextView tv_fbl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dip);
        ButterKnife.bind(this);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        tv_fbl.setText("系统分辨率:"+dm.widthPixels+"*"+dm.heightPixels);

        //要在控件绘制完成后才能获取到相关信息，所以这里要监听绘制状态
        btn_dip.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()     {
            public boolean onPreDraw() {
                Log.d(TAG, "dip:"+btn_dip.getHeight() + "*" + btn_dip.getWidth());
                return true;
            }
        });
        btn_px.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()     {
            public boolean onPreDraw() {
                Log.d(TAG, "px:"+btn_px.getHeight() + "*" + btn_px.getWidth());
                return true;
            }
        });
    }






}
