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

public class BaseRetryPolicy
        implements Policy.RetryPolicy
{
    private final String policyId;
    private final int maxRetries;
    private final long retryDelayMs;
    private final double backoffMultiplier;

    public BaseRetryPolicy(String policyId, int maxRetries, long retryDelayMs, double backoffMultiplier)
    {
        this.policyId = policyId;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        this.backoffMultiplier = backoffMultiplier;
    }

    public BaseRetryPolicy()
    {
        this.policyId = "default";
        this.maxRetries = 3;
        this.retryDelayMs = 1000;
        this.backoffMultiplier = 2.0;
    }

    @Override
    public String getPolicyId()
    {
        return policyId;
    }

    @Override
    public String getPolicyType()
    {
        return "RETRY";
    }

    @Override
    public int getMaxRetries()
    {
        return maxRetries;
    }

    @Override
    public long getRetryDelayMs()
    {
        return retryDelayMs;
    }

    @Override
    public double getBackoffMultiplier()
    {
        return backoffMultiplier;
    }

    @Override
    public boolean validate()
    {
        return maxRetries >= 0 && retryDelayMs >= 0 && backoffMultiplier >= 1.0;
    }

    @Override
    public boolean shouldRetry(int attemptCount, Throwable cause)
    {
        return attemptCount <= maxRetries;
    }

    public long calculateDelay(int attemptCount)
    {
        return (long) (retryDelayMs * Math.pow(backoffMultiplier, attemptCount - 1));
    }
}
