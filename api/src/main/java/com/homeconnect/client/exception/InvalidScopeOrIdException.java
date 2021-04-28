package com.homeconnect.client.exception;

import static java.lang.String.format;

import java.util.Date;

public class InvalidScopeOrIdException extends HomeConnectException {
	 private static final long serialVersionUID = 1L;

	    public InvalidScopeOrIdException(String message, Throwable cause) {
	        super(message, cause);
	    }

	    public InvalidScopeOrIdException(String message) {
	        super(message);
	    }

	    public InvalidScopeOrIdException(Throwable cause) {
	        super(cause);
	    }

	    public InvalidScopeOrIdException(int code, String message, String body) {
	        super(format("Communication error! response code: %d, message: %s, body: %s (Tried at %s)", code, message, body,
	                new Date()));
	    }
}
