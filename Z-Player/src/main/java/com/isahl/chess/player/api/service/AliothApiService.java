package com.isahl.chess.player.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isahl.chess.player.api.config.PlayerConfig;
import com.isahl.chess.player.api.model.NocoListResponse;
import com.isahl.chess.player.api.model.NocoListResponse.ListResponseMeta;
import com.isahl.chess.player.api.model.NocoSingleRecordResponse;
import com.isahl.chess.player.api.model.RpaAuthDo;
import com.isahl.chess.player.api.model.RpaTaskDO;
import com.isahl.chess.player.api.model.TaskStatusDo;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
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

    public AliothApiService(RestTemplateBuilder restTemplateBuilder,
        PlayerConfig playerConfig,
        ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.playerConfig = playerConfig;
        this.restTemplate = restTemplateBuilder.setConnectTimeout(Duration.ofMillis(PlayerConfig.TIMEOUT))
            .setReadTimeout(Duration.ofMillis(PlayerConfig.TIMEOUT))
            .defaultHeader(HttpHeaders.AUTHORIZATION,
                "Bearer " + playerConfig.getNocoApiToken())
            .build();
    }

    public void updateTask(long taskId, String status) {
        TaskStatusDo statusDo = queryStatus(status);
        if (status != null) {
            updateTaskStatusById(taskId, statusDo.getId());
        }
    }

    public List<RpaTaskDO> fetchUnfinishedTaskList() {
        String filters = "{\"task_status\" : \"履约\"}";
        NocoListResponse<RpaTaskDO> listResponse = listCollectionItems(RPA_BIDDING_VIEW_NAME,filters,1,100);
        if(listResponse == null){
            return Collections.emptyList();
        }else{
            List<RpaTaskDO> mergeList =  listResponse.getData();
            ListResponseMeta meta = listResponse.getMeta();
            int currentPage = meta.getPage();
            int pageSize = meta.getPageSize();
            while(currentPage < meta.getTotalPage() ){
                listResponse = listCollectionItems(RPA_BIDDING_VIEW_NAME,filters,++currentPage,pageSize);
                mergeList.addAll(listResponse != null && listResponse.getData()!= null ? listResponse.getData() : Collections.emptyList());
            }
            return mergeList;
        }
    }

    public List<RpaAuthDo> fetchAuthInfos() {
        String filters = "{\"auth_config_title\" : \"msk\"}";
        NocoListResponse<RpaAuthDo> listResponse = listCollectionItems(RPA_AUTH_INFO_VIEW,filters,1,100);
        if(listResponse == null){
            return Collections.emptyList();
        }else{
            List<RpaAuthDo> mergeList =  listResponse.getData();
            ListResponseMeta meta = listResponse.getMeta();
            int currentPage = meta.getPage();
            int pageSize = meta.getPageSize();
            while(currentPage < meta.getTotalPage() ){
                listResponse = listCollectionItems(RPA_AUTH_INFO_VIEW,filters,++currentPage,pageSize);
                mergeList.addAll(listResponse != null && listResponse.getData()!= null ? listResponse.getData() : Collections.emptyList());
            }
            return mergeList;
        }
    }

    public TaskStatusDo queryStatus(String status) {
        String filters = "{\"notice\" : \"" + status + "\"}";
        NocoSingleRecordResponse<TaskStatusDo> singleRecordResponse = getCollectionItem(MD_CONTRACT_STATUS_TABLE,filters);
        if(singleRecordResponse != null){
            return singleRecordResponse.getData();
        }
        return null;
    }

    public void updateTaskStatusById(long taskId, long statusId) {
        String filters = "{\"ref_lifecycle\":" + taskId + "}";
        Map<String, Object> updates = new HashMap<>();
        updates.put("ref_status", statusId);
        updateCollectionItem(CONTRACT_R_STATUS,filters,updates);
    }


    private void updateCollectionItem(String collectionName,String jsonFilters,Map<String,Object> updatedFieldValue){
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("collectionName", collectionName);
        uriVariables.put("filters", jsonFilters);
        MultiValueMap headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(updatedFieldValue, headers);
        restTemplate.exchange(playerConfig.getNocoApiBaseUrl() + "{collectionName}:update?filter={filters}",
            HttpMethod.POST,
            requestEntity,
            HashMap.class,
            uriVariables);
    }


    private <T> NocoSingleRecordResponse<T> getCollectionItem(String collectionName,String jsonFilters){
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("collectionName", collectionName);
        uriVariables.put("filters", jsonFilters);
        HashMap<String, Object> result = restTemplate.getForObject(
            playerConfig.getNocoApiBaseUrl() + "{collectionName}:get?filter={filters}",
            HashMap.class,
            uriVariables);
        if (result != null) {
            return objectMapper.convertValue(result,
                new TypeReference<>() {
                    @Override
                    public Type getType() {
                        return super.getType();
                    }

                    @Override
                    public int compareTo(
                        TypeReference<NocoSingleRecordResponse<T>> o) {
                        return super.compareTo(
                            o);
                    }
                });
        }
        return null;
    }

    private <T> NocoListResponse<T> listCollectionItems(String collectionName,String jsonFilters, int page,int pageSize){
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("collectionName", collectionName);
        uriVariables.put("filters", jsonFilters);
        uriVariables.put("page", page);
        uriVariables.put("pageSize", pageSize);
        HashMap<String, Object> result = restTemplate.getForObject(playerConfig.getNocoApiBaseUrl() +
                "{collectionName}:list?filter={filters}&page={page}&pageSize={pageSize}",
            HashMap.class,
            uriVariables);
        if (result != null) {
            return objectMapper.convertValue(result, new TypeReference<>() {
                @Override
                public Type getType() {
                    return super.getType();
                }

                @Override
                public int compareTo(TypeReference<NocoListResponse<T>> o) {
                    return super.compareTo(o);
                }
            });
        }
        return null;
    }

}
