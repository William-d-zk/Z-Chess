package com.isahl.chess.pawn.endpoint.device;

import com.isahl.chess.pawn.endpoint.device.resource.features.IStateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StateService 基础测试
 */
@SpringBootTest
public class StateServiceTest {

    @Autowired(required = false)
    private IStateService stateService;

    @Test
    public void testContextLoads() {
        // 验证 Spring 上下文可以加载
    }
}
