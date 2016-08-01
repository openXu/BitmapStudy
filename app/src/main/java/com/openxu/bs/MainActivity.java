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
        //pic1  223*220
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
            //android.content.res.Resources来取得一个张图片时，它也是以该格式来构建BitMap的
            //从 Android4.0 开始，该选项无效。即使设置为该值，系统任然会采用  ARGB_8888 来构造图片
            newOpts.inPreferredConfig = Bitmap.Config.ALPHA_8;    //一个像素＝1bites
            Bitmap bitmap1 = BitmapFactory.decodeStream(in, null, newOpts);
            Log.v(TAG, "ALPHA_8占用内容大小："+bitmap1.getByteCount()+"B");   //196240 = 4*223*220

            newOpts = new BitmapFactory.Options();
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_4444;    //一个像素＝2bites
            Bitmap bitmap2 = BitmapFactory.decodeStream(in, null, newOpts);
            Log.v(TAG, "ARGB_4444占用内容大小："+bitmap2.getByteCount()+"B");   //98120 = 2*223*220

            newOpts = new BitmapFactory.Options();
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;    //一个像素＝4bites
            Bitmap bitmap3 = BitmapFactory.decodeStream(in, null, newOpts);
            Log.v(TAG, "ARGB_8888占用内容大小："+bitmap1.getByteCount()+"B");   //196240 = 4*223*220

            newOpts = new BitmapFactory.Options();
            newOpts.inPreferredConfig = Bitmap.Config.RGB_565;    //一个像素＝2bites
            Bitmap bitmap4 = BitmapFactory.decodeStream(in, null, newOpts);
            Log.v(TAG, "RGB_565占用内容大小："+bitmap4.getByteCount()+"B");   //98120 = 2*223*220

            Log.v(TAG, "进程可用总的内存大小："+Runtime.getRuntime().maxMemory()
                    +",进程已用的内存大小:"+Runtime.getRuntime().totalMemory() );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
