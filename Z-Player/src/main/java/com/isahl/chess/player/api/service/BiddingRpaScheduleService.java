package com.isahl.chess.player.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.player.api.config.PlayerConfig;
import com.isahl.chess.player.api.model.BiddingRpaApiResponse;
import com.isahl.chess.player.api.model.BiddingRpaDO;
import com.isahl.chess.player.api.model.RpaAuthDo;
import com.isahl.chess.player.api.model.RpaTaskDO;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.RestTemplate;

/**
 * @author xiaojiang.lxj at 2024-05-14 09:59.
 */
@Service
public class BiddingRpaScheduleService {

    private final Logger log = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;

    private PlayerConfig playerConfig;

    private AliothApiService aliothApiService;

    private Random random = new Random();


    public BiddingRpaScheduleService(
        RestTemplateBuilder restTemplateBuilder,
        ObjectMapper objectMapper,
        PlayerConfig playerConfig,
        AliothApiService aliothApiService) {
        this.objectMapper = objectMapper;
        this.playerConfig = playerConfig;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(PlayerConfig.TIMEOUT))
            .setReadTimeout(Duration.ofMillis(PlayerConfig.TIMEOUT))
            .build();
        this.aliothApiService = aliothApiService;
    }

    /**
     * 查询并订舱
     * @param taskId : 需要处理的任务id，为空则表示处理所有履约状态的任务
     */
    public void queryAndBooking(Long taskId) {
        List<RpaTaskDO> rpaTaskDOList;
        if(taskId != null){
            rpaTaskDOList = aliothApiService.fetchSpecificTask(taskId);
        }else{
            rpaTaskDOList = aliothApiService.fetchUnfinishedTaskList();
        }
        if (CollectionUtils.isEmpty(rpaTaskDOList)) {
            log.info("有效订舱任务列表为空，skipping");
            return;
        }

        List<RpaAuthDo> rpaAuthDoList = aliothApiService.fetchAuthInfos();
        List<RpaAuthDo> rpaQueryAuthDos = new ArrayList<>(8);
        List<RpaAuthDo> rpaBookingAuthDos = new ArrayList<>(8);
        if (!CollectionUtils.isEmpty(rpaAuthDoList)) {
            for (RpaAuthDo authDo : rpaAuthDoList) {
                if ("拍仓查询".equals(authDo.getAuth_config_notice()) || "拍舱查询".equals(authDo.getAuth_config_notice())) {
                    rpaQueryAuthDos.add(authDo);
                } else if ("订舱".equals(authDo.getAuth_config_notice())) {
                    rpaBookingAuthDos.add(authDo);
                }
            }
        }
        if (CollectionUtils.isEmpty(rpaQueryAuthDos) || CollectionUtils.isEmpty(rpaBookingAuthDos)) {
            log.warning("rpa auth info for query or booking is empty, please check!");
            return;
        }

        for (RpaTaskDO taskDO : rpaTaskDOList) {
            try {
                Thread.sleep(30000);
                MultiValueMap<String, String> headers = new HttpHeaders();
                headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                // fixme 这里后续可以根据多账户进行任务分配，目前采取随机选择方式
                RpaAuthDo queryAuth = rpaQueryAuthDos.get(random.nextInt(rpaQueryAuthDos.size()));
                BiddingRpaDO biddingRpaDO = new BiddingRpaDO();
                biddingRpaDO.setNickname(queryAuth.getAuth_username());
                biddingRpaDO.setPassword(queryAuth.getAuth_password());
                String sendingAddress;
                if ("msk".equals(taskDO.getPickup_place_alias_title()) && !ObjectUtils.isEmpty(
                    taskDO.getPickup_place_alias())) {
                    sendingAddress = taskDO.getPickup_place_alias();
                } else {
                    sendingAddress = taskDO.getPickup_place();
                }
                biddingRpaDO.setSendingAddress(sendingAddress);
                String deliveryAddress;
                if ("msk".equals(taskDO.getDestination_place_alias_title()) && !ObjectUtils.isEmpty(
                    taskDO.getDestination_place_alias())) {
                    deliveryAddress = taskDO.getDestination_place_alias();
                } else {
                    deliveryAddress = taskDO.getDestination_place();
                }
                biddingRpaDO.setDeliveryAddress(deliveryAddress);
                biddingRpaDO.setProductName(taskDO.getCargo_type());
                biddingRpaDO.setBox(taskDO.getContainer_type());
                biddingRpaDO.setBoxNum(taskDO.getCabin_amount());
                biddingRpaDO.setWeight(taskDO.getCargo_weight());
                biddingRpaDO.setDate(taskDO.getDepart_date());
                biddingRpaDO.setStartDate(taskDO.getPickup_date());
                biddingRpaDO.setMaxPrice(taskDO.getMaxprice());
                biddingRpaDO.setTaskId(taskDO.getTask_id());
                RpaAuthDo bookingAuth = rpaBookingAuthDos.get(random.nextInt(rpaBookingAuthDos.size()));
                if (playerConfig.getDisableBooking()) {
                    // 关闭订舱操作,订舱账户密码为空即可
                    biddingRpaDO.setOrderNickname("");
                    biddingRpaDO.setOrderPassword("");
                } else {
                    biddingRpaDO.setOrderNickname(bookingAuth.getAuth_username());
                    biddingRpaDO.setOrderPassword(bookingAuth.getAuth_password());
                }
                HttpEntity<BiddingRpaDO> requestEntity = new HttpEntity<>(biddingRpaDO, headers);
                LinkedHashMap<String, Object> result = restTemplate.postForObject(playerConfig.getBiddingRpaApiUrl(),
                    requestEntity,
                    LinkedHashMap.class);
                if (result != null) {
                    BiddingRpaApiResponse biddingRpaApiResponse = objectMapper.convertValue(result,
                        BiddingRpaApiResponse.class);
                    log.info("调用查询及竞拍api结果: " + biddingRpaApiResponse);
                }
            } catch (Throwable t) {
                log.fetal("执行查询并订舱任务遇到异常, task = " + taskDO.toString(), t);
            }
        }
    }
}
