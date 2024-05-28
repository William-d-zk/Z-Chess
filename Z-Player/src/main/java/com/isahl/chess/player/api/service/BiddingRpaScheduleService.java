package com.isahl.chess.player.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.player.api.config.PlayerConfig;
import com.isahl.chess.player.api.model.BiddingRpaApiResponse;
import com.isahl.chess.player.api.model.BiddingRpaDO;
import com.isahl.chess.player.api.model.RpaAuthDo;
import com.isahl.chess.player.api.model.RpaTaskDO;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    public BiddingRpaScheduleService(
        RestTemplateBuilder restTemplateBuilder,
        ObjectMapper objectMapper,
        PlayerConfig playerConfig,
        AliothApiService aliothApiService
    ) {
        this.objectMapper = objectMapper;
        this.playerConfig = playerConfig;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(PlayerConfig.TIMEOUT))
            .setReadTimeout(Duration.ofMillis(PlayerConfig.TIMEOUT))
            .build();
        this.aliothApiService = aliothApiService;
    }

    //@Scheduled(cron = "0 */5 * * * *")
    public void queryAndBooking(){
        List<RpaTaskDO> rpaTaskDOList = aliothApiService.fetchUnfinishedTaskList();
        List<RpaAuthDo> rpaAuthDoList = aliothApiService.fetchAuthInfos();
        List<RpaAuthDo> rpaQueryAuthDos = new ArrayList<>(8);
        List<RpaAuthDo> rpaBookingAuthDos = new ArrayList<>(8);
        if(! CollectionUtils.isEmpty(rpaAuthDoList)){
            for(RpaAuthDo authDo : rpaAuthDoList){
                if("拍仓查询".equals(authDo.getAuth_config_notice()) || "拍舱查询".equals(authDo.getAuth_config_notice())){
                    rpaQueryAuthDos.add(authDo);
                }else if("订舱".equals(authDo.getAuth_config_notice())){
                    rpaBookingAuthDos.add(authDo);
                }
            }
        }
        if(CollectionUtils.isEmpty(rpaQueryAuthDos) || CollectionUtils.isEmpty(rpaBookingAuthDos)){
            log.warning("rpa auth info for query or booking is empty, please check!");
            return;
        }

        if(! CollectionUtils.isEmpty(rpaTaskDOList)){
            for(RpaTaskDO taskDO : rpaTaskDOList){
                try{
                    MultiValueMap<String,String> headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    // TODO 这里后续可以根据多账户进行任务分配
                    RpaAuthDo queryAuth = rpaQueryAuthDos.get(0);
                    BiddingRpaDO biddingRpaDO = new BiddingRpaDO();
                    biddingRpaDO.setNickname(queryAuth.getAuth_username());
                    biddingRpaDO.setPassword(queryAuth.getAuth_password());
                    if("msk".equals(taskDO.getPickup_place_alias_title()) && ! ObjectUtils.isEmpty(taskDO.getPickup_place_alias())){
                        biddingRpaDO.setSendingAddress(taskDO.getPickup_place_alias());
                    }else{
                        biddingRpaDO.setSendingAddress(taskDO.getPickup_place());
                    }
                    if("msk".equals(taskDO.getDestination_place_alias_title()) && ! ObjectUtils.isEmpty(taskDO.getDestination_place_alias())){
                        biddingRpaDO.setDeliveryAddress(taskDO.getDestination_place_alias());
                    }else{
                        biddingRpaDO.setDeliveryAddress(taskDO.getDestination_place());
                    }
                    biddingRpaDO.setProductName(taskDO.getCargo_type());
                    biddingRpaDO.setBox(taskDO.getContainer_type());
                    biddingRpaDO.setBoxNum(taskDO.getCabin_amount());
                    biddingRpaDO.setWeight(taskDO.getCargo_weight());
                    biddingRpaDO.setDate(taskDO.getDepart_date());
                    biddingRpaDO.setStartDate(taskDO.getPickup_date());
                    biddingRpaDO.setMaxPrice(taskDO.getMaxprice());
                    biddingRpaDO.setTaskId(taskDO.getTask_id());
                    // TODO debug时订舱账户密码为空即可
                    biddingRpaDO.setOrderNickname("");
                    biddingRpaDO.setOrderPassword("");
                    HttpEntity<BiddingRpaDO> requestEntity = new HttpEntity<>(biddingRpaDO, headers);
                    LinkedHashMap<String,Object> result = restTemplate.postForObject(playerConfig.getBiddingRpaApiUrl(),requestEntity,
                        LinkedHashMap.class);
                    if(result != null){
                        BiddingRpaApiResponse biddingRpaApiResponse = objectMapper.convertValue(result,
                            new TypeReference<>() {
                                @Override
                                public Type getType() {
                                    return super.getType();
                                }

                                @Override
                                public int compareTo(TypeReference<BiddingRpaApiResponse> o) {
                                    return super.compareTo(o);
                                }
                            });
                        log.info("调用查询及竞拍api结果: "+biddingRpaApiResponse);
                    }
                }catch (Throwable t){
                    log.fetal("执行查询并订舱任务遇到异常, task = "+taskDO.toString(),t);
                }
            }
        }
    }
}
