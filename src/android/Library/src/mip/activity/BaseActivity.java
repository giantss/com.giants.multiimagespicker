package com.chinabike.plugins.mip.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.chinabike.plugins.mip.AppContext;
import com.chinabike.plugins.mip.AppManager;
import com.chinabike.plugins.mip.common.LocalImageHelper;

/**
 * @Description:Activity基类
 * @author linjizong
 * @date 2015-3-18
 */

/**
 * @Description:
 * @author linjizong
 * @date 2015-3-30
 */
public class BaseActivity extends Activity {

	protected AppContext app;
	protected LocalImageHelper helper;

    //应用是否销毁标志
	protected boolean isDestroy;
	//防止重复点击设置的标志，涉及到点击打开其他Activity时，将该标志设置为false，在onResume事件中设置为true
	private boolean clickable=true;

	private Display display;


	protected int maximumImagesCount;
	protected int desiredWidth;
	protected int desiredHeight;
	protected int quality;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = (AppContext)getApplication();
		helper = LocalImageHelper.getInstance();
		//本地图片辅助类初始化
		if (display == null) {
			WindowManager windowManager = (WindowManager)
					getSystemService(Context.WINDOW_SERVICE);
			display = windowManager.getDefaultDisplay();
		}
		isDestroy=false;
		//设置无标题
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		//垂直显示
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// 添加Activity到堆栈
		AppManager.getAppManager().addActivity(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		isDestroy=true;
		// 结束Activity&从堆栈中移除
		AppManager.getAppManager().finishActivity(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		//每次返回界面时，将点击标志设置为可点击
		clickable=true;
	}

	/**
	 * 当前是否可以点击
	 * @return
	 */
	protected boolean isClickable(){
		return  clickable;
	}

	/**
	 * 锁定点击
	 */
	protected void lockClick(){
		clickable=false;
	}



	@Override
	public void startActivityForResult(Intent intent, int requestCode, Bundle options) {
		if(isClickable()) {
			lockClick();
			super.startActivityForResult(intent, requestCode,options);
		}
	}

	/**
	 *   设置滤镜
	 */
	private void setFilter(ImageView view) {
		//先获取设置的src图片
		Drawable drawable=view.getDrawable();
		//当src图片为Null，获取背景图片
		if (drawable==null) {
			drawable=view.getBackground();
		}
		if(drawable!=null){
			//设置滤镜
			drawable.setColorFilter(Color.GRAY, PorterDuff.Mode.MULTIPLY);;
		}
	}
	/**
	 *   清除滤镜
	 */
	private void removeFilter(ImageView view) {
		//先获取设置的src图片
		Drawable drawable=view.getDrawable();
		//当src图片为Null，获取背景图片
		if (drawable==null) {
			drawable=view.getBackground();
		}
		if(drawable!=null){
			//清除滤镜
			drawable.clearColorFilter();
		}
	}

	protected Object[] getImgInfo(Uri contentUri) {
		String res = null;
		int rotation = 0;
		String[] proj = {MediaStore.Images.Media.DATA};
		Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
		if (cursor.moveToFirst()) {
			int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			res = cursor.getString(column_index);
			rotation = cursor.getInt(column_index);
		}
		cursor.close();
		Object[] objects = new Object[2];
		objects[0] = res;
		objects[1] = rotation;
		return objects;
	}
}
