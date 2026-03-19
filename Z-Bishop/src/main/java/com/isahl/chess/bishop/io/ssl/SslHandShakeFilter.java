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

package com.isahl.chess.bishop.io.ssl;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.*;

import com.isahl.chess.bishop.protocol.zchat.model.ctrl.X07_SslHandShakeSend;
import com.isahl.chess.king.base.content.ByteBuf;
import com.isahl.chess.king.base.util.Pair;
import com.isahl.chess.queen.io.core.features.model.content.IControl;
import com.isahl.chess.queen.io.core.features.model.content.IPacket;
import com.isahl.chess.queen.io.core.features.model.content.IProtocol;
import com.isahl.chess.queen.io.core.features.model.session.IPContext;
import com.isahl.chess.queen.io.core.net.socket.AioFilterChain;
import com.isahl.chess.queen.io.core.net.socket.AioPacket;
import javax.net.ssl.SSLEngineResult;

/**
 * @author William.d.zk
 */
public class SslHandShakeFilter<A extends IPContext>
    extends AioFilterChain<SSLZContext<A>, IProtocol, IPacket> {
  public SslHandShakeFilter() {
    super("z-ssl-handshake");
  }

  @Override
  public IPacket encode(SSLZContext<A> context, IProtocol output) {
    // Check if there's handshake data in carrier (generated during pipePeek)
    IPacket carrier = context.getCarrier();
    if (carrier != null && carrier.getBuffer().isReadable()) {
      _Logger.info(
          "SSL encode: sending handshake data from carrier, bytes=%d",
          carrier.getBuffer().readableBytes());
      // Clear carrier after retrieving data to avoid sending twice
      context.setCarrier(null);
      return carrier;
    }

    // Normal encoding for application data (after handshake)
    SSLEngineResult.HandshakeStatus handshakeStatus;
    ByteBuf netOutBuffer = null;
    do {
      if (netOutBuffer == null) {
        netOutBuffer = context.doWrap(output.encode());
      } else {
        netOutBuffer.append(context.doWrap(output.encode()));
      }
      handshakeStatus = context.getHandShakeStatus();
    } while (handshakeStatus == NEED_WRAP);
    return new AioPacket(netOutBuffer);
  }

  @Override
  public IControl<A> decode(SSLZContext<A> context, IPacket input) {
    IPacket handShakePacket = context.getCarrier();
    // Use X07_SslHandShakeSend which sends data when with(session) is called
    X07_SslHandShakeSend x07 = new X07_SslHandShakeSend();
    if (handShakePacket != null) {
      // Carrier contains handshake data (e.g., Server Hello)
      _Logger.info(
          "SSL decode: got handshake packet with %d bytes",
          handShakePacket.getBuffer().readableBytes());
      // Set the handshake packet to be sent when session is available
      x07.setHandShakePacket(handShakePacket);
      x07.setSslContext(context);
    }
    SSLEngineResult.HandshakeStatus handshakeStatus = context.getHandShakeStatus();
    x07.setHandshakeStatus(handshakeStatus);
    context.reset();
    // Return the control message to trigger further processing
    // The with(session) method will be called by the framework, which triggers send
    return (IControl<A>) x07;
  }

  @Override
  public <O extends IProtocol> Pair<ResultType, IPContext> pipeSeek(IPContext context, O output) {
    _Logger.info(
        "SSL pipeSeek entered: outputType=%d, isProxy=%s, isOutFrame=%s, isSslContext=%s",
        output._super(), context.isProxy(), context.isOutFrame(), context instanceof SSLZContext);
    if (checkType(output, IProtocol.PROTOCOL_BISHOP_CONTROL_SERIAL)
        && context.isProxy()
        && context instanceof SSLZContext ssl_ctx
        && context.isOutFrame()) {
      SSLEngineResult.HandshakeStatus handshakeStatus = ssl_ctx.getHandShakeStatus();
      _Logger.debug("SSL pipeSeek: handshakeStatus=%s", handshakeStatus);
      boolean loop = false;
      do {
        switch (handshakeStatus) {
          case NOT_HANDSHAKING, FINISHED -> {
            loop = false;
            context.promotionOut();
            _Logger.info("SSL handshake finished, ready to write");
          }
          case NEED_TASK -> {
            _Logger.debug("SSL pipeSeek: running delegated tasks");
            handshakeStatus = ssl_ctx.doTask();
            loop = true;
          }
          case NEED_WRAP -> {
            _Logger.debug("SSL pipeSeek: need wrap, returning HANDLED");
            return new Pair<>(ResultType.HANDLED, ssl_ctx);
          }
          case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> {
            _Logger.debug("SSL pipeSeek: need unwrap, returning NEED_DATA");
            return new Pair<>(ResultType.NEED_DATA, ssl_ctx);
          }
        }
      } while (loop);
    }
    return new Pair<>(ResultType.IGNORE, context);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <I extends IProtocol> Pair<ResultType, IPContext> pipePeek(IPContext context, I input) {
    boolean typeCheck = checkType(input, IProtocol.IO_QUEEN_PACKET_SERIAL);
    boolean isProxy = context.isProxy();
    boolean isInFrame = context.isInFrame();
    boolean isSslContext = context instanceof SSLZContext;
    boolean isIPacket = input instanceof IPacket;
    _Logger.info(
        "SSL pipePeek entered: inputType=%d, typeCheck=%s, isProxy=%s, isInFrame=%s, isSslContext=%s, isIPacket=%s",
        input._super(), typeCheck, isProxy, isInFrame, isSslContext, isIPacket);
    if (typeCheck && isProxy && isSslContext && isInFrame && isIPacket) {
      _Logger.info("SSL pipePeek: all conditions passed, entering handshake handling");
      SSLZContext ssl_ctx = (SSLZContext) context;
      IPacket in_packet = (IPacket) input;
      SSLEngineResult.HandshakeStatus handshakeStatus = ssl_ctx.getHandShakeStatus();
      ByteBuf netInBuffer = in_packet.getBuffer();
      _Logger.info(
          "SSL pipePeek: handshakeStatus=%s, inputReadable=%d, isReadable=%s",
          handshakeStatus, netInBuffer.readableBytes(), netInBuffer.isReadable());
      ByteBuf appInBuffer = null;
      boolean loop = netInBuffer.isReadable();
      while (loop) {
        switch (handshakeStatus) {
          case NOT_HANDSHAKING, FINISHED -> {
            context.promotionIn();
            _Logger.info("SSL handshake finished, ready to read");
            return new Pair<>(ResultType.IGNORE, ssl_ctx);
          }
          case NEED_WRAP -> {
            _Logger.debug("SSL pipePeek: need wrap, returning HANDLED");
            if (appInBuffer != null) {
              ssl_ctx.setCarrier(new AioPacket(appInBuffer));
            }
            return new Pair<>(ResultType.HANDLED, ssl_ctx);
          }
          case NEED_TASK -> {
            _Logger.debug("SSL pipePeek: running delegated tasks");
            handshakeStatus = ssl_ctx.doTask();
          }
          case NEED_UNWRAP, NEED_UNWRAP_AGAIN -> {
            _Logger.info("SSL pipePeek: unwrapping...");
            appInBuffer = ssl_ctx.doUnwrap(netInBuffer);
            handshakeStatus = ssl_ctx.getHandShakeStatus();
            _Logger.info("SSL pipePeek: after unwrap, handshakeStatus=%s", handshakeStatus);
            // After unwrap, if handshake status becomes NEED_WRAP, generate handshake data
            // immediately
            if (handshakeStatus == NEED_WRAP) {
              _Logger.info(
                  "SSL pipePeek: status changed to NEED_WRAP, generating handshake response");
              // Generate Server Hello by calling doWrap with empty buffer
              // Use a small empty buffer for handshake wrap (SSL engine generates handshake data
              // even with empty input)
              ByteBuf emptyBuf = new ByteBuf(1, false);
              ByteBuf handShakeData = ssl_ctx.doWrap(emptyBuf);
              if (handShakeData != null && handShakeData.isReadable()) {
                ssl_ctx.setCarrier(new AioPacket(handShakeData));
                _Logger.info(
                    "SSL pipePeek: generated handshake response, bytes=%d",
                    handShakeData.readableBytes());
              }
              return new Pair<>(ResultType.HANDLED, ssl_ctx);
            }
            loop =
                handshakeStatus != NEED_UNWRAP && handshakeStatus != NEED_UNWRAP_AGAIN
                    || netInBuffer.isReadable();
          }
        }
      }
      _Logger.debug("SSL pipePeek: returning NEED_DATA");
      return new Pair<>(ResultType.NEED_DATA, context);
    }
    return new Pair<>(ResultType.IGNORE, context);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <O extends IProtocol, I extends IProtocol> I pipeEncode(IPContext context, O output) {
    _Logger.info("SSL pipeEncode called, outputType=%d", output._super());
    return (I) encode((SSLZContext<A>) context, output);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <O extends IProtocol, I extends IProtocol> O pipeDecode(IPContext context, I input) {
    return (O) decode((SSLZContext<A>) context, (IPacket) input);
  }
}
