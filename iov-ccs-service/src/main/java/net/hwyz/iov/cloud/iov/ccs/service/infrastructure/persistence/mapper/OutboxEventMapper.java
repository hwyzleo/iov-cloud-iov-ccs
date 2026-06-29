package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.framework.mysql.dao.BaseDao;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.OutboxEventPo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Outbox 事件表 DAO
 *
 * @author hwyz_leo
 */
@Mapper
public interface OutboxEventMapper extends BaseDao<OutboxEventPo, Long> {

    /**
     * 根据 ID 查询
     *
     * @param id 主键 ID
     * @return outbox 事件
     */
    @Select("SELECT * FROM tb_outbox_event WHERE id = #{id}")
    OutboxEventPo selectById(@Param("id") Long id);

    /**
     * 查询待发布的事件
     *
     * @param limit 限制数量
     * @return 待发布事件列表
     */
    @Select("SELECT * FROM tb_outbox_event WHERE status = 'PENDING' ORDER BY created_time ASC LIMIT #{limit}")
    List<OutboxEventPo> selectPending(@Param("limit") int limit);

    /**
     * 查询失败且可重试的事件
     *
     * @param limit 限制数量
     * @return 失败事件列表
     */
    @Select("SELECT * FROM tb_outbox_event WHERE status = 'FAILED' AND retry_count < max_retry ORDER BY updated_time ASC LIMIT #{limit}")
    List<OutboxEventPo> selectRetryable(@Param("limit") int limit);
}
