package com.isahl.chess.player.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.pawn.endpoint.device.db.central.model.DeviceEntity;
import com.isahl.chess.pawn.endpoint.device.resource.features.IDeviceService;
import com.isahl.chess.player.api.config.PlayerConfig;
import com.isahl.chess.player.api.model.LcApiTokenDO;
import com.isahl.chess.player.api.model.LcOrderDO;
import com.isahl.chess.player.api.model.NocoListResponse;
import com.isahl.chess.player.api.model.NocoListResponse.ListResponseMeta;
import com.isahl.chess.player.api.model.NocoSingleRecordResponse;
import com.isahl.chess.player.api.model.RpaAuthDo;
import com.isahl.chess.player.api.model.RpaTaskDO;
import com.isahl.chess.player.api.model.RpaTaskMessageDO;
import com.isahl.chess.player.api.model.TaskStatusDo;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * 访问nocobase api接口
 *
 * @author xiaojiang.lxj at 2024-05-10 10:53.
 */

@Service
public class AliothApiService {
    private final Logger log = Logger.getLogger("biz.player." + getClass().getSimpleName());

    /**
     * LC api token
     */
    private static final String LC_API_TOKEN_LIST = "zc_ex_form_lc_api_token_list";

    /**
     * 外部表单-LC-订单列表
     */
    private static final String LC_API_ORDER_LIST = "zc_ex_form_lc_order_list";

    /**
     * 竞拍视图
     */
    private static final String RPA_BIDDING_VIEW_NAME = "mdv_contract_rpa_bidding_cabin";

    /**
     * 合约状态表
     */
    private static final String MD_CONTRACT_STATUS_TABLE = "zc_ms_contract_status";

    /**
     * 合约与状态关系表
     */
    private static final String CONTRACT_R_STATUS = "zc_md_obj_r_status";

    /**
     * 主权数据-合约-舱位竞拍
     */
    private static final String FREIGHT_BIDDING = "zc_md_contract_freight-bidding";

    /**
     * 认证信息视图
     */
    private static final String RPA_AUTH_INFO_VIEW = "rdv_contact_authority";

    /**
     * 验证码
     */
    private static final String REINIT_VCODE_TABLE = "zc_id_devi-chess_vcode";

    private final PlayerConfig playerConfig;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    private final IDeviceService deviceService;

    public AliothApiService(
        RestTemplateBuilder restTemplateBuilder,
        PlayerConfig playerConfig,
        ObjectMapper objectMapper,
        IDeviceService deviceService
    ) {
        this.objectMapper = objectMapper;
        this.playerConfig = playerConfig;
        this.deviceService = deviceService;
        this.restTemplate = restTemplateBuilder
                                .connectTimeout(Duration.ofMillis(PlayerConfig.TIMEOUT))
                                .readTimeout(Duration.ofMillis(10 * PlayerConfig.TIMEOUT))
                                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + playerConfig.getNocoApiToken())
                                .build();
    }

    public void updateTask(RpaTaskMessageDO message) {
        TaskStatusDo statusDo = queryStatus(message.getStatus());
        if (message.getTaskId()!= null && statusDo != null) {
            updateTaskStatusById(message.getTaskId(), statusDo.getId());
        }
        if(message.getTaskId()!= null && message.getOrderId() != null){
            saveOrderInfo(message.getTaskId(),message.getOrderId(),message.getOrderPrice());
        }
    }

    /**
     * 获取所有还在履约状态的任务
     *
     * @return
     */
    public List<RpaTaskDO> fetchUnfinishedTaskList() {
        String filters = "{\"task_status\" : \"履约\"}";
        NocoListResponse<RpaTaskDO> listResponse = listCollectionItems(RPA_BIDDING_VIEW_NAME,filters,1,100,null,RpaTaskDO.class);
        if(listResponse == null){
            return Collections.emptyList();
        }else{
            List<RpaTaskDO> mergeList =  listResponse.getData();
            ListResponseMeta meta = listResponse.getMeta();
            int currentPage = meta.getPage();
            int pageSize = meta.getPageSize();
            while(currentPage < meta.getTotalPage() ){
                listResponse = listCollectionItems(RPA_BIDDING_VIEW_NAME,filters,++currentPage,pageSize,null,RpaTaskDO.class);
                mergeList.addAll(listResponse != null && listResponse.getData()!= null ? listResponse.getData() : Collections.emptyList());
            }
            return validateTasks(mergeList);
        }
    }

    /**
     * 获取所有LC api token
     *
     * @return
     */
    public List<LcApiTokenDO> fetchLcAppTokenList() {
        String filters = "{}";
        NocoListResponse<LcApiTokenDO> listResponse = listCollectionItems(LC_API_TOKEN_LIST,filters,1,200,null, LcApiTokenDO.class);
        if(listResponse == null){
            return Collections.emptyList();
        }else{
            List<LcApiTokenDO> mergeList =  listResponse.getData();
            ListResponseMeta meta = listResponse.getMeta();
            int currentPage = meta.getPage();
            int pageSize = meta.getPageSize();
            while(currentPage < meta.getTotalPage() ){
                listResponse = listCollectionItems(LC_API_TOKEN_LIST,filters,++currentPage,pageSize,null,LcApiTokenDO.class);
                mergeList.addAll(listResponse != null && listResponse.getData()!= null ? listResponse.getData() : Collections.emptyList());
            }
            return validateTokens(mergeList);
        }
    }

    /**
     * 过滤掉已过期掉app token
     *
     */
    private List<LcApiTokenDO> validateTokens(List<LcApiTokenDO> tokenDOList){
        if(CollectionUtils.isEmpty(tokenDOList)){
            return Collections.emptyList();
        }

        List<LcApiTokenDO> validTokenList = new ArrayList<>(tokenDOList.size());
        for(LcApiTokenDO tokenDO : tokenDOList){
            Instant current = Instant.now();
            if(tokenDO.getExpire_time().isBefore(current)){
                log.warning("Api Token 已过期，自动更新状态。token info :"+tokenDO);
            }else{
                validTokenList.add(tokenDO);
            }
        }
        return validTokenList;
    }



    /**
     * 获取指定任务
     *
     * @param taskId
     * @return
     */
    public List<RpaTaskDO> fetchSpecificTask(Long taskId) {
        String filters = "{\"task_id\" : " + taskId + "}";
        NocoListResponse<RpaTaskDO> listResponse = listCollectionItems(RPA_BIDDING_VIEW_NAME,filters,1,100,null,RpaTaskDO.class);
        if(listResponse == null){
            return Collections.emptyList();
        }else{
            return validateTasks(listResponse.getData());
        }
    }

    /**
     * 对于还在履约状态的任务，过滤掉日期不满足的任务，并将其状态变更为"止保"
     *
     * @param tasks
     * @return
     */
    private List<RpaTaskDO> validateTasks(List<RpaTaskDO> tasks){
        if(CollectionUtils.isEmpty(tasks)){
            return Collections.emptyList();
        }

        List<RpaTaskDO> validTasks = new ArrayList<>(tasks.size());
        for(RpaTaskDO task : tasks){
            if(!"履约".equals(task.getTask_status())){
                log.info("任务id="+task.getTask_id()+" 当前状态为["+task.getTask_status()+"], 非履约状态，忽略...");
                continue;
            }
            Instant current = Instant.now();
            Instant departDate = Instant.parse(task.getDepart_date()+"T23:59:59+08:00");
            if(departDate.isBefore(current)){
                log.warning("订舱任务已过期，自动更新状态。task info :"+task);
                RpaTaskMessageDO message = new RpaTaskMessageDO();
                message.setTaskId(task.getTask_id());
                message.setStatus("止保");
                updateTask(message);
            }else{
                validTasks.add(task);
            }
        }
        return validTasks;
    }

    public List<RpaAuthDo> fetchAuthInfos() {
        String filters = "{\"auth_config_title\" : \"msk\"}";
        NocoListResponse<RpaAuthDo> listResponse = listCollectionItems(RPA_AUTH_INFO_VIEW,filters,1,100,null,RpaAuthDo.class);
        if(listResponse == null){
            return Collections.emptyList();
        }else{
            List<RpaAuthDo> mergeList =  listResponse.getData();
            ListResponseMeta meta = listResponse.getMeta();
            int currentPage = meta.getPage();
            int pageSize = meta.getPageSize();
            while(currentPage < meta.getTotalPage() ){
                listResponse = listCollectionItems(RPA_AUTH_INFO_VIEW,filters,++currentPage,pageSize,null,RpaAuthDo.class);
                mergeList.addAll(listResponse != null && listResponse.getData()!= null ? listResponse.getData() : Collections.emptyList());
            }
            return mergeList;
        }
    }

    public TaskStatusDo queryStatus(String status) {
        String filters = "{\"notice\" : \"" + status + "\"}";
        NocoSingleRecordResponse<TaskStatusDo> singleRecordResponse = getCollectionItem(MD_CONTRACT_STATUS_TABLE,filters,TaskStatusDo.class);
        if(singleRecordResponse != null){
            return singleRecordResponse.getData();
        }
        return null;
    }

    /**
     * 生成验证码
     * @param serialNo
     */
    public boolean generateVcode(String serialNo){
        if(!StringUtils.hasText(serialNo)){
            return false;
        }
        // 查询最新验证码是否仍然有效，如果有效(创建时间起5分钟内)，则继续使用
        String filters = "{\"serialNo\" : \"" + serialNo + "\",\"valid\" : " + true + "}";
        NocoListResponse<HashMap> listResponse = listCollectionItems(REINIT_VCODE_TABLE,filters,1,1,"-createdAt",HashMap.class);
        if(listResponse != null && !CollectionUtils.isEmpty(listResponse.getData())){
            HashMap<String,Object> record = listResponse.getData().get(0);
            Instant createdAt = Instant.parse(String.valueOf(record.get("createdAt")));
            Instant expireAt = createdAt.plus(5,ChronoUnit.MINUTES);
            if(Instant.now().compareTo(expireAt) <= 0){
                // 不超过5分钟，直接返回
                return true;
            }
        }
        // 新创建验证码
        Map<String,Object> body = new HashMap<>();
        body.put("serialNo", serialNo);
        String uuidString = UUID.randomUUID().toString();
        body.put("vcode", uuidString.substring(0, 6));
        createCollectionItem(REINIT_VCODE_TABLE,JsonUtil.writeValueAsString(body));
        return true;
    }

    /**
     * 验证重置出厂初始化
     * @param serialNo
     * @param vcode
     * @return
     */
    public boolean validateReinit(String serialNo, String vcode) {
        // 查询最新验证码是否仍然有效，如果有效(创建时间起5分钟内)，则继续使用
        String filters = "{\"serialNo\" : \"" + serialNo + "\",\"vcode\" : \"" + vcode + "\"}";
        NocoListResponse<HashMap> listResponse = listCollectionItems(REINIT_VCODE_TABLE,filters,1,1,"-createdAt",HashMap.class);
        if(listResponse != null && !CollectionUtils.isEmpty(listResponse.getData())){
            HashMap<String,Object> record = listResponse.getData().get(0);
            Instant createdAt = Instant.parse(String.valueOf(record.get("createdAt")));
            Instant expireAt = createdAt.plus(5,ChronoUnit.MINUTES);
            if(Instant.now().compareTo(expireAt) <= 0 && Boolean.parseBoolean(String.valueOf(record.get("valid")))){
                // 验证码在5分钟内并且没有被使用过
                String idFilter = "{\"id\":" + record.get("id") + "}";
                Map<String, Object> updates = new HashMap<>();
                updates.put("valid", false);
                updateCollectionItem(REINIT_VCODE_TABLE,idFilter,updates);
                // 删除旧设备注册信息
                DeviceEntity existEntity = deviceService.findByNotice(serialNo);
                if(existEntity != null){
                    deviceService.deleteDevice(existEntity.getId());
                }
                return true;
            }
        }
        return false;
    }

    public void updateTaskStatusById(long taskId, long statusId) {
        String filters = "{\"ref_lifecycle\":" + taskId + "}";
        Map<String, Object> updates = new HashMap<>();
        updates.put("ref_status", statusId);
        updateCollectionItem(CONTRACT_R_STATUS,filters,updates);
    }

    public void saveOrderInfo(long taskId, long orderId,Double orderPrice) {
        String filters = "{\"id\":" + taskId + "}";
        Map<String, Object> updates = new HashMap<>();
        updates.put("booking_order_number", String.valueOf(orderId));
        if(orderPrice != null){
            updates.put("booking_order_price", String.valueOf(orderPrice));
        }
        updateCollectionItem(FREIGHT_BIDDING,filters,updates);
    }

    /**
     * 将lc订单数据写回系统
     * @param lcOrderList
     */
    public void saveLcOrderList(List<LcOrderDO> lcOrderList) {
        if(CollectionUtils.isEmpty(lcOrderList)){
            return;
        }
        createCollectionItem(LC_API_ORDER_LIST,JsonUtil.writeValueAsString(lcOrderList));
    }

    private void createCollectionItem(String collectionName,String body){
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("collectionName", collectionName);
        MultiValueMap headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        restTemplate.exchange(playerConfig.getNocoApiBaseUrl() + "{collectionName}:create",
            HttpMethod.POST,
            requestEntity,
            HashMap.class,
            uriVariables);
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


    private NocoSingleRecordResponse getCollectionItem(String collectionName,String jsonFilters,Class<?> dataClazz){
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("collectionName", collectionName);
        uriVariables.put("filters", jsonFilters);
        HashMap<String, Object> result = restTemplate.getForObject(
            playerConfig.getNocoApiBaseUrl() + "{collectionName}:get?filter={filters}",
            HashMap.class,
            uriVariables);
        if (result != null) {
            NocoSingleRecordResponse response= objectMapper.convertValue(result, NocoSingleRecordResponse.class);
            if(response.getData() != null){
                response.setData(objectMapper.convertValue(response.getData(),dataClazz));
            }
            return response;
        }
        return null;
    }

    private NocoListResponse listCollectionItems(String collectionName,String jsonFilters, int page,int pageSize, String sort,Class<?> dataClazz){
        Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("collectionName", collectionName);
        uriVariables.put("filters", jsonFilters);
        uriVariables.put("sort", sort);
        uriVariables.put("page", page);
        uriVariables.put("pageSize", pageSize);
        String urlPath = "{collectionName}:list?filter={filters}&page={page}&pageSize={pageSize}";
        if(StringUtils.hasText(sort)){
            urlPath = "{collectionName}:list?filter={filters}&page={page}&pageSize={pageSize}&sort={sort}";
        }
        HashMap<String, Object> result = restTemplate.getForObject(playerConfig.getNocoApiBaseUrl() + urlPath,
            HashMap.class,
            uriVariables);
        if (result != null) {
            NocoListResponse response = objectMapper.convertValue(result, NocoListResponse.class);
            List dataList = new ArrayList();
            for(int i = 0;response.getData() != null && i< response.getData().size();i++){
                dataList.add(objectMapper.convertValue(response.getData().get(i),dataClazz));
            }
            response.setData(dataList);
            return response;
        }
        return null;
    }

}
