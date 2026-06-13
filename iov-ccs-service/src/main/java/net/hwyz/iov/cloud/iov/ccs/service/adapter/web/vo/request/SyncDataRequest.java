package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 同步数据请求
 *
 * @author hwyz_leo
 */
@Data
public class SyncDataRequest {

    /**
     * Hex编码的JSON数据
     */
    @NotBlank(message = "Hex数据不能为空")
    private String hexData;
}
