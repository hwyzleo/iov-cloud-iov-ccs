package net.hwyz.iov.cloud.iov.ccs.service.infrastructure.mno.cmcc;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.ccs.service.infrastructure.mno.MnoBindingClient;
import org.springframework.stereotype.Component;

/**
 * CMCC车卡绑定客户端实现
 * <p>
 * 调用中国移动车卡绑定/入库接口
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class CmccBindingClient implements MnoBindingClient {

    private static final String MNO_TYPE = "CMCC";

    @Override
    public boolean bindCard(String iccid, String vin) {
        log.info("调用CMCC车卡绑定接口: iccid={}, vin={}", iccid, vin);

        // TODO: 实现真实的CMCC车卡绑定接口调用
        // 1. 获取Token
        // 2. 构建请求参数
        // 3. 调用车卡绑定接口
        // 4. 解析响应

        // Mock实现：返回成功
        log.info("CMCC车卡绑定成功: iccid={}, vin={}", iccid, vin);
        return true;
    }

    @Override
    public String getMnoType() {
        return MNO_TYPE;
    }
}
