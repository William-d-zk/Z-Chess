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

package com.isahl.chess.queen.io.core.net.udp;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * UDP 通道接口 支持非阻塞 UDP 通信
 *
 * @author william.d.zk
 */
public interface UdpChannel extends Closeable {

  /** 获取本地绑定地址 */
  InetSocketAddress localAddress();

  /**
   * 发送到指定地址
   *
   * @param data 数据
   * @param remote 远程地址
   * @return 发送字节数
   */
  int send(byte[] data, InetSocketAddress remote);

  /**
   * 接收数据
   *
   * @return 数据包和发送者地址
   */
  UdpPacket receive();

  /**
   * 加入多播组
   *
   * @param group 多播组地址
   * @param networkInterface 网络接口
   */
  void joinGroup(InetSocketAddress group, String networkInterface);

  /**
   * 离开多播组
   *
   * @param group 多播组地址
   */
  void leaveGroup(InetSocketAddress group);

  /** 是否支持多播 */
  boolean isMulticastSupported();

  /** 获取 Socket 地址 */
  SocketAddress getSocketAddress();

  /** 是否有效 */
  boolean isValid();
}
