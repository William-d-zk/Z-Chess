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

package com.isahl.chess.audience.knight;

import static org.junit.jupiter.api.Assertions.*;

import com.isahl.chess.audience.testing.BaseTest;
import com.isahl.chess.knight.policy.BaseRetryPolicy;
import com.isahl.chess.knight.policy.Policy;
import com.isahl.chess.knight.policy.RoundRobinLoadBalancePolicy;
import org.junit.jupiter.api.Test;

public class PolicyTest extends BaseTest {

  @Test
  void testBaseRetryPolicy_validate() {
    BaseRetryPolicy policy = new BaseRetryPolicy("test-retry", 3, 1000, 2.0);
    assertTrue(policy.validate());
    assertEquals("test-retry", policy.getPolicyId());
    assertEquals("RETRY", policy.getPolicyType());
    assertEquals(3, policy.getMaxRetries());
    assertEquals(1000, policy.getRetryDelayMs());
    assertEquals(2.0, policy.getBackoffMultiplier());
  }

  @Test
  void testBaseRetryPolicy_shouldRetry() {
    BaseRetryPolicy policy = new BaseRetryPolicy("test-retry", 3, 1000, 2.0);
    assertTrue(policy.shouldRetry(1, null));
    assertTrue(policy.shouldRetry(2, null));
    assertTrue(policy.shouldRetry(3, null));
    assertFalse(policy.shouldRetry(4, null));
  }

  @Test
  void testBaseRetryPolicy_calculateDelay() {
    BaseRetryPolicy policy = new BaseRetryPolicy("test-retry", 3, 1000, 2.0);
    assertEquals(1000, policy.calculateDelay(1));
    assertEquals(2000, policy.calculateDelay(2));
    assertEquals(4000, policy.calculateDelay(3));
  }

  @Test
  void testBaseRetryPolicy_invalid() {
    BaseRetryPolicy invalidPolicy = new BaseRetryPolicy("invalid", -1, -100, 0.5);
    assertFalse(invalidPolicy.validate());
  }

  @Test
  void testRoundRobinLoadBalancePolicy() {
    RoundRobinLoadBalancePolicy policy = new RoundRobinLoadBalancePolicy("round-robin");
    assertTrue(policy.validate());
    assertEquals("round-robin", policy.getPolicyId());
    assertEquals("LOAD_BALANCE", policy.getPolicyType());
  }

  @Test
  void testRoundRobinLoadBalancePolicy_selectNode() {
    RoundRobinLoadBalancePolicy policy = new RoundRobinLoadBalancePolicy("round-robin");
    java.util.List<String> nodes = java.util.Arrays.asList("node1", "node2", "node3");

    String first = policy.selectNode("task1", nodes);
    assertNotBlank(first);

    String second = policy.selectNode("task2", nodes);
    assertNotBlank(second);

    String third = policy.selectNode("task3", nodes);
    assertNotBlank(third);
  }

  @Test
  void testRoundRobinLoadBalancePolicy_emptyNodes() {
    RoundRobinLoadBalancePolicy policy = new RoundRobinLoadBalancePolicy("round-robin");
    assertNull(policy.selectNode("task1", java.util.List.of()));
    assertNull(policy.selectNode("task1", null));
  }

  @Test
  void testPolicyInterfaces() {
    assertTrue(Policy.RetryPolicy.class.isInterface());
    assertTrue(Policy.TimeoutPolicy.class.isInterface());
    assertTrue(Policy.LoadBalancePolicy.class.isInterface());
    assertTrue(Policy.FailoverPolicy.class.isInterface());
  }
}
