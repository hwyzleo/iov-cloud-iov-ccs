package net.hwyz.iov.cloud.iov.ccs.service.domain.service.impl;

import net.hwyz.iov.cloud.iov.ccs.service.domain.event.BusinessAlertEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.event.VmdBindingEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.VehicleSim;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.SimInfoRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.VehicleSimRepository;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.CardStatusEventPublisher;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.LockService;
import net.hwyz.iov.cloud.iov.ccs.service.domain.service.metrics.CcsMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * CardBindingServiceImpl 单元测试
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class CardBindingServiceImplTest {

    @Mock
    private SimInfoRepository simInfoRepository;

    @Mock
    private VehicleSimRepository vehicleSimRepository;

    @Mock
    private LockService lockService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private CardStatusEventPublisher cardStatusEventPublisher;

    @Mock
    private CcsMetricsService metricsService;

    @InjectMocks
    private CardBindingServiceImpl cardBindingService;

    private VmdBindingEvent testEvent;
    private SimInfo testSimInfo;
    private VehicleSim testVehicleSim;

    @BeforeEach
    void setUp() {
        testEvent = VmdBindingEvent.builder()
                .bindingId("test-binding-id")
                .changeType("BIND")
                .vin("test-vin")
                .tboxSn("test-tbox-sn")
                .iccid1("test-iccid1")
                .iccid2("test-iccid2")
                .seq(1L)
                .occurredAt(LocalDateTime.now())
                .originalEventId("test-event-id")
                .build();

        testSimInfo = SimInfo.builder()
                .id(1L)
                .iccid("test-iccid1")
                .imsi("test-imsi")
                .msisdn("test-msisdn")
                .sourceMno("CMCC")
                .sourceType("cmcc_file")
                .simStatus(1)
                .bindingStatus(0)
                .realnameStatus(1)
                .smsStatus(true)
                .dataStatus(true)
                .voiceStatus(true)
                .build();

        testVehicleSim = VehicleSim.builder()
                .id(1L)
                .vin("test-vin")
                .iccid("test-iccid1")
                .cardSlot(1)
                .bindingStatus(1)
                .packageType("TEST")
                .tboxSn("test-tbox-sn")
                .sourceEventId("test-binding-id")
                .sourceSeq(1L)
                .boundTime(LocalDateTime.now())
                .build();
    }

    @Test
    void testHandleBindingEvent_Success() {
        // Given
        when(lockService.executeWithLock(anyString(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        when(simInfoRepository.getByIccid("test-iccid1")).thenReturn(testSimInfo);
        when(simInfoRepository.getByIccid("test-iccid2")).thenReturn(testSimInfo);
        when(vehicleSimRepository.getByIccid("test-iccid1")).thenReturn(null);
        when(vehicleSimRepository.getByIccid("test-iccid2")).thenReturn(null);

        // When
        cardBindingService.handleBindingEvent(testEvent);

        // Then
        verify(simInfoRepository, times(2)).update(any(SimInfo.class));
        verify(vehicleSimRepository, times(2)).upsert(any(VehicleSim.class));
        verify(metricsService, times(2)).recordVmdBindingSuccess();
    }

    @Test
    void testHandleBindingEvent_IccidNotFound() {
        // Given
        when(lockService.executeWithLock(anyString(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        when(simInfoRepository.getByIccid("test-iccid1")).thenReturn(null);
        when(simInfoRepository.getByIccid("test-iccid2")).thenReturn(null);

        // When
        cardBindingService.handleBindingEvent(testEvent);

        // Then
        verify(metricsService, times(2)).recordVmdBindingIccidNotFound();
        verify(eventPublisher, times(2)).publishEvent(any(BusinessAlertEvent.class));
        verify(simInfoRepository, never()).update(any(SimInfo.class));
        verify(vehicleSimRepository, never()).upsert(any(VehicleSim.class));
    }

    @Test
    void testHandleBindingEvent_AlreadyBound() {
        // Given
        when(lockService.executeWithLock(anyString(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        when(simInfoRepository.getByIccid("test-iccid1")).thenReturn(testSimInfo);
        when(vehicleSimRepository.getByIccid("test-iccid1")).thenReturn(testVehicleSim);

        // When
        cardBindingService.handleBindingEvent(testEvent);

        // Then
        verify(simInfoRepository, never()).update(any(SimInfo.class));
        verify(vehicleSimRepository, never()).upsert(any(VehicleSim.class));
    }

    @Test
    void testHandleBindingEvent_NonBindEvent() {
        // Given
        VmdBindingEvent unbindEvent = VmdBindingEvent.builder()
                .bindingId("test-binding-id")
                .changeType("UNBIND")
                .vin("test-vin")
                .build();

        // When
        cardBindingService.handleBindingEvent(unbindEvent);

        // Then
        verify(simInfoRepository, never()).getByIccid(anyString());
        verify(vehicleSimRepository, never()).getByIccid(anyString());
    }

    @Test
    void testIsIccidBound_True() {
        // Given
        when(vehicleSimRepository.getByIccid("test-iccid1")).thenReturn(testVehicleSim);

        // When
        boolean result = cardBindingService.isIccidBound("test-iccid1");

        // Then
        assertTrue(result);
    }

    @Test
    void testIsIccidBound_False() {
        // Given
        when(vehicleSimRepository.getByIccid("test-iccid1")).thenReturn(null);

        // When
        boolean result = cardBindingService.isIccidBound("test-iccid1");

        // Then
        assertFalse(result);
    }
}
