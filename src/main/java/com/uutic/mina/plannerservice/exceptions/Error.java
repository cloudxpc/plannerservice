package com.uutic.mina.plannerservice.exceptions;

import java.io.Serializable;

/**
 * View Model for transferring error message with a list of field errors.
 */
public class Error implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String errorType;
    private final String errorMessage;

    public Error(Exception ex) {
        this.errorType = ex.getClass().getSimpleName();
        this.errorMessage = ex.getMessage();
    }

    public String getErrorType() {
        return errorType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
