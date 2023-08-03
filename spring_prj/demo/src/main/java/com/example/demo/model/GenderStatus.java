package com.example.demo.model;

public enum GenderStatus {
    MALE("男性"),
    FEMALE("女性"),
    OTHER("その他");

    private String viewName;
    private GenderStatus(String viewName) {
        this.viewName = viewName;
    }
    public String getViewName() {
        return viewName;
    }
}
