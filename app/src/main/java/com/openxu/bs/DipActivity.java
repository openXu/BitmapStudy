package com.openxu.bs;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.Button;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DipActivity extends AppCompatActivity {

    private String TAG = "DipActivity";

    @Bind(R.id.btn_dip)
    Button btn_dip;

    @Bind(R.id.btn_px)
    Button btn_px;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dip);
        ButterKnife.bind(this);

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
