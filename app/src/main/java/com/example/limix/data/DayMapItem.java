package com.example.limix.data;

import android.graphics.drawable.Drawable;

public class DayMapItem {

    private int id;
    private String packageName;
    private String appName;
    private String taskDescription;
    private int position;
    private boolean isVisited;
    private boolean isCompleted;
    private Drawable icon;

    public DayMapItem(int id, String packageName, String appName,
                      String taskDescription, int position,
                      boolean isVisited, boolean isCompleted, Drawable icon) {
        this.id = id;
        this.packageName = packageName;
        this.appName = appName;
        this.taskDescription = taskDescription;
        this.position = position;
        this.isVisited = isVisited;
        this.isCompleted = isCompleted;
        this.icon = icon;
    }

    public int getId() { return id; }
    public String getPackageName() { return packageName; }
    public String getAppName() { return appName; }
    public String getTaskDescription() { return taskDescription; }
    public int getPosition() { return position; }
    public boolean isVisited() { return isVisited; }
    public boolean isCompleted() { return isCompleted; }
    public Drawable getIcon() { return icon; }
}