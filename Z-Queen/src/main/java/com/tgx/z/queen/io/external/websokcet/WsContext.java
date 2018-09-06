/*
 * MIT License
 *
 * Copyright (c) 2016~2018 Z-Chess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.tgx.z.queen.io.external.websokcet;

import java.io.IOException;
import java.util.Base64;
import java.util.Random;

import com.tgx.z.queen.base.util.CryptUtil;
import com.tgx.z.queen.event.operator.MODE;
import com.tgx.z.queen.io.core.async.AioContext;
import com.tgx.z.queen.io.core.inf.ISessionOption;

/**
 * @author William.d.zk
 */
public class WsContext
        extends
        AioContext
{
    public final static int HS_State_GET          = 1;
    public final static int HS_State_HOST         = 1 << 1;
    public final static int HS_State_UPGRADE      = 1 << 2;
    public final static int HS_State_CONNECTION   = 1 << 3;
    public final static int HS_State_SEC_KEY      = 1 << 4;
    public final static int HS_State_ORIGIN       = 1 << 5;
    public final static int HS_State_SEC_PROTOCOL = 1 << 6;
    // public final String mSecProtocol, mSubProtocol; //not support right now
    public final static int HS_State_SEC_VERSION  = 1 << 7;
    public final static int HS_State_HTTP_101     = 1 << 8;
    public final static int HS_State_SEC_ACCEPT   = 1 << 9;
    public final static int HS_State_ACCEPT_OK    = HS_State_HTTP_101 | HS_State_SEC_ACCEPT | HS_State_UPGRADE | HS_State_CONNECTION;
    public final static int HS_State_CLIENT_OK    = HS_State_GET
                                                    | HS_State_HOST
                                                    | HS_State_UPGRADE
                                                    | HS_State_CONNECTION
                                                    | HS_State_SEC_KEY
                                                    | HS_State_SEC_VERSION
                                                    | HS_State_ORIGIN;
    public final String     mSecKey, mSecAcceptExpect;
    private final int       mVersion              = 13;
    private final int       mMaxPayloadSize;
    private int             mHandshakeState;
    private WsFrame         mCarrier;
    private WsHandshake     mHandshake;
    private CryptUtil       mCryptUtil            = new CryptUtil();

    public WsContext(ISessionOption option, MODE mode) {
        super(option);
        mMaxPayloadSize = option.setSNF() - 2;
        if (mode.equals(MODE.CONSUMER) || mode.equals(MODE.CONSUMER_SSL)) {
            Random r = new Random(System.nanoTime());
            byte[] seed = new byte[17];
            r.nextBytes(seed);
            mSecKey = Base64.getEncoder()
                            .encodeToString(mCryptUtil.sha1(seed));
            mSecAcceptExpect = getSecAccept(mSecKey);
        }
        else mSecKey = mSecAcceptExpect = null;

        switch (mode) {
            case CLUSTER_SERVER:
            case CLUSTER_CONSUMER:
            case SYMMETRY:
            case MQ_SERVER:
            case MQ_CONSUMER:
                transfer();
                break;
            case CONSUMER:
            case SERVER:
            case CONSUMER_SSL:
            case SERVER_SSL:
                handshake();
                break;
        }
    }

    public WsFrame getCarrier() {
        return mCarrier;
    }

    public void setCarrier(WsFrame frame) {
        mCarrier = frame;
    }

    public void setCarrierNull() {
        mCarrier = null;
    }

    public WsHandshake getHandshake() {
        return mHandshake;
    }

    public void setHandshake(WsHandshake handshake) {
        mHandshake = handshake;
    }

    public void cleanHandshake() {
        mHandshake = null;
    }

    public final int getMaxPayloadSize() {
        return mMaxPayloadSize;
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

    @Override
    public void reset() {
        if (mCarrier != null) mCarrier.reset();
        if (mHandshake != null) mHandshake.dispose();
        mHandshake = null;
        mCarrier = null;
        super.reset();
    }

    @Override
    public void dispose() {
        mCryptUtil = null;
        mHandshake = null;
        mCarrier = null;
        super.dispose();
    }

    public String getSecAccept(String sec_key) {
        return Base64.getEncoder()
                     .encodeToString(mCryptUtil.sha1((sec_key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()));
    }

    public String getSeKey() {
        return mSecKey;
    }

    public final void updateHandshakeState(int state) {
        mHandshakeState |= state;
    }

    public final boolean checkState(int state) {
        return mHandshakeState == state || (mHandshakeState & state) == state;
    }

    public final int getWsVersion() {
        return mVersion;
    }

}
