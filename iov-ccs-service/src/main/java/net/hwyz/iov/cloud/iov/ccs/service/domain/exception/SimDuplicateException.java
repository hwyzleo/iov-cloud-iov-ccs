package net.hwyz.iov.cloud.iov.ccs.service.domain.exception;

/**
 * SIM卡重复异常
 *
 * @author hwyz_leo
 */
public class SimDuplicateException extends RuntimeException {

    public SimDuplicateException(String message) {
        super(message);
    }

    public SimDuplicateException(String iccid, boolean byIccid) {
        super("SIM卡ICCID已存在: " + iccid);
    }
}
