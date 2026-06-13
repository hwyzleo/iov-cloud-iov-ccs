package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.mpt;

import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.BatchSimInfoRequest;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.SimInfoRequest;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.SyncDataRequest;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.ManualSimService;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.BatchSaveException;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.exception.ServiceException;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SIM卡信息管理接口
 * <p>
 * 后台管理接口，用于手动录入和管理SIM卡信息
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/simInfo/v1")
public class MptSimController extends BaseController {

    private final ManualSimService manualSimService;

    /**
     * 查询SIM信息列表（分页+筛选）
     */
    @RequiresPermissions("ccs:simInfo:list")
    @GetMapping("/list")
    public ApiResponse<PageInfo<SimInfo>> list(
            @RequestParam(required = false) String iccid,
            @RequestParam(required = false) String imsi,
            @RequestParam(required = false) String msisdn,
            @RequestParam(required = false) String sourceMno) {

        startPage();
        Map<String, Object> params = new HashMap<>();
        if (iccid != null && !iccid.isEmpty()) params.put("iccid", iccid);
        if (imsi != null && !imsi.isEmpty()) params.put("imsi", imsi);
        if (msisdn != null && !msisdn.isEmpty()) params.put("msisdn", msisdn);
        if (sourceMno != null && !sourceMno.isEmpty()) params.put("sourceMno", sourceMno);

        List<SimInfo> list = manualSimService.listSimInfo(params);
        return ApiResponse.ok(new PageInfo<>(list));
    }

    /**
     * 查询SIM信息详情
     */
    @RequiresPermissions("ccs:simInfo:query")
    @GetMapping("/{iccid}")
    public ApiResponse<SimInfo> getInfo(@PathVariable String iccid) {
        try {
            SimInfo simInfo = manualSimService.getSimInfo(iccid);
            return ApiResponse.ok(simInfo);
        } catch (ServiceException e) {
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 保存单条SIM信息
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ccs:simInfo:add")
    @PostMapping
    public ApiResponse<Void> add(@Validated @RequestBody SimInfoRequest request) {
        log.info("管理后台用户[{}]手动保存SIM信息: iccid={}", SecurityContextHolder.getUserName(), request.getIccid());

        try {
            SimInfo simInfo = convertToSimInfo(request);
            manualSimService.saveSimInfo(simInfo);
            return ApiResponse.ok();
        } catch (ServiceException e) {
            log.warn("SIM信息保存失败: {}", e.getMessage());
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 批量保存SIM信息
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("ccs:simInfo:batchAdd")
    @PostMapping("/batch")
    public ApiResponse<Void> batchAdd(@Validated @RequestBody BatchSimInfoRequest request) {
        log.info("管理后台用户[{}]批量保存SIM信息: count={}", SecurityContextHolder.getUserName(),
                request.getSimList() != null ? request.getSimList().size() : 0);

        try {
            List<SimInfo> simInfoList = request.getSimList().stream()
                    .map(this::convertToSimInfo)
                    .toList();

            manualSimService.batchSaveSimInfo(simInfoList);
            return ApiResponse.ok();
        } catch (BatchSaveException e) {
            log.warn("批量保存部分失败: {}", e.getMessage());
            return ApiResponse.fail("批量保存部分失败，失败ICCID: " + String.join(", ", e.getFailedIccids()));
        } catch (ServiceException e) {
            log.warn("批量保存失败: {}", e.getMessage());
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 同步数据
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.IMPORT)
    @RequiresPermissions("ccs:simInfo:sync")
    @PostMapping("/sync")
    public ApiResponse<Void> syncData(@Validated @RequestBody SyncDataRequest request) {
        log.info("管理后台用户[{}]同步SIM数据: hexLength={}", SecurityContextHolder.getUserName(),
                request.getHexData() != null ? request.getHexData().length() : 0);

        try {
            manualSimService.syncData(request.getHexData());
            return ApiResponse.ok();
        } catch (ServiceException e) {
            log.warn("同步数据失败: {}", e.getMessage());
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 更新SIM信息（仅MANUAL来源可更新）
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ccs:simInfo:edit")
    @PutMapping("/{iccid}")
    public ApiResponse<Void> update(@PathVariable String iccid, @RequestBody SimInfoRequest request) {
        log.info("管理后台用户[{}]更新SIM信息: iccid={}", SecurityContextHolder.getUserName(), iccid);

        try {
            SimInfo simInfo = convertToSimInfo(request);
            manualSimService.updateSimInfo(iccid, simInfo);
            return ApiResponse.ok();
        } catch (ServiceException e) {
            log.warn("SIM信息更新失败: {}", e.getMessage());
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 删除SIM信息（硬删除+审计）
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ccs:simInfo:remove")
    @DeleteMapping("/{iccid}")
    public ApiResponse<Void> delete(@PathVariable String iccid) {
        log.info("管理后台用户[{}]删除SIM信息: iccid={}", SecurityContextHolder.getUserName(), iccid);

        try {
            manualSimService.deleteSimInfo(iccid);
            return ApiResponse.ok();
        } catch (ServiceException e) {
            log.warn("SIM信息删除失败: {}", e.getMessage());
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 转换为SimInfo
     */
    private SimInfo convertToSimInfo(SimInfoRequest request) {
        return SimInfo.builder()
                .iccid(request.getIccid())
                .imsi(request.getImsi())
                .msisdn(request.getMsisdn())
                .sourceMno(request.getMnoType())
                .build();
    }
}
