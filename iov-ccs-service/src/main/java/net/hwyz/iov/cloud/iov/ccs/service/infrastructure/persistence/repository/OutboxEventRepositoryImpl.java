package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.OutboxEvent;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.OutboxEventRepository;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper.OutboxEventMapper;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.OutboxEventPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Outbox 事件仓储实现
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class OutboxEventRepositoryImpl implements OutboxEventRepository {

    private final OutboxEventMapper outboxEventMapper;

    @Override
    public int save(OutboxEvent event) {
        OutboxEventPo po = convertToPo(event);
        return outboxEventMapper.insertPo(po);
    }

    @Override
    public int update(OutboxEvent event) {
        OutboxEventPo po = convertToPo(event);
        return outboxEventMapper.updatePo(po);
    }

    @Override
    public List<OutboxEvent> listPending(int limit) {
        List<OutboxEventPo> poList = outboxEventMapper.selectPending(limit);
        return poList.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<OutboxEvent> listRetryable(int limit) {
        List<OutboxEventPo> poList = outboxEventMapper.selectRetryable(limit);
        return poList.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
    }

    @Override
    public OutboxEvent getById(Long id) {
        OutboxEventPo po = outboxEventMapper.selectById(id);
        return po != null ? convertToEntity(po) : null;
    }

    private OutboxEvent convertToEntity(OutboxEventPo po) {
        return OutboxEvent.builder()
                .id(po.getId())
                .eventType(po.getEventType())
                .aggregateId(po.getAggregateId())
                .payload(po.getPayload())
                .status(po.getStatus())
                .topic(po.getTopic())
                .messageKey(po.getMessageKey())
                .retryCount(po.getRetryCount())
                .maxRetry(po.getMaxRetry())
                .failureReason(po.getFailureReason())
                .createdTime(po.getCreatedTime())
                .publishedTime(po.getPublishedTime())
                .updatedTime(po.getUpdatedTime())
                .build();
    }

    private OutboxEventPo convertToPo(OutboxEvent entity) {
        return OutboxEventPo.builder()
                .id(entity.getId())
                .eventType(entity.getEventType())
                .aggregateId(entity.getAggregateId())
                .payload(entity.getPayload())
                .status(entity.getStatus())
                .topic(entity.getTopic())
                .messageKey(entity.getMessageKey())
                .retryCount(entity.getRetryCount())
                .maxRetry(entity.getMaxRetry())
                .failureReason(entity.getFailureReason())
                .createdTime(entity.getCreatedTime())
                .publishedTime(entity.getPublishedTime())
                .build();
    }
}
