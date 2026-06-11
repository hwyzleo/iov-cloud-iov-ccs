package net.hwyz.iov.cloud.iov.ccs.service.domain.exception;

/**
 * SIM卡未找到异常
 *
 * @author hwyz_leo
 */
public class SimNotFoundException extends RuntimeException {

    public SimNotFoundException(String message) {
        super(message);
    }

    public SimNotFoundException(Long id) {
        super("SIM卡不存在，ID: " + id);
    }

    public SimNotFoundException(String iccid, boolean byIccid) {
        super("SIM卡不存在，ICCID: " + iccid);
    }
}
