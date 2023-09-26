/*
 * MIT License
 *
 * Copyright (c) 2022~2022. Z-Chess
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

package com.isahl.chess.rook.graphic;

import com.isahl.chess.king.base.features.model.IoSerial;
import com.isahl.chess.rook.graphic.model.GNode;

import java.util.Map;
import java.util.Set;

/**
 * @author william.d.zk
 */
public interface IGraph<V extends IoSerial>
{
    /**
     * @param name    节点名称
     * @param type    节点类型
     * @param degree0 节点与图发生初始联结的边
     * @return 新创建的节点
     */
    GNode<V> create(String name, INode.Type type, IEdge degree0);

    /**
     * @param id 联结图上的节点ID
     * @return 联结成功后新建的边
     */
    IEdge connect(long id);

    /**
     * @return 图上所有点的Set
     */
    Set<GNode<V>> nodes();

    /**
     * @param id 节点ID
     * @return 对应ID的节点
     */
    GNode<V> node(long id);

    /**
     * @param type       节点类型
     * @param conditions 场景特征
     * @return 符合条件的节点Set
     */
    Set<GNode<V>> search(INode.Type type, IScene... conditions);

    IEdge assign(long target, long perspective);

    IEdge deassign(long target, long perspective);

    boolean isAssigned(long target, long perspective);

    /**
     * 节点(perspective) 与 节点(target) 之间形成联结, 如果两点之间已经存在联结,
     * 那么将覆盖原有的场景属性清单。
     * 需要注意的是: IScene 场景只能从节点(access)的视角出发
     *
     * @param perspective 视角节点 ID
     * @param target      目标节点 ID
     * @param attributes  场景属性,依附于节点(perspective) 绑定“视角”
     * @see IScene
     */
    void associate(long perspective, long target, Set<IScene> attributes);

    /**
     * 节点(perspective) 与 节点(target) 之间解除联结, 并不会影响场景属性
     *
     * @param perspective 视角节点 ID
     * @param target      目标节点 ID
     */
    void dissociate(long perspective, long target);

    /**
     * 获取 节点(perspective) 视角下, 与其关联的场景信息
     *
     * @param perspective 视角节点 ID
     * @return target, scene_set 联结到节点(target) 所关联到的场景集合
     */
    Map<Long, Set<IScene>> getPerspectiveAssociations(long perspective);

    /**
     * 获取 节点(target) 为终时, 与其关联的场景信息
     *
     * @param target 目标节点 ID
     * @return perspective, scene_set 由 perspective 起始联结到 节点(target) 的关联场景集合
     */
    Map<Long, Set<IScene>> getTargetAssociations(String target);

    /**
     * 获取从固定视角出发的所有目标节点集合
     *
     * @param perspective 视角节点 ID
     * @return 所有从视角出发 关联到的目标节点集合（出度）
     */
    Set<Long> getTargets(long perspective);

    /**
     * 获取抵达目标节点的 视角节点集合
     *
     * @param target 目的节点 ID
     * @return 所有抵达目标节点的 视角节点集合 (入度)
     */
    Set<Long> getPerspectives(long target);
}
