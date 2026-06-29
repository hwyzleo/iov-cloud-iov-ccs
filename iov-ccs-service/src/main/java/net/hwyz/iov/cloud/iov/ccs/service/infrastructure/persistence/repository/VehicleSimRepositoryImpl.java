package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.VehicleSim;
import net.hwyz.iov.cloud.iov.ccs.service.domain.repository.VehicleSimRepository;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.mapper.VehicleSimMapper;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.persistence.po.VehicleSimPo;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 车卡关联仓储实现
 *
 * @author hwyz_leo
 */
@Repository
@RequiredArgsConstructor
public class VehicleSimRepositoryImpl implements VehicleSimRepository {

    private final VehicleSimMapper vehicleSimMapper;

    @Override
    public VehicleSim getByIccid(String iccid) {
        VehicleSimPo po = vehicleSimMapper.selectByIccid(iccid);
        return po != null ? convertToEntity(po) : null;
    }

    @Override
    public List<VehicleSim> listByVin(String vin) {
        List<VehicleSimPo> poList = vehicleSimMapper.selectByVin(vin);
        return poList.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByIccid(String iccid) {
        return vehicleSimMapper.existsByIccid(iccid);
    }

    @Override
    public int save(VehicleSim vehicleSim) {
        VehicleSimPo po = convertToPo(vehicleSim);
        return vehicleSimMapper.insertPo(po);
    }

    @Override
    public int update(VehicleSim vehicleSim) {
        VehicleSimPo po = convertToPo(vehicleSim);
        return vehicleSimMapper.updatePo(po);
    }

    @Override
    public void upsert(VehicleSim vehicleSim) {
        VehicleSimPo existingPo = vehicleSimMapper.selectByIccid(vehicleSim.getIccid());

        if (existingPo != null) {
            // 更新
            existingPo.setVin(vehicleSim.getVin());
            existingPo.setCardSlot(vehicleSim.getCardSlot());
            existingPo.setBindingStatus(vehicleSim.getBindingStatus());
            existingPo.setPackageType(vehicleSim.getPackageType());
            existingPo.setTboxSn(vehicleSim.getTboxSn());
            existingPo.setSourceEventId(vehicleSim.getSourceEventId());
            existingPo.setSourceSeq(vehicleSim.getSourceSeq());
            existingPo.setBoundTime(vehicleSim.getBoundTime());
            vehicleSimMapper.updatePo(existingPo);
        } else {
            // 插入
            VehicleSimPo po = convertToPo(vehicleSim);
            vehicleSimMapper.insertPo(po);
        }
    }

    @Override
    public List<VehicleSim> listByBindingStatus(Integer bindingStatus) {
        List<VehicleSimPo> poList = vehicleSimMapper.selectByBindingStatus(bindingStatus);
        return poList.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
    }

    @Override
    public List<VehicleSim> listAll() {
        List<VehicleSimPo> poList = vehicleSimMapper.selectAll();
        return poList.stream()
                .map(this::convertToEntity)
                .collect(Collectors.toList());
    }

    /**
     * PO转实体
     *
     * @param po 持久化对象
     * @return 实体
     */
    private VehicleSim convertToEntity(VehicleSimPo po) {
        return VehicleSim.builder()
                .id(po.getId())
                .vin(po.getVin())
                .iccid(po.getIccid())
                .cardSlot(po.getCardSlot())
                .bindingStatus(po.getBindingStatus())
                .packageType(po.getPackageType())
                .tboxSn(po.getTboxSn())
                .sourceEventId(po.getSourceEventId())
                .sourceSeq(po.getSourceSeq())
                .boundTime(po.getBoundTime())
                .createdTime(po.getCreatedTime())
                .updatedTime(po.getUpdatedTime())
                .build();
    }

    /**
     * 实体转PO
     *
     * @param entity 实体
     * @return 持久化对象
     */
    private VehicleSimPo convertToPo(VehicleSim entity) {
        return VehicleSimPo.builder()
                .id(entity.getId())
                .vin(entity.getVin())
                .iccid(entity.getIccid())
                .cardSlot(entity.getCardSlot())
                .bindingStatus(entity.getBindingStatus())
                .packageType(entity.getPackageType())
                .tboxSn(entity.getTboxSn())
                .sourceEventId(entity.getSourceEventId())
                .sourceSeq(entity.getSourceSeq())
                .boundTime(entity.getBoundTime())
                .build();
    }
}
