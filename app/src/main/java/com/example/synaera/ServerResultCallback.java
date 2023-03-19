package com.example.synaera;

public interface ServerResultCallback {
    public void onConnected(boolean success);
    public void displayResponse(String result);
    public void logResponse(String result);
}
