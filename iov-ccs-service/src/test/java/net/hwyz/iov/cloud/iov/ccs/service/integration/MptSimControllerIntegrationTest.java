package net.hwyz.iov.cloud.iov.ccs.service.integration;

import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.security.auth.AuthLogic;
import net.hwyz.iov.cloud.framework.security.auth.AuthUtil;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.controller.mpt.MptSimController;
import net.hwyz.iov.cloud.iov.ccs.service.adapter.web.vo.request.SimInfoQueryRequest;
import net.hwyz.iov.cloud.iov.ccs.service.application.service.ManualSimService;
import net.hwyz.iov.cloud.iov.ccs.service.domain.model.entity.SimInfo;
import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * MptSimController 集成测试
 * <p>
 * 直接注入 Controller Bean 调用方法，绕过安全框架 AOP 切面。
 * 验证 Controller → Service 调用链路及 PageHelper 分页。
 */
@DisplayName("MptSimController集成测试")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MptSimControllerIntegrationTest extends BaseTest {

    @Autowired
    private MptSimController controller;

    @MockBean
    private ManualSimService manualSimService;

    @BeforeEach
    void setup() {
        // mock AuthLogic 使 AOP 权限检查通过
        AuthUtil.authLogic = Mockito.mock(AuthLogic.class);

        // 设置 MockHttpServletRequest，使 startPage() 能读取分页参数
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("pageNum", "1");
        request.setParameter("pageSize", "10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    @DisplayName("列表查询 - 无筛选条件返回分页数据")
    void list_noFilter() {
        SimInfo sim = SimInfo.builder()
                .id(1L).iccid("89860123456789012345")
                .imsi("460001234567890").msisdn("8613800138000")
                .sourceMno("MANUAL").build();

        when(manualSimService.listSimInfo(any())).thenReturn(List.of(sim));

        SimInfoQueryRequest query = new SimInfoQueryRequest();
        ApiResponse<PageInfo<SimInfo>> result = controller.list(query);

        assertNotNull(result);
        assertEquals("000000", result.getCode());
        assertEquals(1, result.getData().getList().size());
        assertEquals("89860123456789012345", result.getData().getList().get(0).getIccid());
    }

    @Test
    @DisplayName("列表查询 - 带iccid筛选条件")
    void list_withIccidFilter() {
        when(manualSimService.listSimInfo(any())).thenReturn(List.of());

        SimInfoQueryRequest query = new SimInfoQueryRequest();
        query.setIccid("8986");
        ApiResponse<PageInfo<SimInfo>> result = controller.list(query);

        assertNotNull(result);
        assertEquals("000000", result.getCode());
        assertTrue(result.getData().getList().isEmpty());
    }

    @Test
    @DisplayName("列表查询 - 多条件组合筛选")
    void list_multiFilter() {
        SimInfo sim = SimInfo.builder()
                .id(1L).iccid("89860123456789012345")
                .imsi("460001234567890").msisdn("8613800138000")
                .sourceMno("CMCC").build();

        when(manualSimService.listSimInfo(any())).thenReturn(List.of(sim));

        SimInfoQueryRequest query = new SimInfoQueryRequest();
        query.setIccid("8986");
        query.setSourceMno("CMCC");
        ApiResponse<PageInfo<SimInfo>> result = controller.list(query);

        assertNotNull(result);
        assertEquals("000000", result.getCode());
        assertEquals("CMCC", result.getData().getList().get(0).getSourceMno());
    }

    @Test
    @DisplayName("详情查询 - ICCID存在返回数据")
    void getInfo_success() {
        SimInfo sim = SimInfo.builder()
                .iccid("89860123456789012345")
                .imsi("460001234567890").msisdn("8613800138000").build();

        when(manualSimService.getSimInfo("89860123456789012345")).thenReturn(sim);

        ApiResponse<SimInfo> result = controller.getInfo("89860123456789012345");

        assertNotNull(result);
        assertEquals("000000", result.getCode());
        assertEquals("89860123456789012345", result.getData().getIccid());
    }
}
