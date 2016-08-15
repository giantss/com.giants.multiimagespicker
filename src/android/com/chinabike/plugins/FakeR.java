package com.chinabike.plugins;

import android.app.Activity;
import android.content.Context;

/**
 * Created by Administrator on 2016/2/25.
 */
public class FakeR {
    private Context context;
    private String packageName;

    public FakeR(Activity activity) {
        context = activity.getApplicationContext();
        packageName = context.getPackageName();
    }

    public FakeR(Context context) {
        this.context = context;
        packageName = context.getPackageName();
    }

    public int getId(String group, String key) {
        return context.getResources().getIdentifier(key, group, packageName);
    }

    public static int getId(Context context, String group, String key) {
        return context.getResources().getIdentifier(key, group, context.getPackageName());
    }
}
