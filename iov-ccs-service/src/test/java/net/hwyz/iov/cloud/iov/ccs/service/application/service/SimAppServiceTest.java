package net.hwyz.iov.cloud.iov.ccs.service.application.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import net.hwyz.iov.cloud.iov.ccs.service.application.assembler.SimAssembler;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.BatchImportSimCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.SimCreateCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.cmd.SimUpdateCmd;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.query.SimQuery;
import net.hwyz.iov.cloud.iov.ccs.service.application.dto.result.SimResult;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.Sim;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.SimDomainService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SIM卡应用服务单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class SimAppServiceTest {

    @Mock
    private SimDomainService simDomainService;

    @Mock
    private SimAssembler simAssembler;

    @InjectMocks
    private SimAppService simAppService;

    private Sim testSim;
    private SimResult testSimResult;

    @BeforeEach
    void setUp() {
        testSim = Sim.builder()
                .id(1L)
                .iccid("8986012345678901234")
                .imsi("460012345678901")
                .msisdn("13800138000")
                .mnoCode("CMCC")
                .simState(1)
                .smsAbility(1)
                .dataAbility(1)
                .voiceAbility(1)
                .description("测试SIM卡")
                .createTime(LocalDateTime.now())
                .createBy("admin")
                .modifyTime(LocalDateTime.now())
                .modifyBy("admin")
                .rowVersion(1)
                .rowValid(1)
                .build();

        testSimResult = SimResult.builder()
                .id(1L)
                .iccid("8986012345678901234")
                .imsi("460012345678901")
                .msisdn("13800138000")
                .mnoCode("CMCC")
                .simState(1)
                .smsAbility(1)
                .dataAbility(1)
                .voiceAbility(1)
                .description("测试SIM卡")
                .createTime(LocalDateTime.now())
                .createBy("admin")
                .modifyTime(LocalDateTime.now())
                .modifyBy("admin")
                .build();
    }

    @Test
    void testPageQuery_Success() {
        SimQuery query = SimQuery.builder()
                .pageNum(1)
                .pageSize(10)
                .build();

        Page<Sim> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(testSim));
        page.setTotal(1);

        when(simDomainService.pageQuery(null, null, null, 1, 10)).thenReturn(page);
        when(simAssembler.toResult(testSim)).thenReturn(testSimResult);

        IPage<SimResult> result = simAppService.pageQuery(query);

        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        verify(simDomainService).pageQuery(null, null, null, 1, 10);
    }

    @Test
    void testGetById_Success() {
        when(simDomainService.getById(1L)).thenReturn(testSim);
        when(simAssembler.toResult(testSim)).thenReturn(testSimResult);

        SimResult result = simAppService.getById(1L);

        assertNotNull(result);
        assertEquals("8986012345678901234", result.getIccid());
        verify(simDomainService).getById(1L);
    }

    @Test
    void testCreate_Success() {
        SimCreateCmd cmd = SimCreateCmd.builder()
                .iccid("8986012345678901234")
                .imsi("460012345678901")
                .msisdn("13800138000")
                .mnoCode("CMCC")
                .description("测试SIM卡")
                .createBy("admin")
                .build();

        when(simAssembler.toEntity(cmd)).thenReturn(testSim);
        when(simDomainService.create(testSim)).thenReturn(1L);

        Long id = simAppService.create(cmd);

        assertEquals(1L, id);
        verify(simAssembler).toEntity(cmd);
        verify(simDomainService).create(testSim);
    }

    @Test
    void testUpdate_Success() {
        SimUpdateCmd cmd = SimUpdateCmd.builder()
                .id(1L)
                .iccid("8986012345678901234")
                .imsi("460012345678901")
                .msisdn("13800138000")
                .mnoCode("CMCC")
                .description("更新后的描述")
                .modifyBy("admin")
                .build();

        when(simAssembler.toEntity(cmd)).thenReturn(testSim);
        when(simDomainService.update(testSim)).thenReturn(testSim);
        when(simAssembler.toResult(testSim)).thenReturn(testSimResult);

        SimResult result = simAppService.update(cmd);

        assertNotNull(result);
        verify(simAssembler).toEntity(cmd);
        verify(simDomainService).update(testSim);
    }

    @Test
    void testDeleteById_Success() {
        doNothing().when(simDomainService).deleteById(1L);

        simAppService.deleteById(1L);

        verify(simDomainService).deleteById(1L);
    }

    @Test
    void testDeleteByIds_Success() {
        List<Long> ids = Arrays.asList(1L, 2L);
        doNothing().when(simDomainService).deleteByIds(ids);

        simAppService.deleteByIds(ids);

        verify(simDomainService).deleteByIds(ids);
    }

    @Test
    void testTransitionToStock_Success() {
        doNothing().when(simDomainService).transitionToStock(1L, "admin");

        simAppService.transitionToStock(1L, "admin");

        verify(simDomainService).transitionToStock(1L, "admin");
    }

    @Test
    void testTransitionToActive_Success() {
        doNothing().when(simDomainService).transitionToActive(1L, "admin");

        simAppService.transitionToActive(1L, "admin");

        verify(simDomainService).transitionToActive(1L, "admin");
    }

    @Test
    void testUpdateAbilities_Success() {
        doNothing().when(simDomainService).updateAbilities(1L, 0, 1, 0, "admin");

        simAppService.updateAbilities(1L, 0, 1, 0, "admin");

        verify(simDomainService).updateAbilities(1L, 0, 1, 0, "admin");
    }

    @Test
    void testBatchImport_Success() {
        BatchImportSimCmd cmd = BatchImportSimCmd.builder()
                .mnoType("CMCC")
                .batchNum("BATCH001")
                .simList(Arrays.asList(
                        BatchImportSimCmd.SimImportItem.builder()
                                .iccid("8986012345678901234")
                                .imsi("460012345678901")
                                .msisdn("13800138000")
                                .build()
                ))
                .build();

        List<Sim> simList = Arrays.asList(testSim);
        when(simAssembler.toEntityList(cmd.getSimList())).thenReturn(simList);
        when(simDomainService.batchImport("CMCC", simList, "BATCH001")).thenReturn(1);

        int count = simAppService.batchImport(cmd);

        assertEquals(1, count);
        verify(simAssembler).toEntityList(cmd.getSimList());
        verify(simDomainService).batchImport("CMCC", simList, "BATCH001");
    }
}
