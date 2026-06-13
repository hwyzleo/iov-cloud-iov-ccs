package net.hwyz.iov.cloud.iov.ccs.service.domain.service;

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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * SIM卡领域服务单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class SimDomainServiceTest {

    @Mock
    private SimRepository simRepository;

    @Mock
    private SimLogRepository simLogRepository;

    @InjectMocks
    private SimDomainService simDomainService;

    private Sim testSim;

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
    }

    @Test
    void testGetById_Success() {
        when(simRepository.getById(1L)).thenReturn(testSim);

        Sim result = simDomainService.getById(1L);

        assertNotNull(result);
        assertEquals("8986012345678901234", result.getIccid());
        verify(simRepository).getById(1L);
    }

    @Test
    void testGetById_NotFound() {
        when(simRepository.getById(1L)).thenReturn(null);

        assertThrows(SimNotFoundException.class, () -> simDomainService.getById(1L));
        verify(simRepository).getById(1L);
    }

    @Test
    void testGetByIccid_Success() {
        when(simRepository.getByIccid("8986012345678901234")).thenReturn(testSim);

        Sim result = simDomainService.getByIccid("8986012345678901234");

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(simRepository).getByIccid("8986012345678901234");
    }

    @Test
    void testGetByIccid_NotFound() {
        when(simRepository.getByIccid("8986012345678901234")).thenReturn(null);

        assertThrows(SimNotFoundException.class, () -> simDomainService.getByIccid("8986012345678901234"));
        verify(simRepository).getByIccid("8986012345678901234");
    }

    @Test
    void testCreate_Success() {
        Sim newSim = Sim.create("8986012345678901235", "460012345678902", "13800138001", "CMCC", "admin");
        when(simRepository.countByIccid("8986012345678901235")).thenReturn(0);
        when(simRepository.save(any(Sim.class))).thenAnswer(invocation -> {
            Sim sim = invocation.getArgument(0);
            sim.setId(2L);
            return 1;
        });
        when(simLogRepository.save(any(SimLog.class))).thenReturn(1);

        Long id = simDomainService.create(newSim);

        assertNotNull(id);
        assertEquals(2L, id);
        verify(simRepository).countByIccid("8986012345678901235");
        verify(simRepository).save(any(Sim.class));
        verify(simLogRepository).save(any(SimLog.class));
    }

    @Test
    void testCreate_DuplicateIccid() {
        Sim newSim = Sim.create("8986012345678901234", "460012345678901", "13800138000", "CMCC", "admin");
        when(simRepository.countByIccid("8986012345678901234")).thenReturn(1);

        assertThrows(SimDuplicateException.class, () -> simDomainService.create(newSim));
        verify(simRepository).countByIccid("8986012345678901234");
        verify(simRepository, never()).save(any(Sim.class));
    }

    @Test
    void testUpdate_Success() {
        when(simRepository.getById(1L)).thenReturn(testSim);
        when(simRepository.save(any(Sim.class))).thenReturn(1);
        when(simLogRepository.save(any(SimLog.class))).thenReturn(1);

        testSim.setDescription("更新后的描述");
        Sim result = simDomainService.update(testSim);

        assertNotNull(result);
        verify(simRepository).getById(1L);
        verify(simRepository).save(any(Sim.class));
        verify(simLogRepository).save(any(SimLog.class));
    }

    @Test
    void testUpdate_SimNotFound() {
        when(simRepository.getById(1L)).thenReturn(null);

        assertThrows(SimNotFoundException.class, () -> simDomainService.update(testSim));
        verify(simRepository).getById(1L);
        verify(simRepository, never()).save(any(Sim.class));
    }

    @Test
    void testUpdate_DuplicateIccid() {
        Sim existingSim = Sim.builder().id(1L).iccid("8986012345678901234").build();
        when(simRepository.getById(1L)).thenReturn(existingSim);
        when(simRepository.countByIccid("8986012345678901235")).thenReturn(1);

        testSim.setIccid("8986012345678901235");
        assertThrows(SimDuplicateException.class, () -> simDomainService.update(testSim));
        verify(simRepository).getById(1L);
        verify(simRepository).countByIccid("8986012345678901235");
        verify(simRepository, never()).save(any(Sim.class));
    }

    @Test
    void testDeleteById_Success() {
        when(simRepository.getById(1L)).thenReturn(testSim);
        when(simLogRepository.deleteByIccid("8986012345678901234")).thenReturn(1);
        when(simRepository.deleteByIccid("8986012345678901234")).thenReturn(1);

        simDomainService.deleteById(1L);

        verify(simRepository).getById(1L);
        verify(simLogRepository).deleteByIccid("8986012345678901234");
        verify(simRepository).deleteByIccid("8986012345678901234");
    }

    @Test
    void testDeleteByIds_Success() {
        when(simRepository.getById(1L)).thenReturn(testSim);
        when(simRepository.getById(2L)).thenReturn(testSim);
        when(simLogRepository.deleteByIccid(anyString())).thenReturn(1);
        when(simRepository.deleteByIccid(anyString())).thenReturn(1);

        simDomainService.deleteByIds(Arrays.asList(1L, 2L));

        verify(simRepository, times(2)).getById(anyLong());
        verify(simLogRepository, times(2)).deleteByIccid(anyString());
        verify(simRepository, times(2)).deleteByIccid(anyString());
    }

    @Test
    void testTransitionToStock_Success() {
        testSim.setSimState(1);
        when(simRepository.getById(1L)).thenReturn(testSim);
        when(simRepository.save(any(Sim.class))).thenReturn(1);
        when(simLogRepository.save(any(SimLog.class))).thenReturn(1);

        simDomainService.transitionToStock(1L, "admin");

        verify(simRepository).getById(1L);
        verify(simRepository).save(any(Sim.class));
        verify(simLogRepository).save(any(SimLog.class));
    }

    @Test
    void testTransitionToStock_InvalidState() {
        testSim.setSimState(2);
        when(simRepository.getById(1L)).thenReturn(testSim);

        assertThrows(IllegalStateException.class, () -> simDomainService.transitionToStock(1L, "admin"));
        verify(simRepository).getById(1L);
        verify(simRepository, never()).save(any(Sim.class));
    }

    @Test
    void testTransitionToActive_Success() {
        testSim.setSimState(2);
        when(simRepository.getById(1L)).thenReturn(testSim);
        when(simRepository.save(any(Sim.class))).thenReturn(1);
        when(simLogRepository.save(any(SimLog.class))).thenReturn(1);

        simDomainService.transitionToActive(1L, "admin");

        verify(simRepository).getById(1L);
        verify(simRepository).save(any(Sim.class));
        verify(simLogRepository).save(any(SimLog.class));
    }

    @Test
    void testTransitionToActive_InvalidState() {
        testSim.setSimState(1);
        when(simRepository.getById(1L)).thenReturn(testSim);

        assertThrows(IllegalStateException.class, () -> simDomainService.transitionToActive(1L, "admin"));
        verify(simRepository).getById(1L);
        verify(simRepository, never()).save(any(Sim.class));
    }

    @Test
    void testUpdateAbilities_Success() {
        when(simRepository.getById(1L)).thenReturn(testSim);
        when(simRepository.save(any(Sim.class))).thenReturn(1);
        when(simLogRepository.save(any(SimLog.class))).thenReturn(1);

        simDomainService.updateAbilities(1L, 0, 1, 0, "admin");

        verify(simRepository).getById(1L);
        verify(simRepository).save(any(Sim.class));
        verify(simLogRepository).save(any(SimLog.class));
    }

    @Test
    void testBatchImport_Success() {
        List<Sim> simList = Arrays.asList(
                Sim.create("8986012345678901235", "460012345678902", "13800138001", "CMCC", "admin"),
                Sim.create("8986012345678901236", "460012345678903", "13800138002", "CMCC", "admin")
        );
        when(simRepository.countByIccid(anyString())).thenReturn(0);
        when(simRepository.save(any(Sim.class))).thenReturn(1);
        when(simLogRepository.save(any(SimLog.class))).thenReturn(1);

        int count = simDomainService.batchImport("CMCC", simList, "BATCH001");

        assertEquals(2, count);
        verify(simRepository, times(2)).countByIccid(anyString());
        verify(simRepository, times(2)).save(any(Sim.class));
        verify(simLogRepository, times(2)).save(any(SimLog.class));
    }

    @Test
    void testBatchImport_SkipDuplicate() {
        List<Sim> simList = Arrays.asList(
                Sim.create("8986012345678901234", "460012345678901", "13800138000", "CMCC", "admin"),
                Sim.create("8986012345678901235", "460012345678902", "13800138001", "CMCC", "admin")
        );
        when(simRepository.countByIccid("8986012345678901234")).thenReturn(1);
        when(simRepository.countByIccid("8986012345678901235")).thenReturn(0);
        when(simRepository.save(any(Sim.class))).thenReturn(1);
        when(simLogRepository.save(any(SimLog.class))).thenReturn(1);

        int count = simDomainService.batchImport("CMCC", simList, "BATCH001");

        assertEquals(1, count);
        verify(simRepository, times(2)).countByIccid(anyString());
        verify(simRepository, times(1)).save(any(Sim.class));
        verify(simLogRepository, times(1)).save(any(SimLog.class));
    }
}
