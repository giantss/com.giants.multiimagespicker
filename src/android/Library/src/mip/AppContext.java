package com.chinabike.plugins.mip;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.Display;
import android.view.WindowManager;

import com.chinabike.plugins.mip.common.LocalImageHelper;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import org.apache.cordova.PermissionHelper;

import java.io.File;
import java.util.ArrayList;

//

/**
 * 全局应用程序类：用于保存和调用全局应用配置及访问网络数据
 *
 * @author linjizong
 * @created 2015-3-22
 */
public class AppContext extends Application {
    private static final String TAG = AppContext.class.getSimpleName();
    private static final String APP_CACAHE_DIRNAME = "/webcache";

    //singleton
    private static AppContext appContext = null;
    private Display display;

    private int resultCode;
    private ArrayList<String> fileNames;
    private String errMsg;

    public void setResultCode(int resultCode) {
        this.resultCode = resultCode;
    }

    public int getResultCode() {
        return resultCode;
    }

    public void setFileNames(ArrayList<String> fileNames) {
        this.fileNames = fileNames;
    }

    public ArrayList<String> getFileNames() {
        return fileNames;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public String getErrMsg() {
        return errMsg;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appContext = this;
        init();
    }
	
	    /**
     * http://blog.csdn.net/yanzhenjie1003/article/details/51818269 android-support-multidex解决65536
     * @param base
     */
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public static AppContext getInstance() {
        return appContext;
    }

    /**
     * 初始化
     */
    private void init() {
        reloadImages();
        if (display == null) {
            WindowManager windowManager = (WindowManager)
                    getSystemService(Context.WINDOW_SERVICE);
            display = windowManager.getDefaultDisplay();
        }
    }

    public void reloadImages() {
        initImageLoader(getApplicationContext());
        //本地图片辅助类初始化

        //LocalImageHelper.init(this);
    }




    public static void initImageLoader(Context context) {
        ImageLoaderConfiguration.Builder config = new ImageLoaderConfiguration.Builder(context);
        config.threadPriority(Thread.NORM_PRIORITY);
        config.denyCacheImageMultipleSizesInMemory();
        config.memoryCacheSize((int) Runtime.getRuntime().maxMemory() / 4);
        config.diskCacheFileNameGenerator(new Md5FileNameGenerator());
        config.diskCacheSize(100 * 1024 * 1024); // 100 MiB
        config.tasksProcessingOrder(QueueProcessingType.LIFO);
        //修改连接超时时间5秒，下载超时时间5秒
        config.imageDownloader(new BaseImageDownloader(appContext, 5 * 1000, 5 * 1000));
        //		config.writeDebugLogs(); // Remove for release app
        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config.build());
    }

    public String getCachePath() {
        File cacheDir;
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            cacheDir = getExternalCacheDir();
        else
            cacheDir = getCacheDir();
        if (!cacheDir.exists())
            cacheDir.mkdirs();
        return cacheDir.getAbsolutePath();
    }

    /**
     * @return
     * @Description： 获取当前屏幕的宽度
     */
    public int getWindowWidth() {
        return display.getWidth();
    }

    /**
     * @return
     * @Description： 获取当前屏幕的高度
     */
    public int getWindowHeight() {
        return display.getHeight();
    }

    /**
     * @return
     * @Description： 获取当前屏幕一半宽度
     */
    public int getHalfWidth() {
        return display.getWidth() / 2;
    }

    /**
     * @return
     * @Description： 获取当前屏幕1/4宽度
     */
    public int getQuarterWidth() {
        return display.getWidth() / 4;
    }
}
