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

package com.isahl.chess.rook.graphic.graphql;

import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.rook.graphic.INode;
import com.isahl.chess.rook.graphic.model.Direction;

public interface ISearcher<V extends IoSerial>
{
    /**
     * 从图上的指定的节点进行遍历
     *
     * @param start      搜索开始的节点
     * @param propagator 搜索过程中, 通过边时触发的扩散事件处理器
     * @param visitor    搜索过程中, 访问节点的触发事件处理器
     * @param direction  图的遍历方向
     */
    void traverse(INode start, IPropagator propagator, IVisitor<V> visitor, Direction direction);
}
