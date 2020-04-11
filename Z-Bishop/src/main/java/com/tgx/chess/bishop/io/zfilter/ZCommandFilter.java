/*
 * MIT License
 *
 * Copyright (c) 2016~2019 Z-Chess
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
package com.tgx.chess.bishop.io.zfilter;

import java.util.Arrays;
import java.util.Objects;

import com.tgx.chess.bishop.io.ws.WsFrame;
import com.tgx.chess.bishop.io.zprotocol.ZCommand;
import com.tgx.chess.bishop.io.zprotocol.ztls.X01_EncryptRequest;
import com.tgx.chess.bishop.io.zprotocol.ztls.X02_AsymmetricPub;
import com.tgx.chess.bishop.io.zprotocol.ztls.X03_Cipher;
import com.tgx.chess.bishop.io.zprotocol.ztls.X04_EncryptConfirm;
import com.tgx.chess.bishop.io.zprotocol.ztls.X05_EncryptStart;
import com.tgx.chess.bishop.io.zprotocol.ztls.X06_EncryptComp;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.Pair;
import com.tgx.chess.king.config.Code;
import com.tgx.chess.queen.io.core.async.AioFilterChain;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.ICommandFactory;
import com.tgx.chess.queen.io.core.inf.IEncryptHandler;
import com.tgx.chess.queen.io.core.inf.IProtocol;

/**
 * @author William.d.zk
 */
public class ZCommandFilter
        extends
        AioFilterChain<ZContext,
                       ICommand<ZContext>,
                       WsFrame>
{

    private final ICommandFactory<ZContext,
                                  ZCommand,
                                  WsFrame>                     _CommandFactory;
    private final Logger                                       _Logger = Logger.getLogger(getClass().getName());

    public ZCommandFilter(ICommandFactory<ZContext,
                                          ZCommand,
                                          WsFrame> factory)
    {
        super("queen-command-zfilter");
        this._CommandFactory = factory;
    }

    @Override
    public ResultType preEncode(ZContext context, IProtocol output)
    {
        return preCommandEncode(context, output);
    }

    @Override
    public WsFrame encode(ZContext context, ICommand<ZContext> output)
    {
        WsFrame frame = new WsFrame();
        frame.setPayload(output.encode(context));
        frame.setCtrl(WsFrame.frame_op_code_no_ctrl_bin);
        return frame;
    }

    @Override
    public ResultType preDecode(ZContext context, WsFrame input)
    {
        return preCommandDecode(context, input);
    }

    @Override
    public ICommand<ZContext> decode(ZContext context, WsFrame input)
    {
        ZCommand _command = create(input);
        if (Objects.isNull(_command)) return null;
        _command.decode(input.getPayload(), context);
        switch (_command.getCommand())
        {
            case X01_EncryptRequest.COMMAND:
                X01_EncryptRequest x01 = (X01_EncryptRequest) _command;
                IEncryptHandler encryptHandler = context.getEncryptHandler();
                if (encryptHandler == null) return new X06_EncryptComp(Code.PLAIN_UNSUPPORTED.getCode());
                Pair<Integer,
                     byte[]> keyPair = encryptHandler.getAsymmetricPubKey(x01.pubKeyId);
                if (keyPair != null) {
                    X02_AsymmetricPub x02 = new X02_AsymmetricPub();
                    context.setPubKeyId(keyPair.getFirst());
                    x02.setPubKey(keyPair.getFirst(), keyPair.getSecond());
                    return x02;
                }
                else throw new NullPointerException("create public-key failed!");
            case X02_AsymmetricPub.COMMAND:
                X02_AsymmetricPub x02 = (X02_AsymmetricPub) _command;
                encryptHandler = context.getEncryptHandler();
                if (encryptHandler == null) return new X06_EncryptComp(Code.PLAIN_UNSUPPORTED.getCode());
                byte[] symmetricKey = context.getSymmetricEncrypt()
                                             .createKey("z-tls-rc4");
                if (symmetricKey == null) throw new NullPointerException("create symmetric-key failed!");
                keyPair = encryptHandler.getCipher(x02.pubKey, symmetricKey);
                if (keyPair != null) {
                    context.setPubKeyId(x02.pubKeyId);
                    context.setSymmetricKeyId(keyPair.getFirst());
                    context.reRollKey(symmetricKey);
                    X03_Cipher x03 = new X03_Cipher();
                    x03.pubKeyId = x02.pubKeyId;
                    x03.symmetricKeyId = keyPair.getFirst();
                    x03.cipher = keyPair.getSecond();
                    return x03;
                }
                else throw new NullPointerException("encrypt symmetric-key failed!");
            case X03_Cipher.COMMAND:
                X03_Cipher x03 = (X03_Cipher) _command;
                encryptHandler = context.getEncryptHandler();
                if (context.getPubKeyId() == x03.pubKeyId) {
                    symmetricKey = encryptHandler.getSymmetricKey(x03.pubKeyId, x03.cipher);
                    if (symmetricKey == null) throw new NullPointerException("decrypt symmetric-key failed!");
                    context.setSymmetricKeyId(x03.symmetricKeyId);
                    context.reRollKey(symmetricKey);
                    X04_EncryptConfirm x04 = new X04_EncryptConfirm();
                    x04.symmetricKeyId = x03.symmetricKeyId;
                    x04.code = Code.SYMMETRIC_KEY_OK.getCode();
                    x04.setSign(encryptHandler.getSymmetricKeySign(symmetricKey));
                    return x04;
                }
                else {
                    keyPair = encryptHandler.getAsymmetricPubKey(x03.pubKeyId);
                    if (keyPair != null) {
                        x02 = new X02_AsymmetricPub();
                        context.setPubKeyId(keyPair.getFirst());
                        x02.setPubKey(keyPair.getFirst(), keyPair.getSecond());
                        return x02;
                    }
                    else throw new NullPointerException("create public-key failed!");
                }
            case X04_EncryptConfirm.COMMAND:
                X04_EncryptConfirm x04 = (X04_EncryptConfirm) _command;
                encryptHandler = context.getEncryptHandler();
                if (x04.symmetricKeyId == context.getSymmetricKeyId()
                    && Arrays.equals(encryptHandler.getSymmetricKeySign(context.getReRollKey()), x04.getSign()))
                {
                    X05_EncryptStart x05 = new X05_EncryptStart();
                    x05.symmetricKeyId = x04.symmetricKeyId;
                    x05.salt = encryptHandler.nextRandomInt();
                    return x05;
                }
                else {
                    context.reset();
                    return new X01_EncryptRequest();
                }
            case X05_EncryptStart.COMMAND:
                X05_EncryptStart x05 = (X05_EncryptStart) _command;
                if (context.getSymmetricKeyId() != x05.symmetricKeyId) throw new IllegalStateException("symmetric key id is not equals");
                _Logger.debug("encrypt start, no response");
            case X06_EncryptComp.COMMAND:
                return null;
            default:
                return _command;
        }
    }

    private ZCommand create(WsFrame frame)
    {
        switch (frame.getPayload()[1] & 0xFF)
        {
            case X01_EncryptRequest.COMMAND:
                return new X01_EncryptRequest();
            case X02_AsymmetricPub.COMMAND:
                return new X02_AsymmetricPub();
            case X03_Cipher.COMMAND:
                return new X03_Cipher();
            case X04_EncryptConfirm.COMMAND:
                return new X04_EncryptConfirm();
            case X05_EncryptStart.COMMAND:
                return new X05_EncryptStart();
            case X06_EncryptComp.COMMAND:
                return new X06_EncryptComp();
            case 0xFF:
                throw new UnsupportedOperationException();
            default:
                return _CommandFactory != null ? _CommandFactory.create(frame)
                                               : null;
        }
    }

}
