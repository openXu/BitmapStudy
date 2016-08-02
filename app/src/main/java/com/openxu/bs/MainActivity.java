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

        Log.v(TAG, "进程可用总的内存大小："+Runtime.getRuntime().maxMemory()
                +",进程已用的内存大小:"+Runtime.getRuntime().totalMemory() );
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

        String result = "";
        try {
            //pic1.png基本信息：80*80  9.41 KB (9,646 字节)
            InputStream in = getResources().getAssets().open("pic1.png");
            int lenght = in.available();
            Log.v(TAG, "输入流大小："+lenght+"B");   //9646
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inPreferredConfig = Bitmap.Config.ALPHA_8;     //一个像素＝1bites
            Bitmap bitmap1 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_4444;    //一个像素＝2bites
            Bitmap bitmap2 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;    //一个像素＝4bites
            Bitmap bitmap3 = BitmapFactory.decodeStream(in, null, newOpts);
            //android.content.res.Resources来取得一个张图片时，它也是以RGB_565格式来构建BitMap的
            //从 Android4.0 开始，该选项无效。即使设置为该值，系统任然会采用  ARGB_8888 来构造图片
            newOpts.inPreferredConfig = Bitmap.Config.RGB_565;    //一个像素＝2bites
            Bitmap bitmap4 = BitmapFactory.decodeStream(in, null, newOpts);
            Log.v(TAG, "pic1用ALPHA_8模式打开占用内容大小："+bitmap1.getByteCount()+"字节B");    //25600 = 4*80*80
            Log.v(TAG, "pic1用ARGB_4444模式打开占用内容大小："+bitmap2.getByteCount()+"字节B");  //12800 = 2*80*80
            Log.v(TAG, "pic1用ARGB_8888模式打开占用内容大小："+bitmap3.getByteCount()+"字节B");  //25600 = 4*80*80
            Log.v(TAG, "pic1用RGB_565模式打开占用内容大小："+bitmap4.getByteCount()+"字节B");    //25600 = 4*80*80


            //pic2.png基本信息：80*80  9.06 KB (9,282 字节)
            in = getResources().getAssets().open("pic2.png");
            lenght = in.available();
            Log.v(TAG, "输入流大小："+lenght+"B");   //9282
            newOpts.inPreferredConfig = Bitmap.Config.ALPHA_8;     //一个像素＝1bites
            Bitmap bitmap5 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_4444;    //一个像素＝2bites
            Bitmap bitmap6 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;    //一个像素＝4bites
            Bitmap bitmap7 = BitmapFactory.decodeStream(in, null, newOpts);
            //android.content.res.Resources来取得一个张图片时，它也是以RGB_565格式来构建BitMap的
            //从 Android4.0 开始，该选项无效。即使设置为该值，系统任然会采用  ARGB_8888 来构造图片
            newOpts.inPreferredConfig = Bitmap.Config.RGB_565;    //一个像素＝2bites
            Bitmap bitmap8 = BitmapFactory.decodeStream(in, null, newOpts);
            Log.v(TAG, "pic2用ALPHA_8模式打开占用内容大小："+bitmap5.getByteCount()+"字节B");    //25600 = 4*80*80
            Log.v(TAG, "pic2用ARGB_4444模式打开占用内容大小："+bitmap6.getByteCount()+"字节B");  //12800 = 2*80*80
            Log.v(TAG, "pic2用ARGB_8888模式打开占用内容大小："+bitmap7.getByteCount()+"字节B");  //25600 = 4*80*80
            Log.v(TAG, "pic2用RGB_565模式打开占用内容大小："+bitmap8.getByteCount()+"字节B");    //25600 = 4*80*80



        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
