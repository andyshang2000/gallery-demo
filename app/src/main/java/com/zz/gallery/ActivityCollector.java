package com.zz.gallery;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class ActivityCollector {
    private static List<Activity> activityList = new ArrayList<>();

    public static void addActivity(Activity activity) {
        activityList.add(activity);
    }

    public static void removeActivity(Activity activity) {
        activityList.remove(activity);
    }

    // 获取栈顶Activity
    public static Activity getTopActivity() {
        if (activityList.isEmpty())
            return null;
        return activityList.get(activityList.size() - 1);
    }
}