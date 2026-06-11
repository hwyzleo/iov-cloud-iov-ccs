package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.BatchImportSimCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.SimAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SIM卡服务接口控制器
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/sim/v1")
public class ServiceSimController {

    private final SimAppService simAppService;

    /**
     * 批量导入SIM卡
     */
    @PostMapping(value = "/batchImport")
    public void batchImport(@RequestBody @Validated BatchImportSimCmd cmd) {
        simAppService.batchImport(cmd);
    }
}
