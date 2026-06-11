package net.hwyz.iov.cloud.iov.ccs.api.service;

import net.hwyz.iov.cloud.iov.ccs.api.vo.request.BatchImportSimRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * SIM卡服务Feign客户端
 *
 * @author hwyz_leo
 */
@FeignClient(name = "iov-ccs", path = "/api/service/sim/v1")
public interface SimService {

    /**
     * 批量导入SIM卡
     *
     * @param request 批量导入请求
     */
    @PostMapping(value = "/batchImport")
    void batchImport(@RequestBody BatchImportSimRequest request);
}
