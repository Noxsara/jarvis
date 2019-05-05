package com.avengers.jarvis.exception;

public class MonitorException extends RuntimeException{

    private int code;

    public MonitorException(String msg, Integer code) {
        super(msg);
        this.code = code;
    }

    public MonitorException(String msg) {
        super(msg);
    }

    public int getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
