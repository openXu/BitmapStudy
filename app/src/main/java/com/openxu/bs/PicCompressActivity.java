package com.openxu.bs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import butterknife.Bind;
import butterknife.ButterKnife;

public class PicCompressActivity extends AppCompatActivity {

    private String TAG = "PicCompressActivity";

    @Bind(R.id.iv_1)
    ImageView iv_1;
    @Bind(R.id.iv_2)
    ImageView iv_2;

    @Bind(R.id.tv_fbl)
    TextView tv_fbl;
    @Bind(R.id.tv_hit1)
    TextView tv_hit1;
    @Bind(R.id.tv_hit2)
    TextView tv_hit2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compress);
        ButterKnife.bind(this);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        tv_fbl.setText("系统分辨率:"+dm.widthPixels+"*"+dm.heightPixels);


        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pic_res);
        Log.i(TAG, "原始bitmap大小："+bitmap.getWidth()+"*"+bitmap.getHeight());
        tv_hit1.setText("原图展示："+bitmap.getWidth()+"*"+bitmap.getHeight());
        iv_1.setImageBitmap(bitmap);



    }



    /**
     * 尺寸压缩
     * @param srcPath 原图片路径
     * @param upDir 压缩之后的缓存路径
     * @return
     */
    public Bitmap getimage(String srcPath, String upDir) {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        // 开始读入图片，此时把options.inJustDecodeBounds 设回true了
        newOpts.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(srcPath, newOpts);// 此时返回bm为空
        int w = newOpts.outWidth;
        int h = newOpts.outHeight;
        // 现在主流手机比较多是800*480分辨率，所以高和宽我们设置为
        float hh = 800f;
        float ww = 480f;
        // 缩放比，只用高或者宽其中一个数据进行计算即可
        int be = 1;// be=1表示不缩放
        if (h > hh || w > ww) {
            final int heightRatio = Math.round((float) h/ (float) hh);
            final int widthRatio = Math.round((float) w / (float) ww);
            be = heightRatio < widthRatio ? heightRatio : widthRatio;
        }

       /* if (w > h && w > ww) {// 如果宽度大的话根据宽度固定大小缩放
            be = (int) (newOpts.outWidth / ww);
        } else if (w < h && h > hh) {// 如果高度高的话根据宽度固定大小缩放
            be = (int) (newOpts.outHeight / hh);
        }
        if (be <= 0)
            be = 1;*/
        Log.d(TAG, "比例压缩之前图片大小为："+w+  "*" + h +",比例缩放比："+be);
        newOpts.inSampleSize = be;// 设置缩放比例
        newOpts.inJustDecodeBounds = false;
        // 重新读入图片，注意此时已经把options.inJustDecodeBounds 设回false了
        bitmap = BitmapFactory.decodeFile(srcPath, newOpts);
        Log.d(TAG, "比例压缩之后图片大小为："+bitmap.getWidth()+  "*" + bitmap.getHeight());
        bitmap = compressImage(bitmap);
        File bitFile = saveBitMaptoSdcard(bitmap, upDir);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int options = 100;
            bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);// 质量压缩方法，这里100表示不
            Log.d(TAG, "压缩之后图片大小为："+  ",size=" + (baos.size()/1024)+"kb");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitFile;
    }

    /**
     * 将图片压缩到200kb以内
     * @param image
     * @return
     */
    private Bitmap compressImage(Bitmap image) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int options = 100;
        image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        while (baos.toByteArray().length / 1024 > 200) { // 循环判断如果压缩后图片是否大于500kb,大于继续压缩
            Log.i(TAG, "压缩后图片大小："+(baos.toByteArray().length / 1024)+",太大，继续压缩");
            baos.reset();// 重置baos即清空baos
            options -= 10;// 每次都减少10
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);// 这里压缩options%，把压缩后的数据存放到baos中
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());// 把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);// 把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    /**
     * 将图片存储到指定文件夹
     * @param bitmap
     * @param dir
     * @return
     */
    private File saveBitMaptoSdcard(Bitmap bitmap, String dir) {
        // 得到外部存储卡的路径
        // ff.png是将要存储的图片的名称
        File dir_file = new File(dir);
        if (!dir_file.exists() && !dir_file.isDirectory()) {
            // 文件夹不存在，则创建文件夹
            dir_file.mkdir();
        }
        File file = new File(dir, "/"+ System.currentTimeMillis() + ".png");
        Log.v(TAG, "svaeBitmapSdacrd===" + file.getAbsolutePath());
        // 从资源文件中选择一张图片作为将要写入的源文件
        try {
            FileOutputStream out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }






}
