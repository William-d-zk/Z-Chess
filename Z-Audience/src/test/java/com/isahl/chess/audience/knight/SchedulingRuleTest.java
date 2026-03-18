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

import com.isahl.chess.audience.testing.BaseTest;
import com.isahl.chess.knight.engine.SchedulerEngine;
import com.isahl.chess.knight.engine.SchedulingRule;
import com.isahl.chess.knight.policy.Policy;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SchedulingRuleTest
        extends BaseTest
{

    @Test
    void testDispatchRule()
    {
        SchedulingRule.DispatchRule rule = new TestDispatchRule("rule1", Arrays.asList("node1", "node2"));
        assertEquals("rule1", rule.getRuleId());
        assertEquals("DISPATCH", rule.getRuleType());
        assertTrue(rule.validate());
        assertEquals(Arrays.asList("node1", "node2"), rule.getTargetNodes());
        assertFalse(rule.isBroadcast());
    }

    @Test
    void testClaimRule()
    {
        SchedulingRule.ClaimRule rule = new TestClaimRule("claim1", 5, 1000);
        assertEquals("claim1", rule.getRuleId());
        assertEquals("CLAIM", rule.getRuleType());
        assertTrue(rule.validate());
        assertEquals(5, rule.getMaxClaimCount());
        assertEquals(1000, rule.getClaimIntervalMs());
        assertFalse(rule.isExclusive());
    }

    @Test
    void testGroupRule()
    {
        SchedulingRule.GroupRule rule = new TestGroupRule("group1", "group-A", 2);
        assertEquals("group1", rule.getRuleId());
        assertEquals("GROUP", rule.getRuleType());
        assertTrue(rule.validate());
        assertEquals("group-A", rule.getGroupId());
        assertEquals(2, rule.getMinNodesRequired());
        assertFalse(rule.allowCrossGroup());
    }

    @Test
    void testSchedulingRule_validate()
    {
        SchedulingRule rule = new TestSchedulingRule("invalid", null);
        assertFalse(rule.validate());
    }

    static class TestSchedulingRule
            implements SchedulingRule
    {
        private final String ruleId;
        private final String ruleType;

        TestSchedulingRule(String ruleId, String ruleType)
        {
            this.ruleId = ruleId;
            this.ruleType = ruleType;
        }

        @Override
        public String getRuleId()
        {
            return ruleId;
        }

        @Override
        public String getRuleType()
        {
            return ruleType;
        }

        @Override
        public boolean validate()
        {
            return ruleId != null && ruleType != null;
        }

        @Override
        public List<String> resolveTargetNodes(SchedulerEngine.TaskContext context)
        {
            return List.of();
        }

        @Override
        public Policy getPolicy(String policyType)
        {
            return null;
        }
    }

    static class TestDispatchRule
            implements SchedulingRule.DispatchRule
    {
        private final String ruleId;
        private final List<String> targetNodes;

        TestDispatchRule(String ruleId, List<String> targetNodes)
        {
            this.ruleId = ruleId;
            this.targetNodes = targetNodes;
        }

        @Override
        public String getRuleId()
        {
            return ruleId;
        }

        @Override
        public String getRuleType()
        {
            return "DISPATCH";
        }

        @Override
        public boolean validate()
        {
            return ruleId != null && targetNodes != null && !targetNodes.isEmpty();
        }

        @Override
        public List<String> resolveTargetNodes(SchedulerEngine.TaskContext context)
        {
            return targetNodes;
        }

        @Override
        public Policy getPolicy(String policyType)
        {
            return null;
        }

        @Override
        public List<String> getTargetNodes()
        {
            return targetNodes;
        }

        @Override
        public boolean isBroadcast()
        {
            return false;
        }
    }

    static class TestClaimRule
            implements SchedulingRule.ClaimRule
    {
        private final String ruleId;
        private final int maxClaimCount;
        private final long claimIntervalMs;

        TestClaimRule(String ruleId, int maxClaimCount, long claimIntervalMs)
        {
            this.ruleId = ruleId;
            this.maxClaimCount = maxClaimCount;
            this.claimIntervalMs = claimIntervalMs;
        }

        @Override
        public String getRuleId()
        {
            return ruleId;
        }

        @Override
        public String getRuleType()
        {
            return "CLAIM";
        }

        @Override
        public boolean validate()
        {
            return ruleId != null && maxClaimCount > 0;
        }

        @Override
        public List<String> resolveTargetNodes(SchedulerEngine.TaskContext context)
        {
            return List.of();
        }

        @Override
        public Policy getPolicy(String policyType)
        {
            return null;
        }

        @Override
        public int getMaxClaimCount()
        {
            return maxClaimCount;
        }

        @Override
        public long getClaimIntervalMs()
        {
            return claimIntervalMs;
        }

        @Override
        public boolean isExclusive()
        {
            return false;
        }
    }

    static class TestGroupRule
            implements SchedulingRule.GroupRule
    {
        private final String ruleId;
        private final String groupId;
        private final int minNodesRequired;

        TestGroupRule(String ruleId, String groupId, int minNodesRequired)
        {
            this.ruleId = ruleId;
            this.groupId = groupId;
            this.minNodesRequired = minNodesRequired;
        }

        @Override
        public String getRuleId()
        {
            return ruleId;
        }

        @Override
        public String getRuleType()
        {
            return "GROUP";
        }

        @Override
        public boolean validate()
        {
            return ruleId != null && groupId != null && minNodesRequired > 0;
        }

        @Override
        public List<String> resolveTargetNodes(SchedulerEngine.TaskContext context)
        {
            return List.of();
        }

        @Override
        public Policy getPolicy(String policyType)
        {
            return null;
        }

        @Override
        public String getGroupId()
        {
            return groupId;
        }

        @Override
        public int getMinNodesRequired()
        {
            return minNodesRequired;
        }

        @Override
        public boolean allowCrossGroup()
        {
            return false;
        }
    }
}
