package net.hwyz.iov.cloud.iov.ccs.service.application.service.exception;

/**
 * 业务异常基类
 *
 * @author hwyz_leo
 */
public class ServiceException extends RuntimeException {

    private final int code;

    public ServiceException(String message) {
        super(message);
        this.code = 400;
    }

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
        this.code = 400;
    }

    public int getCode() {
        return code;
    }
}
