package net.hwyz.iov.cloud.iov.ccs.service.integration;

import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.OutboxEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.OutboxEventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Outbox 事件仓储集成测试
 * <p>
 * 直接覆盖 {@link net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.repository.OutboxEventRepositoryImpl}
 * 的 insertPo / updatePo 路径。该 Mapper 同样继承 BaseDao 但曾缺少 XML 绑定，
 * 若仅修复 VehicleSim 而在 Outbox 写入时才暴露同样问题，本测试用于回归防护。
 * <p>
 * 与生产消费路径一致：仓储 save 不回填主键，需通过 listPending / listRetryable
 * 取回带 id 的实体后再 update。
 *
 * @author hwyz_leo
 */
@DisplayName("Outbox事件仓储集成测试")
class OutboxEventRepositoryIntegrationTest extends BaseTest {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String EVENT_TYPE_CARD_BINDING = "CARD_BINDING_STATUS";

    /**
     * 查询扫描上限，确保新插入记录（created_time 最新）能出现在结果集中
     */
    private static final int LOOKUP_LIMIT = 100000;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private OutboxEvent buildTestEvent(String aggregateId, String status, Integer retryCount, Integer maxRetry) {
        return OutboxEvent.builder()
                .eventType(EVENT_TYPE_CARD_BINDING)
                .aggregateId(aggregateId)
                .payload("{\"vin\":\"" + aggregateId + "\",\"bindingStatus\":1}")
                .status(status)
                .topic("test-vehicle-sim-binding-status-changed")
                .messageKey(aggregateId)
                .retryCount(retryCount)
                .maxRetry(maxRetry)
                .createdTime(LocalDateTime.now())
                .build();
    }

    /**
     * 保存 pending 事件并通过 listPending 取回带 id 的实体（模拟生产发布扫描路径）
     */
    private OutboxEvent saveAndFetchPending(String aggregateId, Integer retryCount, Integer maxRetry) {
        outboxEventRepository.save(buildTestEvent(aggregateId, STATUS_PENDING, retryCount, maxRetry));
        return outboxEventRepository.listPending(LOOKUP_LIMIT).stream()
                .filter(e -> aggregateId.equals(e.getAggregateId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到刚保存的 pending outbox 事件: " + aggregateId));
    }

    @Test
    @DisplayName("保存Outbox事件并可被查询（insertPo）")
    void save_insertsAndCanBeFetched() {
        OutboxEvent event = buildTestEvent("HWYZTESTINT0001001", STATUS_PENDING, 0, 3);

        int rows = outboxEventRepository.save(event);
        assertEquals(1, rows);

        OutboxEvent found = outboxEventRepository.listPending(LOOKUP_LIMIT).stream()
                .filter(e -> "HWYZTESTINT0001001".equals(e.getAggregateId()))
                .findFirst()
                .orElse(null);
        assertNotNull(found);
        assertNotNull(found.getId());
        assertEquals(EVENT_TYPE_CARD_BINDING, found.getEventType());
        assertEquals(STATUS_PENDING, found.getStatus());
        assertEquals("test-vehicle-sim-binding-status-changed", found.getTopic());
        assertEquals("HWYZTESTINT0001001", found.getMessageKey());
        assertEquals("{\"vin\":\"HWYZTESTINT0001001\",\"bindingStatus\":1}", found.getPayload());
        assertEquals(0, found.getRetryCount());
        assertEquals(3, found.getMaxRetry());
        assertNotNull(found.getCreatedTime());
    }

    @Test
    @DisplayName("保存时retryCount/maxRetry为空使用数据库默认值")
    void save_nullRetryDefaults() {
        OutboxEvent found = saveAndFetchPending("HWYZTESTINT0001002", null, null);

        assertNotNull(found);
        assertEquals(0, found.getRetryCount());
        assertEquals(3, found.getMaxRetry());
    }

    @Test
    @DisplayName("更新Outbox事件状态（updatePo）")
    void update_status() {
        OutboxEvent event = saveAndFetchPending("HWYZTESTINT0001003", 0, 3);

        event.setStatus(STATUS_PUBLISHED);
        event.setPublishedTime(LocalDateTime.now());
        int rows = outboxEventRepository.update(event);
        assertEquals(1, rows);

        OutboxEvent found = outboxEventRepository.getById(event.getId());
        assertEquals(STATUS_PUBLISHED, found.getStatus());
        assertNotNull(found.getPublishedTime());
    }

    @Test
    @DisplayName("更新Outbox事件失败重试信息（updatePo）")
    void update_failureInfo() {
        outboxEventRepository.save(buildTestEvent("HWYZTESTINT0001004", STATUS_FAILED, 0, 3));

        OutboxEvent event = outboxEventRepository.listRetryable(LOOKUP_LIMIT).stream()
                .filter(e -> "HWYZTESTINT0001004".equals(e.getAggregateId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到刚保存的 retryable outbox 事件"));

        event.setRetryCount(1);
        event.setFailureReason("kafka send timeout");
        int rows = outboxEventRepository.update(event);
        assertEquals(1, rows);

        OutboxEvent found = outboxEventRepository.getById(event.getId());
        assertEquals(1, found.getRetryCount());
        assertEquals("kafka send timeout", found.getFailureReason());
    }

    @Test
    @DisplayName("查询待发布事件包含新建记录")
    void listPending_containsSaved() {
        String aggregateId = "HWYZTESTINT0001005";
        outboxEventRepository.save(buildTestEvent(aggregateId, STATUS_PENDING, 0, 3));

        List<OutboxEvent> pending = outboxEventRepository.listPending(LOOKUP_LIMIT);
        assertTrue(pending.stream().anyMatch(e -> aggregateId.equals(e.getAggregateId())));
    }

    @Test
    @DisplayName("查询可重试事件包含失败未超限记录")
    void listRetryable_containsSaved() {
        String aggregateId = "HWYZTESTINT0001006";
        outboxEventRepository.save(buildTestEvent(aggregateId, STATUS_FAILED, 1, 3));

        List<OutboxEvent> retryable = outboxEventRepository.listRetryable(LOOKUP_LIMIT);
        assertTrue(retryable.stream().anyMatch(e -> aggregateId.equals(e.getAggregateId())));
    }
}
