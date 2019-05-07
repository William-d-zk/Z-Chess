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

package com.tgx.chess.rook.io.zoperator;

import java.util.Objects;

import com.tgx.chess.bishop.io.ws.filter.WsControlFilter;
import com.tgx.chess.bishop.io.ws.filter.WsFrameFilter;
import com.tgx.chess.bishop.io.ws.filter.WsHandShakeFilter;
import com.tgx.chess.bishop.io.zfilter.ZCommandFilter;
import com.tgx.chess.bishop.io.zfilter.ZTlsFilter;
import com.tgx.chess.bishop.io.zprotocol.ZCommandFactories;
import com.tgx.chess.bishop.io.zprotocol.ZContext;
import com.tgx.chess.king.base.inf.ITriple;
import com.tgx.chess.king.base.log.Logger;
import com.tgx.chess.king.base.util.IoUtil;
import com.tgx.chess.king.base.util.Triple;
import com.tgx.chess.queen.event.inf.ISort;
import com.tgx.chess.queen.event.operator.AioWriter;
import com.tgx.chess.queen.event.operator.CloseOperator;
import com.tgx.chess.queen.event.operator.ErrorOperator;
import com.tgx.chess.queen.event.operator.TransferOperator;
import com.tgx.chess.queen.io.core.inf.ICommand;
import com.tgx.chess.queen.io.core.inf.IFilterChain;
import com.tgx.chess.queen.io.core.inf.IPacket;
import com.tgx.chess.queen.io.core.inf.IPipeDecoder;
import com.tgx.chess.queen.io.core.inf.IPipeEncoder;
import com.tgx.chess.queen.io.core.inf.ISession;

/**
 * @author william.d.zk
 */
@SuppressWarnings("unchecked")
public enum ZSort
        implements
        ISort {
	/**
	 * 
	 */
	
	CONSUMER,
	CONSUMER_SSL {
		@Override
		public boolean isSSL() {
			return true;
		}
	};
	
	@Override
	public Mode getMode() {
		return Mode.LINK;
	}
	
	@Override
	public Type getType() {
		return Type.CONSUMER;
	}
	
	private static final Logger					_Log				= Logger.getLogger(ZSort.class.getName());
	private final CloseOperator<ZContext>		_CloseOperator		= new CloseOperator<>();
	private final ErrorOperator<ZContext>		_ErrorOperator		= new ErrorOperator<>(_CloseOperator);
	private final AioWriter<ZContext>			_AioWriter			= new AioWriter<>();
	private final IPipeEncoder<ZContext>		_ConsumerEncoder	= new IPipeEncoder<ZContext>()
																	{
																		final WsHandShakeFilter<ZContext> handshakeFilter = new WsHandShakeFilter<>(ZSort.this);
																		
																		{
																			IFilterChain<ZContext> header = new ZTlsFilter<>();
																			handshakeFilter.linkAfter(header);
																			handshakeFilter.linkFront(new WsFrameFilter<>())
																			               .linkFront(new ZCommandFilter<>(ZCommandFactories.CONSUMER))
																			               .linkFront(new WsControlFilter<>());
																		}
																		
																		@Override
																		public ITriple handle(ICommand command,
																		                      ISession<ZContext> session) {
																			try {
																				IPacket send = (IPacket) filterWrite(command,
																				                                     handshakeFilter,
																				                                     session.getContext());
																				Objects.requireNonNull(send);
																				_Log.info("ConsumerEncoder send:%s",
																				          IoUtil.bin2Hex(send.getBuffer()
																				                             .array(),
																				                         "."));
																				session.write(send,
																				              _AioWriter);
																			}
																			catch (Exception e) {
																				return new Triple<>(e,
																				                    session,
																				                    _ErrorOperator);
																			}
																			return null;
																		}
																		
																		@Override
																		public String toString() {
																			return "_ConsumerEncoder";
																		}
																	};
	private final TransferOperator<ZContext>	_ConsumerTransfer	= new TransferOperator<>(_ConsumerEncoder);
	private final IPipeDecoder<ZContext>		_ConsumerDecoder	= new IPipeDecoder<ZContext>()
																	{
																		final WsHandShakeFilter handshakeFilter = new WsHandShakeFilter(ZSort.this);
																		
																		{
																			IFilterChain<ZContext> header = new ZTlsFilter<>();
																			handshakeFilter.linkAfter(header);
																			handshakeFilter.linkFront(new WsFrameFilter<>())
																			               .linkFront(new ZCommandFilter<>(ZCommandFactories.CONSUMER))
																			               .linkFront(new WsControlFilter<>());
																		}
																		
																		@Override
																		public ITriple handle(IPacket input,
																		                      ISession<ZContext> session) {
																			return new Triple<>(filterRead(input,
																			                               handshakeFilter,
																			                               session),
																			                    session,
																			                    _ConsumerTransfer);
																		}
																		
																		@Override
																		public String toString() {
																			return "_ConsumerDecoder";
																		}
																	};
	
	public CloseOperator<ZContext> getCloseOperator() {
		return _CloseOperator;
	}
	
	public IPipeDecoder<ZContext> getConsumerDecoder() {
		return _ConsumerDecoder;
	}
	
	public IPipeEncoder<ZContext> getConsumerEncoder() {
		return _ConsumerEncoder;
	}
	
	public TransferOperator<ZContext> getTransfer() {
		return _ConsumerTransfer;
	}
	
}
