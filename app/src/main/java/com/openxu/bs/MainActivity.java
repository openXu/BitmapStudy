package com.openxu.bs;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        Log.v(TAG, "进程可用总的内存大小：" + Runtime.getRuntime().maxMemory()
                + ",进程已用的内存大小:" + Runtime.getRuntime().totalMemory());
    }


    @OnClick({R.id.btn_local_pic, R.id.btn_dip, R.id.btn_dpi_pic})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_local_pic:
                startActivity(new Intent(this, PicSizeActivity.class));
                break;
            case R.id.btn_dip:
                startActivity(new Intent(this, DipActivity.class));
                break;
            case R.id.btn_dpi_pic:
                startActivity(new Intent(this, DpiPicActivity.class));
                break;
            case R.id.btn_pic_compress:
                startActivity(new Intent(this, PicCompressActivity.class));
                break;
        }
    }



}
