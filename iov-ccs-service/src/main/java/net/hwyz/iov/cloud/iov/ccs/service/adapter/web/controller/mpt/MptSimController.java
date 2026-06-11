package net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.mpt;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.assembler.SimMptAssembler;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.SimCreateMpt;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.SimMpt;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.SimUpdateMpt;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.SimCreateCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.SimUpdateCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.query.SimQuery;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.result.SimResult;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.SimAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SIM卡管理后台控制器
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/sim/v1")
public class MptSimController {

    private final SimAppService simAppService;
    private final SimMptAssembler simMptAssembler;

    /**
     * 分页查询SIM卡列表
     */
    @RequiresPermissions("iov:mno:sim:list")
    @GetMapping(value = "/list")
    public IPage<SimMpt> list(SimMpt simMpt) {
        SimQuery query = simMptAssembler.toQuery(simMpt);
        IPage<SimResult> resultPage = simAppService.pageQuery(query);
        return resultPage.convert(simMptAssembler::toMpt);
    }

    /**
     * 获取SIM卡详情
     */
    @RequiresPermissions("iov:mno:sim:query")
    @GetMapping(value = "/{simId}")
    public SimMpt getById(@PathVariable Long simId) {
        SimResult result = simAppService.getById(simId);
        return simMptAssembler.toMpt(result);
    }

    /**
     * 新增SIM卡
     */
    @RequiresPermissions("iov:mno:sim:add")
    @PostMapping
    public Long create(@RequestBody @Validated SimCreateMpt simCreateMpt) {
        SimCreateCmd cmd = simMptAssembler.toCreateCmd(simCreateMpt);
        return simAppService.create(cmd);
    }

    /**
     * 修改SIM卡
     */
    @RequiresPermissions("iov:mno:sim:edit")
    @PutMapping
    public SimMpt update(@RequestBody @Validated SimUpdateMpt simUpdateMpt) {
        SimUpdateCmd cmd = simMptAssembler.toUpdateCmd(simUpdateMpt);
        SimResult result = simAppService.update(cmd);
        return simMptAssembler.toMpt(result);
    }

    /**
     * 删除SIM卡
     */
    @RequiresPermissions("iov:mno:sim:remove")
    @DeleteMapping(value = "/{simIds}")
    public void delete(@PathVariable Long[] simIds) {
        List<Long> idList = Arrays.stream(simIds).collect(Collectors.toList());
        simAppService.deleteByIds(idList);
    }

    /**
     * 状态变更：测试 -> 库存
     */
    @RequiresPermissions("iov:mno:sim:edit")
    @PutMapping(value = "/transitionToStock/{simId}")
    public void transitionToStock(@PathVariable Long simId) {
        simAppService.transitionToStock(simId, "admin");
    }

    /**
     * 状态变更：库存 -> 激活
     */
    @RequiresPermissions("iov:mno:sim:edit")
    @PutMapping(value = "/transitionToActive/{simId}")
    public void transitionToActive(@PathVariable Long simId) {
        simAppService.transitionToActive(simId, "admin");
    }

    /**
     * 更新SIM卡能力
     */
    @RequiresPermissions("iov:mno:sim:edit")
    @PutMapping(value = "/updateAbilities/{simId}")
    public void updateAbilities(
            @PathVariable Long simId,
            @RequestParam(required = false) Integer smsAbility,
            @RequestParam(required = false) Integer dataAbility,
            @RequestParam(required = false) Integer voiceAbility) {
        simAppService.updateAbilities(simId, smsAbility, dataAbility, voiceAbility, "admin");
    }
}
