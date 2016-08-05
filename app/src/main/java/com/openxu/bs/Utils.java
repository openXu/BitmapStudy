package com.openxu.bs;

/**
 * author : openXu
 * create at : 2016/8/5 16:31
 * blog : http://blog.csdn.net/xmxkf
 * gitHub : https://github.com/openXu
 * project : BitmapStudy
 * class name : Utils
 * version : 1.0
 * class describe：
 */
public class Utils {

    /**
     * 根据基准比例获取首先加载图片资源的drawable文件夹的名称
     * @param scale
     */
    public static String getDrawableName(float scale){
        if(scale == 0.75f){
            return "drawable-ldpi";
        }else if(scale == 1){
            return "drawable-mdpi";
        }else if(scale == 1.5f){
            return "drawable-hdpi";
        }else if(scale == 2){
            return "drawable-xhdpi";
        }else if(scale == 3){
            return "drawable-xxhdpi";
        }else if(scale == 4){
            return "drawable-xxxhdpi";
        }
        return "";
    }

    /**
     * 根据基准比例获取首先加载图片资源的drawable文件夹的名称
     */
    public static float getScaleByDrawablename(String drawableName){
        switch (drawableName){
            case "drawable":
                return 1;
            case "drawable-ldpi":
                return 0.75f;
            case "drawable-mdpi":
                return 1;
            case "drawable-hdpi":
                return 1.5f;
            case "drawable-xhdpi":
                return 2;
            case "drawable-xxhdpi":
                return 3;
            case "drawable-xxxhdpi":
                return 4;
        }
        return -1;
    }


    /**
     * 获取不同drawable目录图片被加载后，与图片的原始大小的缩放比例
     * @param scale
     */
    public static float getPicScale(String drawableName, float scale){
        float drawbleScale = 0;
        switch (drawableName){
            case "drawable":
                drawbleScale = 1;
                break;
            case "drawable-ldpi":
                drawbleScale = 0.75f;
                break;
            case "drawable-mdpi":
                drawbleScale = 1;
                break;
            case "drawable-hdpi":
                drawbleScale = 1.5f;
                break;
            case "drawable-xhdpi":
                drawbleScale = 2;
                break;
            case "drawable-xxhdpi":
                drawbleScale = 3;
                break;
            case "drawable-xxxhdpi":
                drawbleScale = 4;
                break;
        }
        return scale/drawbleScale;
    }




}
