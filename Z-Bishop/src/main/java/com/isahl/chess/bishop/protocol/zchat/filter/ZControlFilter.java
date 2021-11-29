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
package com.isahl.chess.bishop.protocol.zchat.filter;

import com.isahl.chess.bishop.protocol.ws.model.WsFrame;
import com.isahl.chess.bishop.protocol.zchat.ZContext;
import com.isahl.chess.bishop.protocol.zchat.model.ctrl.zls.*;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.king.config.CodeKing;
import com.isahl.chess.queen.io.core.features.model.content.*;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IPContext;
import com.isahl.chess.queen.io.core.features.model.session.proxy.IProxyContext;
import com.isahl.chess.queen.io.core.features.model.session.zls.IEContext;
import com.isahl.chess.queen.io.core.features.model.session.zls.IEncryptor;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author William.d.zk
 */
public class ZControlFilter<T extends ZContext>
        extends AioFilterChain<T, IControl, IFrame>
{

    private final IFactory<IFrame, T> _CommandFactory;

    public ZControlFilter(IFactory<IFrame, T> factory)
    {
        super("z_control");
        _CommandFactory = Objects.requireNonNull(factory);
    }

    @Override
    public WsFrame encode(T context, IControl output)
    {
        WsFrame frame = new WsFrame();
        frame.put(output.encode(context)
                        .array());
        frame.put(WsFrame.frame_op_code_no_ctrl_bin);
        return frame;
    }

    @Override
    public IControl decode(T context, IFrame input)
    {
        IControl control = _CommandFactory.create(input, context);
        if(control != null) {
            switch(control.serial()) {
                case 0x01:
                    if(context instanceof IEContext ec) {
                        X01_EncryptRequest x01 = (X01_EncryptRequest) control;
                        IEncryptor encryptHandler = ec.getEncryptHandler();
                        if(encryptHandler == null) {
                            return new X06_EncryptComp().withCode(CodeKing.PLAIN_UNSUPPORTED.getCode());
                        }
                        Pair<Integer, byte[]> keyPair = encryptHandler.getAsymmetricPubKey(x01.pubKeyId);
                        if(keyPair != null) {
                            X02_AsymmetricPub x02 = new X02_AsymmetricPub();
                            ec.setPubKeyId(keyPair.getFirst());
                            x02.setPubKey(keyPair.getFirst(), keyPair.getSecond());
                            return x02;
                        }
                        else {throw new NullPointerException("create public-key failed!");}
                    }
                    throw new UnsupportedOperationException(String.format("context: %s is not a IEContext", context));
                case 0x02:
                    if(context instanceof IEContext ec) {
                        X02_AsymmetricPub x02 = (X02_AsymmetricPub) control;
                        IEncryptor encryptHandler = ec.getEncryptHandler();
                        byte[] symmetricKey = ec.getSymmetricEncrypt()
                                                .createKey("z-tls-rc4");
                        if(symmetricKey == null) {throw new NullPointerException("create symmetric-key failed!");}
                        Pair<Integer, byte[]> keyPair = encryptHandler.getCipher(x02.pubKey, symmetricKey);
                        if(keyPair != null) {
                            ec.setPubKeyId(x02.pubKeyId);
                            ec.setSymmetricKeyId(keyPair.getFirst());
                            ec.reRollKey(symmetricKey);
                            X03_Cipher x03 = new X03_Cipher();
                            x03.pubKeyId = x02.pubKeyId;
                            x03.symmetricKeyId = keyPair.getFirst();
                            x03.cipher = keyPair.getSecond();
                            return x03;
                        }
                        else {throw new NullPointerException("encrypt symmetric-key failed!");}

                    }
                    throw new UnsupportedOperationException(String.format("context: %s is not a IEContext", context));
                case 0x03:
                    if(context instanceof IEContext ec) {
                        X03_Cipher x03 = (X03_Cipher) control;
                        IEncryptor encryptHandler = ec.getEncryptHandler();
                        if(ec.getPubKeyId() == x03.pubKeyId) {
                            byte[] symmetricKey = encryptHandler.getSymmetricKey(x03.pubKeyId, x03.cipher);
                            if(symmetricKey == null) {throw new NullPointerException("decrypt symmetric-key failed!");}
                            ec.setSymmetricKeyId(x03.symmetricKeyId);
                            ec.reRollKey(symmetricKey);
                            X04_EncryptConfirm x04 = new X04_EncryptConfirm();
                            x04.symmetricKeyId = x03.symmetricKeyId;
                            x04.code = CodeKing.SYMMETRIC_KEY_OK.getCode();
                            x04.setSign(encryptHandler.getSymmetricKeySign(symmetricKey));
                            return x04;
                        }
                        else {
                            Pair<Integer, byte[]> keyPair = encryptHandler.getAsymmetricPubKey(x03.pubKeyId);
                            if(keyPair != null) {
                                X02_AsymmetricPub x02 = new X02_AsymmetricPub();
                                ec.setPubKeyId(keyPair.getFirst());
                                x02.setPubKey(keyPair.getFirst(), keyPair.getSecond());
                                return x02;
                            }
                            else {throw new NullPointerException("create public-key failed!");}
                        }

                    }
                    throw new UnsupportedOperationException(String.format("context: %s is not a IEContext", context));
                case 0x04:
                    if(context instanceof IEContext ec) {
                        X04_EncryptConfirm x04 = (X04_EncryptConfirm) control;
                        IEncryptor encryptHandler = ec.getEncryptHandler();
                        if(x04.symmetricKeyId == ec.getSymmetricKeyId() &&
                           Arrays.equals(encryptHandler.getSymmetricKeySign(ec.getReRollKey()), x04.getSign()))
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
                    }
                    throw new UnsupportedOperationException(String.format("context: %s is not a IEContext", context));
                case 0x05:
                    if(context instanceof IEContext ec) {
                        X05_EncryptStart x05 = (X05_EncryptStart) control;
                        if(ec.getSymmetricKeyId() != x05.symmetricKeyId) {
                            throw new IllegalStateException("symmetric key id is not equals");
                        }
                        _Logger.debug("encrypt start, no response");
                    }
                    throw new UnsupportedOperationException(String.format("context: %s is not a IEContext", context));
                case 0x06:
                    return null;
                default:
                    return control;
            }
        }
        return null;
    }

    @Override
    public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output)
    {
        if(checkType(output, IProtocol.PROTOCOL_BISHOP_COMMAND_SERIAL)) {
            if(context instanceof ZContext && context.isOutConvert()) {
                return new Pair<>(ResultType.NEXT_STEP, context);
            }
            IPContext acting = context;
            while(acting.isProxy()) {
                acting = ((IProxyContext<?>) acting).getActingContext();
                if(acting instanceof ZContext && acting.isOutConvert()) {
                    return new Pair<>(ResultType.NEXT_STEP, acting);
                }
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input)
    {
        if(checkType(input, IProtocol.PROTOCOL_BISHOP_FRAME_SERIAL) && !((IFrame) input).isCtrl()) {
            if(context instanceof ZContext && context.isInConvert()) {
                return new Pair<>(ResultType.HANDLED, context);
            }
            IPContext acting = context;
            while(acting.isProxy()) {
                acting = ((IProxyContext<?>) acting).getActingContext();
                if(acting instanceof ZContext && acting.isInConvert()) {
                    return new Pair<>(ResultType.HANDLED, acting);
                }
            }
        }
        return new Pair<>(ResultType.IGNORE, context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> I pipeEncode(IPContext context, O output)
    {
        return (I) encode((T) context, (ICommand) output);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input)
    {
        return (O) decode((T) context, (IFrame) input);
    }

}
