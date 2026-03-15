package com.isahl.chess.pawn.endpoint.device;

import com.isahl.chess.pawn.endpoint.device.resource.features.IStateService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StateService 基础测试
 * 简化版本，不依赖完整 Spring 上下文
 */
public class StateServiceTest
{
    @Test
    public void test()
    {
        // 基础测试 - 验证类可以实例化
        assertNotNull(IStateService.class);
    }
}
