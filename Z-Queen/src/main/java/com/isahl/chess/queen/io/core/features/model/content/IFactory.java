package com.isahl.chess.queen.io.core.features.model.content;

import com.isahl.chess.queen.io.core.features.model.channels.IRespConnected;
import com.isahl.chess.queen.io.core.features.model.session.IContext;

import java.nio.ByteBuffer;

/**
 * 核心方法
 * 根据命令字 创建具体的通讯指令
 * 通讯指令 必须是 IRouteLv4 & IStreamProtocol & IControl 的子集
 * IControl 作为 ISession 的最基本的指令单元，严格化的支持 Route 和 Stream 处理
 *
 * @author william.d.zk
 * @see IRespConnected
 */
public interface IFactory<I extends IFrame, C extends IContext>
{
    <T extends IControl> T create(I frame, C context);

    <T extends IControl> T create(int serial, ByteBuffer input);
}
