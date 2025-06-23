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

package com.isahl.chess.player.api.controller;

import static com.isahl.chess.king.base.util.IoUtil.isBlank;

import com.isahl.chess.king.base.content.ZResponse;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.CryptoUtil;
import com.isahl.chess.king.base.util.CryptoUtil.ASymmetricKeyPair;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.king.config.CodeKing;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.resource.model.DeviceProfile;
import com.isahl.chess.pawn.endpoint.device.resource.model.DeviceProfile.ExpirationProfile;
import com.isahl.chess.pawn.endpoint.device.resource.model.DeviceProfile.KeyPairProfile;
import com.isahl.chess.player.api.model.DeviceDo;
import com.isahl.chess.player.api.service.AliothApiService;
import com.isahl.chess.player.api.service.MixOpenService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author william.d.zk {@code @date} 2019/05/01
 */
@RestController
@RequestMapping("device")
public class DeviceController {

    private final Logger _Logger = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private final MixOpenService _MixOpenService;

    private final AliothApiService aliothApiService;

    @Autowired
    public DeviceController(
        MixOpenService mixOpenService,
        AliothApiService aliothApiService
        ) {
        _MixOpenService = mixOpenService;
        this.aliothApiService = aliothApiService;
    }


    /**
     * 仓娲项目定制，出厂初始化
     * 
     * docker版本初始化会将初始化及验证过程合并，额外返回验证接口的信息
     * 
     * @param docker: 是否为docker版本初始化
     */
    @PostMapping("init")
    public ZResponse<?> initDevice(
        @RequestParam(name = "docker",required = false,defaultValue = "false") Boolean docker,
        @RequestBody DeviceDo deviceDo
    ) {
        if (ObjectUtils.isEmpty(deviceDo.getProfile().getWifiMac()) || ObjectUtils.isEmpty(
            deviceDo.getProfile().getEthernetMac()) || ObjectUtils.isEmpty(deviceDo.getProfile().getBluetoothMac())) {
            return ZResponse.error(CodeKing.ERROR.getCode(), "wifi/ethernet/bluetooth 硬件地址不能为空");
        }
        String serialNo = CryptoUtil.MD5(
            deviceDo.getProfile().getEthernetMac() + "|" + deviceDo.getProfile().getWifiMac() + "|"
                + deviceDo.getProfile().getBluetoothMac());
        // 查询是否已经注册过该设备
        DeviceEntity existDevice = _MixOpenService.findByNumber(serialNo);
        if(existDevice != null){
            // 已经注册过设备，返回算法名称及公钥等信息
            Map<String, String> data = new HashMap<>();
            data.put("token", existDevice.getToken());
            data.put("serial", serialNo);
            data.put("algorithm", existDevice.getProfile().getKeyPairProfile().getKeyPairAlgorithm());
            data.put("publicKey", existDevice.getProfile().getKeyPairProfile().getPublicKey());
            return ZResponse.success(data);
        }
        deviceDo.setNumber(serialNo);
        // 这里使用256位密钥长度，是因为后续需要签名及校验，192位长度曲线签名校验不支持
        ASymmetricKeyPair keyPair = CryptoUtil.generateEccKeyPair(256);
        if (ObjectUtils.isEmpty(keyPair)) {
            return ZResponse.error(CodeKing.ERROR.getCode(), "服务器内部错误");
        }

        KeyPairProfile keyPairProfile = new KeyPairProfile(keyPair.getAlgorithm(), keyPair.getPublicKey(),
            keyPair.getPrivateKey());
        deviceDo.getProfile().setKeyPairProfile(keyPairProfile);
        DeviceEntity deviceEntity = new DeviceEntity();
        deviceEntity.setVNotice(deviceDo.getNumber());
        deviceEntity.setUsername(deviceDo.getUsername());
        deviceEntity.setCreatedById(deviceDo.getUid());
        deviceEntity.setUpdatedById(deviceDo.getUid());
        deviceEntity.setUpdatedAt(LocalDateTime.now());
        deviceEntity.setCreatedAt(LocalDateTime.now());
        deviceEntity.setCode(deviceDo.getName());
        if(docker){
            LocalDateTime activationAt = LocalDateTime.now();
            LocalDate expirationAt = activationAt.plusYears(1).plusMonths(1).toLocalDate();
            ExpirationProfile expirationProfile = new ExpirationProfile(activationAt, LocalDateTime.of(expirationAt,LocalTime.now()));
            deviceDo.getProfile().setExpirationProfile(expirationProfile);
        }
        deviceEntity.setProfile(deviceDo.getProfile());

        _Logger.info("设备初始化信息:\n" + deviceDo);
        DeviceEntity entity = _MixOpenService.newDevice(deviceEntity);
        // 设备初始化成功，返回算法名称及公钥
        Map<String, String> data = new HashMap<>();
        data.put("token", entity.getToken());
        data.put("serial", serialNo);
        data.put("algorithm", keyPair.getAlgorithm());
        data.put("publicKey", keyPair.getPublicKey());
        if(docker){
            LocalDateTime expireDate = entity.getProfile().getExpirationProfile().getExpireAt();
            String expireDateString = expireDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String expireInfo =
                    entity.getNotice() + "|" + expireDateString + "|" + entity.getProfile().getKeyPairProfile().getPublicKey();
            // 返回经过md5的有效期信息(serial|date|publicKey)
            data.put("expire_info", CryptoUtil.MD5(expireInfo).toLowerCase());
            data.put("expire_date",expireDateString);
        }
        return ZResponse.success(data);
    }

    /**
     * 仓娲项目定制，用户首次验证
     *
     * 添加 expireAt 参数是为了运维方便，客户端使用该接口是不带这个参数的
     *
     * @param token : 设备令牌
     * @param expireAt : e.g. 2024-12-25
     * @param body : 用ecc公钥加密过的数据
     * @return 返回 md5(serial|expireDate|publicKey)
     */
    @PostMapping("verify")
    public ZResponse<?> verifyDevice(
        @RequestParam(name = "token") String token,
        @RequestParam(name = "expireAt",required = false) String expireAt,
        @RequestBody String body
    ) {
        if (!isBlank(token)) {
            DeviceEntity exist = _MixOpenService.findByToken(token);
            if (exist != null) {
                DeviceProfile existedDeviceProfile = exist.getProfile();
                String decryptedBody = CryptoUtil.eccDecrypt(existedDeviceProfile.getKeyPairProfile().getPrivateKey(),
                    body);
                DeviceProfile requestDeviceProfile = JsonUtil.readValue(decryptedBody, DeviceProfile.class);
                if (ObjectUtils.isEmpty(requestDeviceProfile)) {
                    return ZResponse.error(CodeKing.ERROR.getCode(), "设备档案验证失败");
                }
                if (ObjectUtils.nullSafeEquals(requestDeviceProfile.getEthernetMac(),
                    existedDeviceProfile.getEthernetMac()) && ObjectUtils.nullSafeEquals(
                    requestDeviceProfile.getWifiMac(), existedDeviceProfile.getWifiMac()) && ObjectUtils.nullSafeEquals(
                    requestDeviceProfile.getBluetoothMac(), existedDeviceProfile.getBluetoothMac())) {
                    // 更新激活日期/失效日期
                    LocalDate expirationAt = null;
                    if(StringUtils.hasLength(expireAt)){
                        try{
                            expirationAt = LocalDate.parse(expireAt,DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        }catch (Exception e){
                            _Logger.fetal("convert expiration encounter exception.",e);
                            return ZResponse.error(CodeKing.ERROR.getCode(), "失效日期转换异常yyyy-MM-dd");
                        }
                    }
                    if (existedDeviceProfile.getExpirationProfile() == null || existedDeviceProfile.getExpirationProfile().getActivationAt() == null) {
                        LocalDateTime activationAt = LocalDateTime.now();
                        if(expirationAt == null){
                            expirationAt = activationAt.plusYears(1).plusMonths(1).toLocalDate();
                        }
                        ExpirationProfile expirationProfile = new ExpirationProfile(activationAt, LocalDateTime.of(expirationAt,LocalTime.now()));
                        existedDeviceProfile.setExpirationProfile(expirationProfile);
                        _MixOpenService.updateDevice(exist);
                    }else{
                        if(expirationAt != null){
                            existedDeviceProfile.getExpirationProfile().setExpireAt(LocalDateTime.of(expirationAt,LocalTime.now()));
                            _MixOpenService.updateDevice(exist);
                        }
                    }

                    Map<String, String> data = new HashMap<>();
                    LocalDateTime expireDate = existedDeviceProfile.getExpirationProfile().getExpireAt();
                    String expireDateString = expireDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    String expireInfo =
                        exist.getNotice() + "|" + expireDateString + "|" + existedDeviceProfile.getKeyPairProfile().getPublicKey();
                    _Logger.info("token = " + token + " , expireInfo = " + expireInfo);
                    // 返回经过md5的有效期信息(serial|date|publicKey)
                    data.put("expire_info", CryptoUtil.MD5(expireInfo).toLowerCase());
                    data.put("expire_date",expireDateString);
                    return ZResponse.success(data);
                }
            }
        }
        return ZResponse.error(CodeKing.ERROR.getCode(), "设备档案验证失败");
    }

    /**
     * 仓娲项目定制，用户首次验证激活v2版本
     * 软件有效期(默认有效期1年外加一个月)
     *
     * 添加 expireAt 参数是为了运维方便，客户端使用该接口是不带这个参数的
     *
     * @param expireAt : e.g. 2024-12-25
     * @param token : 设备令牌
     * @param body : 用ecc公钥md5的数据 md5(ethernetMac|wifiMac|bluetoothMac|publicKey)
     * @return 返回 md5(serial|expireDate|publicKey)
     */
    @PostMapping("verify-v2")
    public ZResponse<?> verifyDeviceV2(
        @RequestParam(name = "token") String token,
        @RequestParam(name = "expireAt",required = false) String expireAt,
        @RequestBody String body
    ) {
        if (!isBlank(token)) {
            DeviceEntity exist = _MixOpenService.findByToken(token);
            if (exist != null) {
                DeviceProfile existedDeviceProfile = exist.getProfile();
                String existedMd5 = CryptoUtil.MD5(
                    existedDeviceProfile.getEthernetMac() + "|" + existedDeviceProfile.getWifiMac() + "|"
                        + existedDeviceProfile.getBluetoothMac() + "|" + existedDeviceProfile.getKeyPairProfile()
                        .getPublicKey());
                if (!ObjectUtils.nullSafeEquals(existedMd5, body) && !ObjectUtils.nullSafeEquals(body,
                    existedMd5.toLowerCase())) {
                    return ZResponse.error(CodeKing.ERROR.getCode(), "设备档案验证失败，激活信息与初始注册信息不匹配。");
                }

                // 更新激活日期/失效日期
                LocalDate expirationAt = null;
                if(StringUtils.hasLength(expireAt)){
                    try{
                        expirationAt = LocalDate.parse(expireAt,DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }catch (Exception e){
                        _Logger.fetal("convert expiration encounter exception.",e);
                        return ZResponse.error(CodeKing.ERROR.getCode(), "失效日期转换异常yyyy-MM-dd");
                    }
                }
                if (existedDeviceProfile.getExpirationProfile() == null || existedDeviceProfile.getExpirationProfile().getActivationAt() == null) {
                    LocalDateTime activationAt = LocalDateTime.now();
                    if(expirationAt == null){
                        expirationAt = activationAt.plusYears(1).plusMonths(1).toLocalDate();
                    }
                    ExpirationProfile expirationProfile = new ExpirationProfile(activationAt, LocalDateTime.of(expirationAt,LocalTime.now()));
                    existedDeviceProfile.setExpirationProfile(expirationProfile);
                    _MixOpenService.updateDevice(exist);
                }else{
                    if(expirationAt != null){
                        existedDeviceProfile.getExpirationProfile().setExpireAt(LocalDateTime.of(expirationAt,LocalTime.now()));
                        _MixOpenService.updateDevice(exist);
                    }
                }
                Map<String, String> data = new HashMap<>();
                LocalDateTime expireDate = existedDeviceProfile.getExpirationProfile().getExpireAt();
                String expireDateString = expireDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                String expireInfo =
                    exist.getNotice() + "|" + expireDateString + "|" + existedDeviceProfile.getKeyPairProfile().getPublicKey();
                _Logger.info("token = " + token + " , expireInfo = " + expireInfo);
                // 返回经过md5的有效期信息(serial|date|publicKey)
                data.put("expire_info", CryptoUtil.MD5(expireInfo).toLowerCase());
                data.put("expire_date",expireDateString);
                return ZResponse.success(data);
            }
        }
        return ZResponse.error(CodeKing.ERROR.getCode(), "设备档案验证失败，不存在该设备");
    }

    /**
     * 仓娲项目定制接口，更新授权有效期 <br>
     *
     * @param serial : 序列号
     * @param expireAt: 失效日期 yyyy-MM-dd
     * @return
     */
    @GetMapping("renew")
    public ZResponse<?> renew(
        @RequestParam(name = "serial") String serial,
        @RequestParam(name = "expireAt") String expireAt
        ){
        if (!isBlank(serial)) {
            DeviceEntity exist = _MixOpenService.findByNumber(serial);
            if (exist != null) {
                DeviceProfile existedDeviceProfile = exist.getProfile();
                if(existedDeviceProfile.getExpirationProfile() == null ){
                    return ZResponse.error(CodeKing.ERROR.getCode(), "设备失效信息不存在，请确认设备是否已激活");
                }
                // 更新失效日期
                LocalDate expirationAt;
                if(StringUtils.hasLength(expireAt)){
                    try{
                        expirationAt = LocalDate.parse(expireAt,DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }catch (Exception e){
                        _Logger.fetal("convert expiration encounter exception.",e);
                        return ZResponse.error(CodeKing.ERROR.getCode(), "失效日期转换异常yyyy-MM-dd");
                    }
                }else{
                    return ZResponse.error(CodeKing.ERROR.getCode(), "失效日期为空");
                }
                existedDeviceProfile.getExpirationProfile().setExpireAt(LocalDateTime.of(expirationAt,LocalTime.now()));
                existedDeviceProfile.getExpirationProfile().setLastRenewAt(LocalDateTime.now());
                _MixOpenService.updateDevice(exist);
                LocalDateTime expireDate = existedDeviceProfile.getExpirationProfile().getExpireAt();
                String renewInfo =
                    exist.getNotice() + "|" + expireDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                _Logger.info("serial = " + serial + " , renewInfo = " + renewInfo);
                // 返回经过私钥签名的有效期信息(serial|date)
                Map<String, String> data = new HashMap<>();
                data.put("renew_info", renewInfo);
                data.put("signature", CryptoUtil.eccSign(existedDeviceProfile.getKeyPairProfile().getPrivateKey(),renewInfo));
                return ZResponse.success(data);
            }
        }
        return ZResponse.error(CodeKing.ERROR.getCode(), "更新有效期失败，不存在该设备");
    }

    /**
     * 生成验证码
     * @param serialNo 设备序列号
     * @return
     */
    @GetMapping("gvcode")
    public ZResponse<?> generateVcode(
        @RequestParam(name = "serialNo") String serialNo
    ) {
        if(!StringUtils.hasText(serialNo)){
            return ZResponse.error(CodeKing.ERROR.getCode(), "设备序列号为空");
        }
        return ZResponse.success(aliothApiService.generateVcode(serialNo));
    }

    /**
     * 验证重置出厂初始化
     * @param serialNo 设备序列号
     * @param vcode 验证码
     * @return
     */
    @PostMapping("vreinit")
    public ZResponse<?>  validateReinit(
        @RequestParam(name = "serialNo") String serialNo,
        @RequestBody String vcode
    ) {
        if(!StringUtils.hasText(serialNo)){
            return ZResponse.error(CodeKing.ERROR.getCode(), "设备序列号为空");
        }
        if(!StringUtils.hasText(vcode)){
            return ZResponse.error(CodeKing.ERROR.getCode(), "验证码为空");
        }
        return ZResponse.success(aliothApiService.validateReinit(serialNo, vcode));
    }

    /**
     * ecc加密测试
     */
    @PostMapping("test/eccEncrypt")
    public Object eccEncrypt(
        @RequestParam(name = "publicKey") String publicKey,
        @RequestBody String data
    ) {
        return ZResponse.success(CryptoUtil.eccEncrypt(publicKey, data));
    }

    @PostMapping("test/eccDecrypt")
    public Object eccDecrypt(
        @RequestParam(name = "privateKey") String privateKey,
        @RequestBody String data
    ) {
        return ZResponse.success(CryptoUtil.eccDecrypt(privateKey, data));
    }

    @PostMapping("test/eccSign")
    public Object eccSign(
        @RequestParam(name = "privateKey") String privateKey,
        @RequestBody String data
    ) {
        return ZResponse.success(CryptoUtil.eccSign(privateKey, data));
    }

    @PostMapping("test/eccVerify")
    public Object eccSign(
        @RequestParam(name = "publicKey") String publicKey,
        @RequestParam(name = "signature") String signature,
        @RequestBody String data
    ) {
        return ZResponse.success(CryptoUtil.eccVerify(publicKey, data, signature));
    }

    @PostMapping("register")
    public @ResponseBody
    ZResponse<?> registerDevice(
        @RequestBody DeviceDo deviceDo
    ) {
        DeviceEntity deviceEntity = new DeviceEntity();
        deviceEntity.setVNotice(deviceDo.getNumber());
        deviceEntity.setUsername(deviceDo.getUsername());
        deviceEntity.setProfile(deviceDo.getProfile());
        deviceEntity.setCreatedById(deviceDo.getUid());
        deviceEntity.setUpdatedById(deviceDo.getUid());
        deviceEntity.setUpdatedAt(LocalDateTime.now());
        deviceEntity.setCreatedAt(LocalDateTime.now());
        deviceEntity.setCode(deviceDo.getName());
        return ZResponse.success(_MixOpenService.newDevice(deviceEntity));
    }

    @GetMapping("open/query")
    public @ResponseBody
    ZResponse<?> queryDeviceByToken(
        @RequestParam("token") String token
    ) {
        if (!isBlank(token)) {
            DeviceEntity exist = _MixOpenService.findByToken(token);
            if (exist != null) {
                return ZResponse.success(exist);
            }
        }
        return ZResponse.error(CodeKing.MISS.getCode(), "device miss");
    }

    @GetMapping("manager/query")
    public @ResponseBody
    ZResponse<?> queryDeviceByNumber(
        @RequestParam("number") String number) {
        if (!isBlank(number)) {
            DeviceEntity exist = _MixOpenService.findByNumber(number);
            if (exist != null) {
                return ZResponse.success(exist);
            }
        }
        return ZResponse.error(CodeKing.MISS.getCode(), "device miss");
    }

    @GetMapping("online/all")
    public @ResponseBody
    ZResponse<?> listOnlineDevices(
        @RequestParam(value = "page",
            defaultValue = "0",
            required = false)
            Integer page,
        @RequestParam(value = "size",
            defaultValue = "20",
            required = false)
            Integer size) {
        size = size < 1 ? 10 : size > 50 ? 50 : size;
        page = page < 0 ? 0 : page;
        return ZResponse.success(_MixOpenService.getOnlineDevice(PageRequest.of(page, size)));
    }

    @GetMapping("online/stored")
    public @ResponseBody
    ZResponse<?> listStored(
        @RequestParam(value = "page",
            required = false,
            defaultValue = "0")
            Integer page,
        @RequestParam(value = "size",
            defaultValue = "20",
            required = false)
            Integer size) {
        size = size < 1 ? 10 : size > 50 ? 50 : size;
        page = page < 0 ? 0 : page;
        return ZResponse.success(_MixOpenService.getStorageDevice(PageRequest.of(page, size)));
    }
}
