package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量SIM信息请求
 *
 * @author hwyz_leo
 */
@Data
public class BatchSimInfoRequest {

    /**
     * SIM信息列表
     */
    @NotEmpty(message = "SIM信息列表不能为空")
    @Valid
    private List<SimInfoRequest> simList;
}
