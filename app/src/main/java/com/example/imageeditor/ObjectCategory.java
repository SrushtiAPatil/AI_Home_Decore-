package com.example.imageeditor;

import java.util.List;

public class ObjectCategory {
    private String name;
    private int iconResId;
    private List<Integer> objects;

    public ObjectCategory(String name, int iconResId, List<Integer> objects) {
        this.name = name;
        this.iconResId = iconResId;
        this.objects = objects;
    }

    public String getName() {
        return name;
    }

    public int getIconResId() {
        return iconResId;
    }

    public List<Integer> getObjects() {
        return objects;
    }
}