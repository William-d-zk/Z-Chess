package com.isahl.chess.queen.io.core.features.model.content;

import com.isahl.chess.king.base.features.model.IoFactory;
import com.isahl.chess.queen.io.core.features.model.channels.IAfterConnected;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;

/**
 * 核心方法
 * 根据命令字 创建具体的通讯指令
 * 通讯指令 必须是 IRouteLv4 & IStreamProtocol & IControl 的子集
 * IControl 作为 ISession 的最基本的指令单元，严格化的支持 Route 和 Stream 处理
 *
 * @author william.d.zk
 * @see IAfterConnected
 */
public interface IProtocolFactory<I extends IFrame, C extends IPContext>
        extends IoFactory
{
    IControl<C> create(I frame, C context);
}
