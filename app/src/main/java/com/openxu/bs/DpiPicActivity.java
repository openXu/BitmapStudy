package com.openxu.bs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.Bind;
import butterknife.ButterKnife;

public class DpiPicActivity extends AppCompatActivity {

    private String TAG = "DpiPicActivity";

    @Bind(R.id.imageView)
    ImageView imageView;

    @Bind(R.id.tv_hit)
    TextView tv_hit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dpi_pic);
        ButterKnife.bind(this);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int densityDpi = dm.densityDpi;
        float systemDensity = dm.density;
        String drawbleName = "drawable-xxhdpi";

        float dirDensity = Utils.getDensityByDrawablename(drawbleName);
        float scale = Utils.getPicScale(drawbleName, systemDensity);
//        BitmapFactory.decodeResource()
//        imageView.setImageResource();
//        getResources().getDrawable()

        TypedValue value = new TypedValue();
        getResources().getValue(R.drawable.pic_res, value, true);
        Log.i(TAG, ""+value);
        //720*1280  TypedValue{t=0x3/d=0xa2 "res/drawable-xxhdpi-v4/pic_dpi.png" a=2 r=0x7f02004c}
        //1080*1920 TypedValue{t=0x3/d=0x72 "res/drawable-xxhdpi-v4/pic_dpi.png" a=5 r=0x7f02004c}

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pic_res);
        Log.i(TAG, "bitmap大小："+bitmap.getWidth()+"*"+bitmap.getHeight());
        imageView.setImageBitmap(bitmap);


        String hint =  "手机分辨率: "+dm.widthPixels+"*"+dm.heightPixels+"\n"+
                "dpi："+densityDpi+"  density："+systemDensity+"\n"+
                "默认从"+Utils.getDrawableName(systemDensity)+"中加载图片\n\n"+
                "图片的原始宽高: 360*360\n"+
                "当前图片存放在："+drawbleName+"中, density："+dirDensity+"\n"+
                (systemDensity==dirDensity? "不用对图片做缩放处理 ":(systemDensity>dirDensity?
                        "应该将图片放大"+scale+"倍后展示":
                        "应该将图片缩放为原来的"+scale))+"\n"+
                "加载到内存中后图片的宽高:"+bitmap.getWidth() + "*" + bitmap.getWidth()+"\n";

        Log.d(TAG, hint);
        tv_hit.setText(hint);

    }






}
