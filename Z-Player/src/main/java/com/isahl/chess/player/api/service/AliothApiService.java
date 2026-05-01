/*
 * MIT License
 *
 * Copyright (c) 2016~2024. Z-Chess
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

package com.isahl.chess.player.api.service;

import com.isahl.chess.player.api.config.PlayerConfig;
import com.isahl.chess.player.api.model.LcApiTokenDO;
import com.isahl.chess.player.api.model.LcOrderDO;
import com.isahl.chess.player.api.model.RpaAuthDo;
import com.isahl.chess.player.api.model.RpaTaskDO;
import com.isahl.chess.player.api.model.RpaTaskMessageDO;
import com.isahl.chess.player.api.model.TaskStatusDo;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/** 外部 API 服务（已移除 nocobase 依赖） */
@Service
public class AliothApiService {
  private final Logger log = LoggerFactory.getLogger("biz.player." + getClass().getSimpleName());

  private final PlayerConfig playerConfig;

  public AliothApiService(PlayerConfig playerConfig) {
    this.playerConfig = playerConfig;
  }

  /** 更新任务状态（已禁用） */
  public void updateTask(RpaTaskMessageDO message) {
    log.debug("updateTask disabled, message: %s", message);
  }

  /** 获取所有还在履约状态的任务（已禁用，返回空列表） */
  public List<RpaTaskDO> fetchUnfinishedTaskList() {
    log.debug("fetchUnfinishedTaskList disabled, returning empty list");
    return Collections.emptyList();
  }

  /** 获取所有LC api token（已禁用，返回空列表） */
  public List<LcApiTokenDO> fetchLcAppTokenList() {
    log.debug("fetchLcAppTokenList disabled, returning empty list");
    return Collections.emptyList();
  }

  /** 获取指定任务（已禁用，返回空列表） */
  public List<RpaTaskDO> fetchSpecificTask(Long taskId) {
    log.debug("fetchSpecificTask disabled, taskId: %s", taskId);
    return Collections.emptyList();
  }

  /** 获取认证信息（已禁用，返回空列表） */
  public List<RpaAuthDo> fetchAuthInfos() {
    log.debug("fetchAuthInfos disabled, returning empty list");
    return Collections.emptyList();
  }

  /** 查询状态（已禁用，返回 null） */
  public TaskStatusDo queryStatus(String status) {
    log.debug("queryStatus disabled, status: %s", status);
    return null;
  }

  /** 生成验证码（已禁用） */
  public boolean generateVcode(String serialNo) {
    if (!StringUtils.hasText(serialNo)) {
      return false;
    }
    log.debug("generateVcode disabled, serialNo: %s", serialNo);
    return true;
  }

  /** 验证重置出厂初始化（已禁用） */
  public boolean validateReinit(String serialNo, String vcode) {
    log.debug("validateReinit disabled, serialNo: %s", serialNo);
    return false;
  }

  /** 更新任务状态（已禁用） */
  public void updateTaskStatusById(long taskId, long statusId) {
    log.debug("updateTaskStatusById disabled, taskId: %s, statusId: %s", taskId, statusId);
  }

  /** 保存订单信息（已禁用） */
  public void saveOrderInfo(long taskId, long orderId, Double orderPrice) {
    log.debug("saveOrderInfo disabled, taskId: %s, orderId: %s", taskId, orderId);
  }

  /** 将lc订单数据写回系统（已禁用） */
  public void saveLcOrderList(List<LcOrderDO> lcOrderList) {
    log.debug(
        "saveLcOrderList disabled, list size: %s", lcOrderList != null ? lcOrderList.size() : 0);
  }
}
