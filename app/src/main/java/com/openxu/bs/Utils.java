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
     * 根据系统基准比例获取默认的drawable文件夹的名称
     * @param density
     */
    public static String getDrawableName(float density){
        if(density == 0.75f){
            return "drawable-ldpi";
        }else if(density == 1){
            return "drawable-mdpi";
        }else if(density == 1.5f){
            return "drawable-hdpi";
        }else if(density == 2){
            return "drawable-xhdpi";
        }else if(density == 3){
            return "drawable-xxhdpi";
        }else if(density == 4){
            return "drawable-xxxhdpi";
        }
        return "";
    }

    /**
     * 根据drawable文件夹的名称获取对应的系统基准比例
     */
    public static float getDensityByDrawablename(String drawableName){
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
     * 根据图片存放目录名称和系统基准比例，计算图片需要放大或者缩小的比例
     * @param density
     */
    public static float getPicScale(String drawableName, float density){
        float drawbleDensity = getDensityByDrawablename(drawableName);
        return density/drawbleDensity;
    }




}
