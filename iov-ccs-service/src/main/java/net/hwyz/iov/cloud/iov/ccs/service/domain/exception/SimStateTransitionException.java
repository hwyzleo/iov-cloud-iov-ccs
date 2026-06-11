package net.hwyz.iov.cloud.iov.ccs.service.domain.exception;

/**
 * SIM卡状态流转异常
 *
 * @author hwyz_leo
 */
public class SimStateTransitionException extends RuntimeException {

    public SimStateTransitionException(String message) {
        super(message);
    }

    public SimStateTransitionException(Integer fromState, Integer toState) {
        super("SIM卡状态流转不合法，从状态 " + fromState + " 到状态 " + toState);
    }
}
