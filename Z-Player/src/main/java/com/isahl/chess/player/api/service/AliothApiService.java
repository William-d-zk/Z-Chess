package com.isahl.chess.player.api.service;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isahl.chess.player.api.config.PlayerConfig;
import com.isahl.chess.player.api.model.NocoListResponse;
import com.isahl.chess.player.api.model.NocoSingleRecordResponse;
import com.isahl.chess.player.api.model.RpaAuthDo;
import com.isahl.chess.player.api.model.RpaTaskDO;
import com.isahl.chess.player.api.model.TaskStatusDo;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * 访问nocobase api接口
 *
 * @author xiaojiang.lxj at 2024-05-10 10:53.
 */

@Service
public class AliothApiService {

    /**
     * 竞拍视图
     */
    private static final String RPA_BIDDING_VIEW_NAME = "mdv_contract_rpa_bidding_cabin";

    /**
     * 合约状态表
     */
    private static final String MD_CONTRACT_STATUS_TABLE = "zc_md_contract_status";

    /**
     * 合约与状态关系表
     */
    private static final String CONTRACT_R_STATUS = "zc_md_obj_r_status";

    /**
     * 认证信息视图
     */
    public static final String RPA_AUTH_INFO_VIEW = "rdv_contact_authority";

    private PlayerConfig playerConfig;

    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;


    public AliothApiService(RestTemplateBuilder restTemplateBuilder,PlayerConfig playerConfig,ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.playerConfig = playerConfig;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(PlayerConfig.TIMEOUT))
            .setReadTimeout(Duration.ofMillis(PlayerConfig.TIMEOUT))
            .defaultHeader(HttpHeaders.AUTHORIZATION,"Bearer "+playerConfig.getNocoApiToken())
            .build();
    }

    public void updateTask(long taskId,String status){
        TaskStatusDo statusDo = queryStatus(status);
        if(status != null){
            updateTaskStatusById(taskId,statusDo.getId());
        }
    }

    public List<RpaTaskDO> fetchUnfinishedTaskList(){
        JSONObject filterJson = new JSONObject();
        filterJson.put("task_status","履约");
        Map<String,Object> uriVariables = new HashMap<>();
        uriVariables.put("collectionName",RPA_BIDDING_VIEW_NAME);
        uriVariables.put("filters",filterJson.toString());
        uriVariables.put("page",1);
        uriVariables.put("pageSize",100);
        LinkedHashMap<String,Object> result = restTemplate.getForObject(playerConfig.getNocoApiBaseUrl()+"{collectionName}:list?filter={filters}&page={page}&pageSize={pageSize}",LinkedHashMap.class,uriVariables);
        if(result != null){
            NocoListResponse<RpaTaskDO> nocoListResponse = objectMapper.convertValue(result,
                new TypeReference<>() {
                    @Override
                    public Type getType() {
                        return super.getType();
                    }

                    @Override
                    public int compareTo(TypeReference<NocoListResponse<RpaTaskDO>> o) {
                        return super.compareTo(o);
                    }
                });
            return nocoListResponse.getData();
        }
        return Collections.emptyList();
    }


    public List<RpaAuthDo> fetchAuthInfos(){
        JSONObject filterJson = new JSONObject();
        filterJson.put("auth_config_title","msk");
        Map<String,Object> uriVariables = new HashMap<>();
        uriVariables.put("collectionName",RPA_AUTH_INFO_VIEW);
        uriVariables.put("filters",filterJson.toString());
        uriVariables.put("page",1);
        uriVariables.put("pageSize",100);
        LinkedHashMap<String,Object> result = restTemplate.getForObject(playerConfig.getNocoApiBaseUrl()+"{collectionName}:list?filter={filters}&page={page}&pageSize={pageSize}",LinkedHashMap.class,uriVariables);
        if(result != null){
            NocoListResponse<RpaAuthDo> nocoListResponse = objectMapper.convertValue(result,
                new TypeReference<>() {
                    @Override
                    public Type getType() {
                        return super.getType();
                    }

                    @Override
                    public int compareTo(TypeReference<NocoListResponse<RpaAuthDo>> o) {
                        return super.compareTo(o);
                    }
                });
            return nocoListResponse.getData();
        }
        return Collections.emptyList();
    }

    public TaskStatusDo queryStatus(String status){
        JSONObject filterJson = new JSONObject();
        filterJson.put("notice",status);
        Map<String,Object> uriVariables = new HashMap<>();
        uriVariables.put("collectionName",MD_CONTRACT_STATUS_TABLE);
        uriVariables.put("filters",filterJson.toString());
        LinkedHashMap<String,Object> result = restTemplate.getForObject(playerConfig.getNocoApiBaseUrl()+"{collectionName}:get?filter={filters}",
            LinkedHashMap.class,uriVariables);
        if(result != null){
            NocoSingleRecordResponse<TaskStatusDo> nocoSingleRecordResponse =  objectMapper.convertValue(result,
                new TypeReference<>() {
                    @Override
                    public Type getType() {
                        return super.getType();
                    }

                    @Override
                    public int compareTo(TypeReference<NocoSingleRecordResponse<TaskStatusDo>> o) {
                        return super.compareTo(o);
                    }
                });
            return nocoSingleRecordResponse.getData();
        }
        return null;
    }

    public void updateTaskStatusById(long taskId, long statusId){
        JSONObject filterJson = new JSONObject();
        filterJson.put("ref_lifecycle",taskId);
        Map<String,Object> uriVariables = new HashMap<>();
        uriVariables.put("collectionName",CONTRACT_R_STATUS);
        uriVariables.put("filters",filterJson.toString());
        MultiValueMap headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        Map<String,Object> body = new HashMap<>();
        body.put("ref_status",statusId);
        HttpEntity<Map<String,Object>> requestEntity = new HttpEntity<>(body,headers);
        restTemplate.exchange(playerConfig.getNocoApiBaseUrl()+"{collectionName}:update?filter={filters}", HttpMethod.POST,requestEntity,
            NocoSingleRecordResponse.class,uriVariables);
    }

}
