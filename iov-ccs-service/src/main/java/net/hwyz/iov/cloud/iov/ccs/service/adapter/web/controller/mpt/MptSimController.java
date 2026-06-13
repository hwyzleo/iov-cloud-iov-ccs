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
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.SimInfoQueryRequest;
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
 * SIM卡信息管理接口（后台管理）
 *
 * <pre>
 * 接口清单：
 *   POST   /                    保存单条SIM信息
 *   POST   /batch               批量保存SIM信息
 *   POST   /sync                Hex编码数据同步（Upsert）
 *   GET    /list                分页条件查询
 *   GET    /{iccid}             查询详情
 *   PUT    /{iccid}             更新（仅MANUAL来源，路径与body iccid须一致）
 *   DELETE /{iccid}             删除单条
 *   DELETE /batch/{iccids}      批量删除
 * </pre>
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
     * 分页条件查询SIM信息列表
     *
     * @param query 筛选条件（iccid/imsi/msisdn/sourceMno，均可选）
     * @return 分页结果，包含列表和分页元数据
     */
    @RequiresPermissions("ccs:simInfo:list")
    @GetMapping("/list")
    public ApiResponse<PageInfo<SimInfo>> list(@Validated SimInfoQueryRequest query) {
        startPage();
        Map<String, Object> params = new HashMap<>();
        if (query.getIccid() != null && !query.getIccid().isEmpty()) {
            params.put("iccid", query.getIccid());
        }
        if (query.getImsi() != null && !query.getImsi().isEmpty()) {
            params.put("imsi", query.getImsi());
        }
        if (query.getMsisdn() != null && !query.getMsisdn().isEmpty()) {
            params.put("msisdn", query.getMsisdn());
        }
        if (query.getSourceMno() != null && !query.getSourceMno().isEmpty()) {
            params.put("sourceMno", query.getSourceMno());
        }

        List<SimInfo> list = manualSimService.listSimInfo(params);
        return ApiResponse.ok(new PageInfo<>(list));
    }

    /**
     * 查询单条SIM信息详情
     *
     * @param iccid ICCID（集成电路卡识别码，19-20位数字）
     * @return SIM信息，不存在时返回业务失败
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
     * <p>
     * ICCID已存在时拒绝保存。入库前执行格式校验和规范化。
     * 默认状态：TEST / UNBOUNDED / NO_REAL_NAME。
     *
     * @param request SIM信息（iccid 19-20位数字、imsi 15位数字、msisdn 手机号、mnoType 运营商类型）
     * @return 操作结果
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
     * <p>
     * 复用单条逻辑，部分失败时收集失败ICCID后整体返回失败。
     *
     * @param request SIM信息列表
     * @return 操作结果
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
     * Hex编码数据同步
     * <p>
     * 接收Hex编码的JSON数组，解码后逐条Upsert。
     * 强制状态字段：TEST / UNBOUNDED / NO_REAL_NAME。
     * 允许覆盖 IMSI/MSISDN/source_*。
     *
     * @param request Hex编码的JSON数据
     * @return 操作结果
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
     * <p>
     * 路径iccid与请求体iccid必须一致，否则拒绝。
     * 运营商来源（CMCC/CUCC）记录不可更新。
     *
     * @param iccid   路径中的ICCID（决定更新目标）
     * @param request 更新内容（iccid必须与路径一致）
     * @return 操作结果
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("ccs:simInfo:edit")
    @PutMapping("/{iccid}")
    public ApiResponse<Void> update(@PathVariable String iccid, @Validated @RequestBody SimInfoRequest request) {
        // 路径与body的iccid一致性校验
        if (!iccid.equals(request.getIccid())) {
            log.warn("更新SIM信息失败: 路径iccid[{}]与请求体iccid[{}]不一致", iccid, request.getIccid());
            return ApiResponse.fail("路径ICCID与请求体ICCID不一致");
        }

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
     * 删除单条SIM信息（硬删除+审计）
     *
     * @param iccid ICCID
     * @return 操作结果
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
     * 批量删除SIM信息（硬删除+审计）
     * <p>
     * 部分失败时收集失败ICCID后整体返回。
     *
     * @param iccids ICCID列表（逗号分隔）
     * @return 操作结果
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("ccs:simInfo:remove")
    @DeleteMapping("/batch/{iccids}")
    public ApiResponse<Void> batchDelete(@PathVariable List<String> iccids) {
        log.info("管理后台用户[{}]批量删除SIM信息: count={}", SecurityContextHolder.getUserName(), iccids.size());

        try {
            manualSimService.batchDeleteSimInfo(iccids);
            return ApiResponse.ok();
        } catch (BatchSaveException e) {
            log.warn("批量删除部分失败: {}", e.getMessage());
            return ApiResponse.fail("批量删除部分失败，失败ICCID: " + String.join(", ", e.getFailedIccids()));
        } catch (ServiceException e) {
            log.warn("批量删除失败: {}", e.getMessage());
            return ApiResponse.fail(e.getMessage());
        }
    }

    /**
     * 请求体转SimInfo实体，附带格式校验
     *
     * @param request 请求体（已通过@Validated基础校验）
     * @return SimInfo实体
     * @throws ServiceException 格式不合法时抛出
     */
    private SimInfo convertToSimInfo(SimInfoRequest request) {
        if (request.getIccid() == null || !request.getIccid().matches("^\\d{19,20}$")) {
            throw new ServiceException("ICCID格式不正确，必须为19-20位数字");
        }
        if (request.getImsi() == null || !request.getImsi().matches("^\\d{15}$")) {
            throw new ServiceException("IMSI格式不正确，必须为15位数字");
        }
        if (request.getMsisdn() == null || !request.getMsisdn().matches("^(86)?1[3-9]\\d{9}$")) {
            throw new ServiceException("MSISDN格式不正确");
        }
        return SimInfo.builder()
                .iccid(request.getIccid())
                .imsi(request.getImsi())
                .msisdn(request.getMsisdn())
                .sourceMno(request.getMnoType())
                .build();
    }
}
