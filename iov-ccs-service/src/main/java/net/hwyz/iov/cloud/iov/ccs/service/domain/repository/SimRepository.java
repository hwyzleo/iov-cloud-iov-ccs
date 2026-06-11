package net.hwyz.iov.cloud.iov.ccs.service.domain.repository;

import com.baomidou.mybatisplus.core.metadata.IPage;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.Sim;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SIM卡仓储接口
 *
 * @author hwyz_leo
 */
public interface SimRepository {

    /**
     * 根据ID查询SIM卡
     *
     * @param id 主键
     * @return SIM卡实体
     */
    Sim getById(Long id);

    /**
     * 根据ICCID查询SIM卡
     *
     * @param iccid ICCID
     * @return SIM卡实体
     */
    Sim getByIccid(String iccid);

    /**
     * 根据ICCID查询有效SIM卡数量
     *
     * @param iccid ICCID
     * @return 数量
     */
    int countByIccid(String iccid);

    /**
     * 保存SIM卡
     *
     * @param sim SIM卡实体
     * @return 影响行数
     */
    int save(Sim sim);

    /**
     * 根据ID删除SIM卡
     *
     * @param id 主键
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据ICCID删除SIM卡
     *
     * @param iccid ICCID
     * @return 影响行数
     */
    int deleteByIccid(String iccid);

    /**
     * 分页查询SIM卡
     *
     * @param iccid     ICCID
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @return 分页结果
     */
    IPage<Sim> pageQuery(String iccid, LocalDateTime beginTime, LocalDateTime endTime, int pageNum, int pageSize);

    /**
     * 查询所有SIM卡
     *
     * @return SIM卡列表
     */
    List<Sim> listAll();
}
