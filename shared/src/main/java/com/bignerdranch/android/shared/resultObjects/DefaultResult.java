package com.bignerdranch.android.shared.resultObjects;

public class DefaultResult {
    private String type;
    private boolean success;
    private Object data;

    public DefaultResult(String type, boolean success, Object data) {
        this.type = type;
        this.success = success;
        this.data = data;
    }

    public String getType() {
        return type;
    }

    public boolean isSuccess() {
        return success;
    }

    //Object should be the result object specific to the type
    public Object getData() {
        return data;
    }
}
