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

package com.isahl.chess.bishop.protocol.zchat;

import com.isahl.chess.king.base.crypt.features.ISymmetric;
import com.isahl.chess.king.base.crypt.util.AesGcm;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.queen.io.core.features.model.channels.INetworkOption;
import com.isahl.chess.queen.io.core.features.model.session.ISort;
import com.isahl.chess.queen.io.core.features.model.session.zls.IEContext;
import com.isahl.chess.queen.io.core.features.model.session.zls.IEncryptor;

/**
 * EZContext - 加密上下文
 *
 * <p>安全更新: 已使用 AES-GCM 替代已移除的 RC4 算法
 *
 * @author Z-Chess Team
 */
public class EZContext extends ZContext implements IEContext {
  private final CryptoUtil _CryptoUtil = new CryptoUtil();

  private int mPubKeyId = -2;
  private boolean mUpdateKeyIn, mUpdateKeyOut;
  private int mSymmetricKeyId;
  private byte[] mSymmetricKeyIn, mSymmetricKeyOut, mSymmetricKeyReroll;
  // 使用 AES-GCM 替代 RC4 (安全修复)
  private AesGcm mEncryptAesGcm, mDecryptAesGcm;
  private IEncryptor mEncryptHandler;

  public EZContext(INetworkOption option, ISort.Mode mode, ISort.Type type) {
    super(option, mode, type);
  }

  /** 获取对称解密器 (使用 AES-GCM 替代已移除的 RC4) */
  @Override
  public ISymmetric getSymmetricDecrypt() {
    return mDecryptAesGcm == null ? mDecryptAesGcm = new AesGcm() : mDecryptAesGcm;
  }

  /** 获取对称加密器 (使用 AES-GCM 替代已移除的 RC4) */
  @Override
  public ISymmetric getSymmetricEncrypt() {
    return mEncryptAesGcm == null ? mEncryptAesGcm = new AesGcm() : mEncryptAesGcm;
  }

  @Override
  public int getSymmetricKeyId() {
    return mSymmetricKeyId;
  }

  @Override
  public void setSymmetricKeyId(int symmetricKeyId) {
    mSymmetricKeyId = symmetricKeyId;
  }

  @Override
  public byte[] getSymmetricKeyIn() {
    return mSymmetricKeyIn;
  }

  @Override
  public byte[] getSymmetricKeyOut() {
    return mSymmetricKeyOut;
  }

  @Override
  public byte[] getReRollKey() {
    return mSymmetricKeyReroll;
  }

  @Override
  public boolean needUpdateKeyIn() {
    if (mUpdateKeyIn) {
      mUpdateKeyIn = false;
      return true;
    }
    return false;
  }

  @Override
  public boolean needUpdateKeyOut() {
    if (mUpdateKeyOut) {
      mUpdateKeyOut = false;
      return true;
    }
    return false;
  }

  @Override
  public void promotionIn() {
    updateKeyIn();
  }

  /*
   * 处理流中仅提供信号即可，
   * 真正的操作在decode 中使用 cryptIn最终执行
   */
  @Override
  public void updateKeyIn() {
    mUpdateKeyIn = true;
  }

  /*
   * 处理流中仅提供信号即可，
   * 真正的操作在encode 中使用 cryptOut最终执行
   */
  @Override
  public void promotionOut() {
    updateKeyOut();
  }

  @Override
  public void updateKeyOut() {
    mUpdateKeyOut = true;
  }

  @Override
  public void reRollKey(byte[] key) {
    mSymmetricKeyReroll = key;
  }

  @Override
  public void swapKeyIn(byte[] key) {
    mSymmetricKeyIn = key;
  }

  @Override
  public void swapKeyOut(byte[] key) {
    mSymmetricKeyOut = key;
  }

  @Override
  public int getPubKeyId() {
    return mPubKeyId;
  }

  @Override
  public void setPubKeyId(int pubKeyId) {
    mPubKeyId = pubKeyId;
  }

  @Override
  public IEncryptor getEncryptHandler() {
    return mEncryptHandler;
  }

  @Override
  public void setEncryptHandler(IEncryptor handler) {
    mEncryptHandler = handler;
  }

  public CryptoUtil getCryptUtil() {
    return _CryptoUtil;
  }

  @Override
  public void cryptIn() {
    super.promotionIn();
  }

  @Override
  public void cryptOut() {
    super.promotionOut();
  }

  @Override
  public boolean isInCrypt() {
    return _DecodeState.get() == DECODE_PAYLOAD;
  }

  @Override
  public boolean isOutCrypt() {
    return _EncodeState.get() == ENCODE_PAYLOAD;
  }
}
