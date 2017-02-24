package com.chinabike.plugins;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.chinabike.plugins.mip.AppContext;
import com.chinabike.plugins.mip.activity.LocalAlbum;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PermissionHelper;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Created by Administrator on 2016/2/3.
 */
public class MultiImagesPicker extends CordovaPlugin {

    private static final String TAG = "MultiImagesPicker";

    private static CordovaWebView webView = null;
    protected static Context context = null;

    private  CallbackContext callbackContext;
    private JSONObject params;

    private int maximumImagesCount;
    private int desiredWidth;
    private int desiredHeight;
    private int quality;

    public static final int PERMISSION_DENIED_ERROR = 20;
    protected final static String[] permissions = { Manifest.permission.READ_EXTERNAL_STORAGE };
    public static final int PICK_ALBUM_SEC = 1;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        this.callbackContext = callbackContext;
        this.params = args.getJSONObject(0);

        if (action.equals("getPictures")) {

            maximumImagesCount = 9;
            desiredWidth = 0;
            desiredHeight = 0;
            quality = 100;
            if (this.params.has("maximumImagesCount")) {
                maximumImagesCount = this.params.getInt("maximumImagesCount");
            }
            if (this.params.has("width")) {
                desiredWidth = this.params.getInt("width");
            }
            if (this.params.has("height")) {
                desiredHeight = this.params.getInt("height");
            }
            if (this.params.has("quality")) {
                quality = this.params.getInt("quality");
            }


            if(!PermissionHelper.hasPermission(this, permissions[0])) {
                PermissionHelper.requestPermissions(this, PICK_ALBUM_SEC, permissions);
            } else {
                this.getPictures(maximumImagesCount, desiredWidth,desiredHeight,quality);
            }



            return true;
        }

        return false;
    }

    public void getPictures(int maximumImagesCount, int desiredWidth, int desiredHeight, int quality) {


        Intent intent = new Intent(cordova.getActivity(), LocalAlbum.class);

        intent.putExtra("MAX_IMAGES", maximumImagesCount);
        intent.putExtra("WIDTH", desiredWidth);
        intent.putExtra("HEIGHT", desiredHeight);
        intent.putExtra("QUALITY", quality);
        cordova.startActivityForResult(this, intent, 200);

        PluginResult r = new PluginResult(PluginResult.Status.NO_RESULT);
        r.setKeepCallback(true);
        callbackContext.sendPluginResult(r);
    }


    public void onRequestPermissionResult(int requestCode, String[] permissions,
                                          int[] grantResults) throws JSONException {
        switch (requestCode) {
            case PICK_ALBUM_SEC:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    this.getPictures(maximumImagesCount, desiredWidth, desiredHeight, quality);
                } else {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(cordova.getActivity(), permissions[0])) {
                        showPermissionRationale(requestCode);
                    } else {
                        openPermissionSetting();
                        PluginResult result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                        callbackContext.sendPluginResult(result);
                    }
                }
                break;
        }
    }


    private void showPermissionRationale(final int requestCode) {
        showMessageOKCancel("You need to allow access Album", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                PermissionHelper.requestPermissions(MultiImagesPicker.this, requestCode, permissions);
            }
        }, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(requestCode == PICK_ALBUM_SEC){
                    PluginResult result = new PluginResult(PluginResult.Status.ILLEGAL_ACCESS_EXCEPTION);
                    callbackContext.sendPluginResult(result);
                }
            }
        });
    }

    private void openPermissionSetting() {
        showMessageOKCancel("This app needs to access Album", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Activity activity = cordova.getActivity();
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
            }
        }, null);
    }
    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener, DialogInterface.OnClickListener cancelListener) {
        new AlertDialog.Builder(cordova.getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, okListener)
                .setNegativeButton(android.R.string.cancel, cancelListener)
                .create()
                .show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        AppContext app = AppContext.getInstance();
        if (app.getResultCode() != 0) {
            resultCode = app.getResultCode();
        }
        ArrayList<String> fileNames = app.getFileNames();
        String errMsg = app.getErrMsg();
        if (resultCode == Activity.RESULT_OK && fileNames != null) {
//            ArrayList<String> fileNames = data.getStringArrayListExtra("MULTIPLEFILENAMES");
            JSONArray res = new JSONArray(fileNames);
            this.callbackContext.success(res);
            app.setResultCode(Activity.RESULT_CANCELED);  //还原未选中状态
        } else if (resultCode == Activity.RESULT_CANCELED && errMsg != null) {
//            String error = data.getStringExtra("ERRORMESSAGE");
            this.callbackContext.error(errMsg);
        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONArray res = new JSONArray();
            this.callbackContext.success(res);
        } else {
//            this.callbackContext.error("No images selected");
            this.callbackContext.error("没有选中照片");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "destroyed");
    }
}
