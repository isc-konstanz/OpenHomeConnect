package com.homeconnect.client.exception;

public class HomeConnectException extends Exception {
    
    private static final long serialVersionUID = 1L;

    public HomeConnectException(String message, Throwable cause) {
        super(message, cause);
    }

    public HomeConnectException(String message) {
        super(message);
    }

    public HomeConnectException(Throwable cause) {
        super(cause);
    }

}
