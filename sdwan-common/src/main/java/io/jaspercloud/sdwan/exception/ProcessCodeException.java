package io.jaspercloud.sdwan.exception;

public class ProcessCodeException extends ProcessException {

    private int code;

    public int getCode() {
        return code;
    }

    public ProcessCodeException(int code) {
        this.code = code;
    }

    public ProcessCodeException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ProcessCodeException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
