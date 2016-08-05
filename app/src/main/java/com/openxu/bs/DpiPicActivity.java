package com.openxu.bs;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewTreeObserver;
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
        float scale = dm.density;
        String drawbleName = "drawable-xxhdpi";

        //要在控件绘制完成后才能获取到相关信息，所以这里要监听绘制状态
        imageView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener()     {
            public boolean onPreDraw() {
                String hint =  "图片的原始宽高: 360*360\n"+
                        "系统dpi为："+densityDpi+"scale为："+scale+"\n"+
                        "应该从"+Utils.getDrawableName(scale)+"中加载图片\n"+
                        "当前图片存放在："+drawbleName+"中, 他的scale为："+Utils.getScaleByDrawablename(drawbleName)+"\n"+
                        "应该缩放的比例："+Utils.getPicScale(drawbleName, scale)+"\n"+
                        "加载到内存中后图片的宽高:"+imageView.getHeight() + "*" + imageView.getWidth()+"\n";

                Log.d(TAG, hint);
                tv_hit.setText(hint);

                return true;
            }
        });

    }






}
