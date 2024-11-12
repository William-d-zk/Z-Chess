/*
 * MIT License
 *
 * Copyright (c) 2016~2022. Z-Chess
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

package com.isahl.chess.pawn.endpoint.device.resource.model;

import static jakarta.persistence.TemporalType.TIMESTAMP;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import jakarta.persistence.Temporal;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DeviceProfile
        implements Serializable
{
    @Serial
    private static final long serialVersionUID = -3059633247602550952L;

    private String wifiMac;
    private String ethernetMac;
    private String bluetoothMac;
    private String imei;
    private String imsi;
    private KeyPairProfile keyPairProfile;
    private ExpirationProfile expirationProfile;

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class ExpirationProfile implements Serializable {
        @Serial
        private static final long serialVersionUID = 4453066710608521455L;
        /**
         * 首次激活时间
         */
        private LocalDateTime activationAt;

        /**
         * 失效时间
         */
        private LocalDateTime expireAt;

        /**
         * 最近一次刷新有效期
         */
        private LocalDateTime lastRenewAt;


        public ExpirationProfile() {
        }

        public ExpirationProfile(LocalDateTime activationDateTime) {
            this.activationAt = activationDateTime;
        }

        public ExpirationProfile(LocalDateTime activationDateTime, LocalDateTime expireAt) {
            this.activationAt = activationDateTime;
            this.expireAt = expireAt;
        }

        public ExpirationProfile(LocalDateTime activationDateTime, LocalDateTime expireAt, LocalDateTime lastRenewAt) {
            this.activationAt = activationDateTime;
            this.expireAt = expireAt;
            this.lastRenewAt = lastRenewAt;
        }

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @Temporal(TIMESTAMP)
        public LocalDateTime getActivationAt() {
            return activationAt;
        }

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        public void setActivationAt(LocalDateTime activationAt) {
            this.activationAt = activationAt;
        }

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @Temporal(TIMESTAMP)
        public LocalDateTime getExpireAt() {
            return expireAt;
        }

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        public void setExpireAt(LocalDateTime expireAt) {
            this.expireAt = expireAt;
        }

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonDeserialize(using = LocalDateTimeDeserializer.class)
        @Temporal(TIMESTAMP)
        public LocalDateTime getLastRenewAt() {
            return lastRenewAt;
        }

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        public void setLastRenewAt(LocalDateTime lastRenewAt) {
            this.lastRenewAt = lastRenewAt;
        }

        @Override
        public String toString() {
            return "ExpirationProfile{" +
                "activationAt=" + activationAt +
                ", expireAt=" + expireAt +
                ", lastRenewAt=" + lastRenewAt +
                '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class KeyPairProfile implements Serializable {
        @Serial
        private static final long serialVersionUID = -413771969356174480L;

        private String keyPairAlgorithm;
        private String publicKey;
        private String privateKey;

        public KeyPairProfile() {
        }

        public KeyPairProfile(String keyPairAlgorithm, String publicKey, String privateKey) {
            this.keyPairAlgorithm = keyPairAlgorithm;
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }

        public String getKeyPairAlgorithm() {
            return keyPairAlgorithm;
        }

        public void setKeyPairAlgorithm(String keyPairAlgorithm) {
            this.keyPairAlgorithm = keyPairAlgorithm;
        }

        public String getPublicKey() {
            return publicKey;
        }

        public void setPublicKey(String publicKey) {
            this.publicKey = publicKey;
        }

        public String getPrivateKey() {
            return privateKey;
        }

        public void setPrivateKey(String privateKey) {
            this.privateKey = privateKey;
        }

        @Override
        public String toString() {
            return "KeyPairProfile{" +
                "keyPairAlgorithm='" + keyPairAlgorithm + '\'' +
                ", publicKey='" + publicKey + '\'' +
                ", privateKey='" + privateKey + '\'' +
                '}';
        }
    }

    public String getWifiMac()
    {
        return wifiMac;
    }

    public void setWifiMac(String wifiMac)
    {
        this.wifiMac = wifiMac;
    }

    public String getEthernetMac()
    {
        return ethernetMac;
    }

    public void setEthernetMac(String ethernetMac)
    {
        this.ethernetMac = ethernetMac;
    }

    public String getBluetoothMac()
    {
        return bluetoothMac;
    }

    public void setBluetoothMac(String bluetoothMac)
    {
        this.bluetoothMac = bluetoothMac;
    }

    public String getImei()
    {
        return imei;
    }

    public void setImei(String imei)
    {
        this.imei = imei;
    }

    public String getImsi()
    {
        return imsi;
    }

    public void setImsi(String imsi)
    {
        this.imsi = imsi;
    }

    public KeyPairProfile getKeyPairProfile() {
        return keyPairProfile;
    }

    public void setKeyPairProfile(
        KeyPairProfile keyPairProfile) {
        this.keyPairProfile = keyPairProfile;
    }

    public ExpirationProfile getExpirationProfile() {
        return expirationProfile;
    }

    public void setExpirationProfile(
        ExpirationProfile expirationProfile) {
        this.expirationProfile = expirationProfile;
    }

    @Override
    public String toString() {
        return "DeviceProfile{" +
            "wifiMac='" + wifiMac + '\'' +
            ", ethernetMac='" + ethernetMac + '\'' +
            ", bluetoothMac='" + bluetoothMac + '\'' +
            ", imei='" + imei + '\'' +
            ", imsi='" + imsi + '\'' +
            ", keyPairProfile=" + keyPairProfile +
            '}';
    }
}
