package com.openxu.bs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PicSizeActivity extends AppCompatActivity {

    private String TAG = "PicSizeActivity";

    @Bind(R.id.iv_jpg)
    ImageView iv_jpg;
    @Bind(R.id.tv_jpg)
    TextView tv_jpg;
    @Bind(R.id.iv_png1)
    ImageView iv_png1;
    @Bind(R.id.tv_png1)
    TextView tv_png1;
    @Bind(R.id.iv_png2)
    ImageView iv_png2;
    @Bind(R.id.tv_png2)
    TextView tv_png2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pic_size);
        ButterKnife.bind(this);

        measurePicSize();
    }



    private void measurePicSize(){
        try {
            //pic_jpg.jpg 硬盘信息 234*220 7.17 KB (7,350 字节)
            InputStream in = getResources().getAssets().open("pic_jpg.jpg");
            int lenght = in.available();
            BitmapFactory.Options newOpts = new BitmapFactory.Options();
            newOpts.inPreferredConfig = Bitmap.Config.ALPHA_8;     //一个像素＝1bites   此格式无效
            Bitmap bJpg1 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_4444;    //一个像素＝2bites  此格式不建议使用，无效
            Bitmap bJpg2 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;    //一个像素＝4bites  默认格式
            Bitmap bJpg3 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.RGB_565;    //一个像素＝2bites    此格式无效(jpg格式图片不支持透明度，是可以用这种格式的)
            Bitmap bJpg4 = BitmapFactory.decodeStream(in, null, newOpts);
            String log1 = "输入流大小："+lenght+"B\n"+
                    "pic_jpg用ALPHA_8格式打开占用 内存大小："+bJpg1.getByteCount()+"字节B\n"+
                    "pic_jpg用ARGB_4444格式打开占用 内存大小："+bJpg2.getByteCount()+"字节B\n"+
                    "pic_jpg用ARGB_8888格式打开占用 内存大小："+bJpg3.getByteCount()+"字节B\n"+
                    "pic_jpg用RGB_565格式打开占用 内存大小："+bJpg4.getByteCount()+"字节B";
            Log.v(TAG, log1);
            /*
                输入流大小：7350
                pic_jpg用ALPHA_8格式打开占用 内存大小：205920 = 234*220*4
                pic_jpg用ARGB_4444格式打开占用 内存大小：102960 = 234*220*2
                pic_jpg用ARGB_8888格式打开占用 内存大小：205920 = 234*220*4
                pic_jpg用RGB_565格式打开占用 内存大小：102960 = 234*220*2
             */

            //pic1.png硬盘信息：80*80  9.41 KB (9,646 字节)
            in = getResources().getAssets().open("pic1.png");
            lenght = in.available();
            newOpts.inPreferredConfig = Bitmap.Config.ALPHA_8;     //一个像素＝1bites   此格式无效
            Bitmap bitmap1 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_4444;    //一个像素＝2bites  此格式不建议使用，无效
            Bitmap bitmap2 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;    //一个像素＝4bites  默认格式
            Bitmap bitmap3 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.RGB_565;    //一个像素＝2bites    此格式无效
            Bitmap bitmap4 = BitmapFactory.decodeStream(in, null, newOpts);
            String log2 = "输入流大小："+lenght+"B\n"+
                    "pic1用ALPHA_8格式打开占用 内存大小："+bitmap1.getByteCount()+"字节B\n"+
                    "pic1用ARGB_4444格式打开占用 内存大小："+bitmap2.getByteCount()+"字节B\n"+
                    "pic1用ARGB_8888格式打开占用 内存大小："+bitmap3.getByteCount()+"字节B\n"+
                    "pic1用RGB_565格式打开占用 内存大小："+bitmap4.getByteCount()+"字节B";
            Log.v(TAG, log2);
            /*
                输入流大小：9646
                pic1用ALPHA_8格式打开占用 内存大小：25600 = 80*80*4
                pic1用ARGB_4444格式打开占用 内存大小：12800 = 80*80*2
                pic1用ARGB_8888格式打开占用 内存大小：25600 = 80*80*4
                pic1用RGB_565格式打开占用 内存大小：25600 = 80*80*4
             */

            //pic2.png硬盘信息：80*80  9.06 KB (9,282 字节)
            in = getResources().getAssets().open("pic2.png");
            lenght = in.available();
            newOpts.inPreferredConfig = Bitmap.Config.ALPHA_8;
            Bitmap bitmap5 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_4444;
            Bitmap bitmap6 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap7 = BitmapFactory.decodeStream(in, null, newOpts);
            newOpts.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bitmap8 = BitmapFactory.decodeStream(in, null, newOpts);
            String log3 = "输入流大小："+lenght+"B\n"+
                    "pic2用ALPHA_8格式打开占用 内存大小："+bitmap5.getByteCount()+"字节B\n"+
                    "pic2用ARGB_4444格式打开占用 内存大小："+bitmap6.getByteCount()+"字节B\n"+
                    "pic2用ARGB_8888格式打开占用 内存大小："+bitmap7.getByteCount()+"字节B\n"+
                    "pic2用RGB_565格式打开占用 内存大小："+bitmap8.getByteCount()+"字节B";
            Log.v(TAG, log3);
            /*
                输入流大小：9282
                pic1用ALPHA_8格式打开占用 内存大小：25600 = 80*80*4
                pic1用ARGB_4444格式打开占用 内存大小：12800 = 80*80*2
                pic1用ARGB_8888格式打开占用 内存大小：25600 = 80*80*4
                pic1用RGB_565格式打开占用 内存大小：25600 = 80*80*4
             */
            showResult(bJpg3,bitmap3,bitmap7, log1, log2, log3);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showResult(Bitmap b1, Bitmap b2, Bitmap b3, String s1, String s2, String s3){
        iv_jpg.setImageBitmap(b1);
        iv_png1.setImageBitmap(b2);
        iv_png2.setImageBitmap(b3);
        tv_jpg.setText(s1);
        tv_png1.setText(s2);
        tv_png2.setText(s3);
    }


}
