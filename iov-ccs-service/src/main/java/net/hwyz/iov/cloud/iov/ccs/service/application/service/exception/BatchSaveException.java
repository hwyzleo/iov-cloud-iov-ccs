package net.hwyz.iov.cloud.iov.ccs.service.application.service.exception;

import java.util.List;

/**
 * 批量保存异常
 * <p>
 * 当批量保存部分失败时抛出，包含失败的ICCID列表
 *
 * @author hwyz_leo
 */
public class BatchSaveException extends ServiceException {

    private final List<String> failedIccids;

    public BatchSaveException(String message, List<String> failedIccids) {
        super(message);
        this.failedIccids = failedIccids;
    }

    public List<String> getFailedIccids() {
        return failedIccids;
    }
}
