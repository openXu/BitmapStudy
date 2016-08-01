package com.openxu.bs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.InputStream;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    @Bind(R.id.btn_local_pic)
    Button btnLocalPic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }


    @OnClick(R.id.btn_local_pic)
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_local_pic:
                measurePicSize();
                break;
        }

    }

    private void measurePicSize(){
        //pic1  233*220
        //实际大小：93.4 KB (95,646 字节)
        //占用空间96.0 KB (98,304 字节)
        String result = "";
        try {
            InputStream in = getResources().getAssets().open("pic1.png");
            //获取文件的字节数
            int lenght = in.available();
            //创建byte数组
            Log.v(TAG, "输入流大小："+lenght+"B");   //95646

            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inPreferredConfig = Bitmap.Config.ALPHA_8;
            Bitmap bitmap = BitmapFactory.decodeStream(in, null, newOpts);
            int w = newOpts.outWidth;
            int h = newOpts.outHeight;
            Log.v(TAG, "图片宽高："+w+"*"+h);       //223*220
            Log.v(TAG, "bitmap占用内容大小："+bitmap.getByteCount()+"B");   //196240

            Log.v(TAG, "进程可用总的内存大小："+Runtime.getRuntime().maxMemory()
                    +",进程已用的内存大小:"+Runtime.getRuntime().totalMemory() );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
