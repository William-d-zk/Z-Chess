/*
 * MIT License
 *
 * Copyright (c) 2022. Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.isahl.chess.rook.storage.db.config;

import com.isahl.chess.knight.raft.config.IRaftConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RookJpaConfig 单元测试
 * 简化版本，不依赖完整 Spring 上下文
 */
class RookJpaConfigTest
{

    @Test
    void testUidConfig()
    {
        // 测试 Uid 配置类的基本功能
        IRaftConfig.Uid uid = new IRaftConfig.Uid();
        
        assertEquals(-1, uid.getIdcId(), "默认 idcId 应为 -1");
        assertEquals(-1, uid.getClusterId(), "默认 clusterId 应为 -1");
        assertEquals(-1, uid.getNodeId(), "默认 nodeId 应为 -1");
        assertEquals(-1, uid.getType(), "默认 type 应为 -1");
        
        // 测试设置有效值
        uid.setIdcId(0);
        uid.setClusterId(0);
        uid.setNodeId(0);
        uid.setType(0);
        
        assertEquals(0, uid.getIdcId());
        assertEquals(0, uid.getClusterId());
        assertEquals(0, uid.getNodeId());
        assertEquals(0, uid.getType());
        
        // 测试 reset
        uid.reset();
        
        assertEquals(-1, uid.getIdcId());
        assertEquals(-1, uid.getClusterId());
        assertEquals(-1, uid.getNodeId());
        assertEquals(-1, uid.getType());
    }
}
