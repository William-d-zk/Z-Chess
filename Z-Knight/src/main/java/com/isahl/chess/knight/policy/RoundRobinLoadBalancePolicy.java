/*
 * MIT License
 *
 * Copyright (c) 2016~2021. Z-Chess
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

package com.isahl.chess.knight.policy;

import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinLoadBalancePolicy implements Policy.LoadBalancePolicy {
  private final String policyId;
  private final AtomicInteger sequence;

  public RoundRobinLoadBalancePolicy(String policyId) {
    this.policyId = policyId;
    this.sequence = new AtomicInteger(0);
  }

  @Override
  public String getPolicyId() {
    return policyId;
  }

  @Override
  public String getPolicyType() {
    return "LOAD_BALANCE";
  }

  @Override
  public boolean validate() {
    return true;
  }

  @Override
  public String selectNode(String taskId, Iterable<String> availableNodes) {
    if (availableNodes == null) {
      return null;
    }
    String selected = null;
    int count = 0;
    int index = sequence.getAndIncrement();
    for (String node : availableNodes) {
      if (count++ == index) {
        selected = node;
        break;
      }
    }
    return selected;
  }
}
