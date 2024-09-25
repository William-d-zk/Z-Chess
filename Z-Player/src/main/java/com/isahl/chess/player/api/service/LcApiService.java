package com.isahl.chess.player.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.pawn.endpoint.device.db.central.model.LcOrderEntity;
import com.isahl.chess.pawn.endpoint.device.db.central.repository.ILcApiRepository;
import com.isahl.chess.player.api.config.PlayerConfig;
import com.isahl.chess.player.api.helper.XmlToJsonConverter;
import com.isahl.chess.player.api.model.LcApiListResponse;
import com.isahl.chess.player.api.model.LcOrderDO;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * @author xiaojiang.lxj at 2024-09-18 15:40.
 */
@Service
public class LcApiService {

    private static final Integer PAGE_SIZE = 100;

    private final Logger log = Logger.getLogger("biz.player." + getClass().getSimpleName());

    private final PlayerConfig playerConfig;

    private final AliothApiService aliothApiService;

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    private final XmlToJsonConverter xmlToJsonConverter;

    private final ILcApiRepository iLcApiRepository;


    public LcApiService(
        PlayerConfig playerConfig,
        AliothApiService aliothApiService,
        RestTemplateBuilder restTemplateBuilder,
        ObjectMapper objectMapper,
        XmlToJsonConverter xmlToJsonConverter,
        ILcApiRepository iLcApiRepository) {
        this.playerConfig = playerConfig;
        this.aliothApiService = aliothApiService;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(10 * PlayerConfig.TIMEOUT))
            .setReadTimeout(Duration.ofMillis(10 * PlayerConfig.TIMEOUT))
            .build();
        this.objectMapper = objectMapper;
        this.xmlToJsonConverter = xmlToJsonConverter;
        this.iLcApiRepository = iLcApiRepository;
    }

    /**
     * 从良仓导入订单数据，v1版本，通过noco api写入数据库
     *
     * @param appToken
     * @param appKey
     */
    public void importOrderListFromLc(String appToken,String appKey,String createFrom, String createTo){
        long start = System.currentTimeMillis();
        LcApiListResponse apiListResponse = fetchOrderList(appToken,appKey,1,PAGE_SIZE,createFrom,createTo);
        if(apiListResponse == null){
            return;
        }
        saveOrderList(apiListResponse.getData());
        Integer totalCount = Integer.parseInt(apiListResponse.getCount());
        Integer totalPage = (totalCount % PAGE_SIZE == 0) ? totalCount / PAGE_SIZE : (totalCount / PAGE_SIZE) + 1;
        int page = 1;
        while(++page <=  totalPage){
            log.info("appToken = "+appToken+" totalPage = "+totalPage+" currentPage = "+page);
            apiListResponse = fetchOrderList(appToken,appKey,page,PAGE_SIZE,createFrom,createTo);
            saveOrderList(apiListResponse.getData());
        }
        long end = System.currentTimeMillis();
        log.info("从AppToken = "+appToken+" 导入 " + totalCount + " 条数据完成，耗时 " + (end-start)+ " ms");
    }

    /**
     * 从良仓导入订单数据，v2版本，直接写入数据库
     *
     * @param appToken
     * @param appKey
     */
    public void importOrderListFromLcV2(String appToken,String appKey,String createFrom, String createTo){
        long start = System.currentTimeMillis();
        LcApiListResponse apiListResponse = fetchOrderList(appToken,appKey,1,PAGE_SIZE,createFrom,createTo);
        if(apiListResponse == null){
            log.info("appToken = "+appToken+" 没有获取到订单数据，请检查程序是否有异常或者咨询源数据方是否确有订单数据");
            return;
        }
        int totalSaved = 0;
        try{
            saveOrderListV2(apiListResponse.getData());
            totalSaved += apiListResponse.getData().size();
        }catch (Throwable ignored){
        }
        Integer totalCount = Integer.parseInt(apiListResponse.getCount());
        int totalPage = (totalCount % PAGE_SIZE == 0) ? totalCount / PAGE_SIZE : (totalCount / PAGE_SIZE) + 1;
        int page = 1;

        while(++page <=  totalPage){
            log.info("appToken = "+appToken+" totalCount = " + totalCount +" totalSaved = " + totalSaved +" totalPage = "+totalPage+" currentPage = "+page);
            apiListResponse = fetchOrderList(appToken,appKey,page,PAGE_SIZE,createFrom,createTo);
            if(apiListResponse == null){
                log.fetal("获取订单列表出现异常，将于 " + PlayerConfig.SLEEP_INTERVAL+"ms 后自动重试 appToken = "+appToken+" totalCount = "+totalCount+" totalSaved = "+totalSaved+" totalPage = "+totalPage+" currentPage = "+page);
                try {
                    Thread.sleep(PlayerConfig.SLEEP_INTERVAL);
                    --page;
                } catch (InterruptedException e) {
                    log.fetal("Thread sleep encounter exception.",e);
                }
            }else{
                try{
                    saveOrderListV2(apiListResponse.getData());
                    totalSaved += apiListResponse.getData().size();
                }catch (Throwable ignored){
                }
            }
        }
        long end = System.currentTimeMillis();
        log.info("从AppToken = "+appToken+" 导入 " + totalSaved + " 条数据完成，耗时 " + (end-start)+ " ms");
    }


    /**
     * 从良仓oms api获取订单数据
     *
     * @param appToken
     * @param appKey
     * @param page
     * @param pageSize
     * @param createFrom 订单创建开始时间， 格式YYYY-MM-DD HH:II:SS
     * @param createTo 订单创建结束时间， 格式YYYY-MM-DD HH:II:SS
     *
     * @return
     */
    public LcApiListResponse fetchOrderList(String appToken,String appKey, Integer page,Integer pageSize, String createFrom, String createTo){
        long start  = System.currentTimeMillis();
        String create_date_from = (createFrom != null) ? createFrom : "";
        String create_date_to = (createTo != null) ? createTo : "";
        MultiValueMap<String, String> headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
        String xmlBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:ns1=\"http://www.example.org/Ec/\">\n"
            + "    <SOAP-ENV:Body>\n"
            + "        <ns1:callService>\n"
            + "            <paramsJson>\n"
            + "                {\n"
            + "                    \"pageSize\":"+pageSize+",\n"
            + "                    \"page\":"+ page + ",\n"
            + "                    \"order_code\":\"\",\n"
            + "                    \"order_status\":\"\",\n"
            + "                    \"shipping_method\":\"\",\n"
            + "                    \"order_code_arr\":[],\n"
            + "                    \"create_date_from\":\"" + create_date_from +"\",\n"
            + "                    \"create_date_to\":\"" + create_date_to + "\",\n"
            + "                    \"modify_date_from\":\"\",\n"
            + "                    \"modify_date_to\":\"\",\n"
            + "                    \"ship_date_from\":\"\",\n"
            + "                    \"ship_date_to\":\"\"\n"
            + "                }\n"
            + "            </paramsJson>\n"
            + "            <appToken>"+appToken+"</appToken>\n"
            + "            <appKey>"+appKey+"</appKey>\n"
            + "            <service>getOrderList</service>\n"
            + "        </ns1:callService>\n"
            + "    </SOAP-ENV:Body>\n"
            + "</SOAP-ENV:Envelope>\n"
            + "    ";
        try{
            HttpEntity<String> requestEntity = new HttpEntity<>(xmlBody, headers);
            String result = restTemplate.postForObject(playerConfig.getLcWebApiUrl(),
                requestEntity,
                String.class);
            if (result != null) {
                JsonNode jsonNode = xmlToJsonConverter.convertXmlToJson(result);
                String responseText = jsonNode.findPath("response").asText();
                LcApiListResponse response = JsonUtil.readValue(responseText,LcApiListResponse.class);
                List dataList = new ArrayList(response.getData() != null ? response.getData().size() : 0);
                int countDateCreateNull = 0;
                for(int i=0;response.getData() != null && i < response.getData().size();i++){
                    LcOrderDO orderDO = objectMapper.convertValue(response.getData().get(i),LcOrderDO.class);
                    if("0000-00-00 00:00:00".equals(orderDO.getDate_create())){
                        ++countDateCreateNull;
                        orderDO.setDate_create(null);
                    }
                    if("0000-00-00 00:00:00".equals(orderDO.getDate_modify())){
                        orderDO.setDate_modify(null);
                    }
                    if("0000-00-00 00:00:00".equals(orderDO.getDate_release())){
                        orderDO.setDate_release(null);
                    }
                    if("0000-00-00 00:00:00".equals(orderDO.getDate_shipping())){
                        orderDO.setDate_shipping(null);
                    }
                    if("0000-00-00 00:00:00".equals(orderDO.getShip_batch_time())){
                        orderDO.setShip_batch_time(null);
                    }
                    dataList.add(orderDO);
                }
                if(countDateCreateNull > 0){
                    log.warning("appToken="+appToken+" page="+page+" pageSize="+pageSize+" 共有 "+countDateCreateNull+" 条记录其 date_create 字段为空!");
                }
                response.setData(dataList);
                return response;
            }
        }catch (Throwable t){
            log.fetal("获取订单列表遇到异常, appToken = " + appToken, t);
        }finally {
            long end = System.currentTimeMillis();
            log.info("appToken = " +appToken+" 获取良仓订单列表耗时 " + (end-start) + " ms");
        }
        return null;
    }

    /**
     * 保存订单数据，通过noco api
     *
     * @param lcOrderDOList
     */
    public void saveOrderList(List<LcOrderDO> lcOrderDOList){
        long start =  System.currentTimeMillis();
        if(CollectionUtils.isEmpty(lcOrderDOList)){
            return;
        }
        try{
            aliothApiService.saveLcOrderList(lcOrderDOList);
        }catch (Throwable t){
            log.fetal("写入良仓订单数据出现异常",t);
        }finally {
            log.info("写入良仓订单数据耗时 " + (System.currentTimeMillis() - start) + " ms");
        }
    }

    /**
     * 保存订单数据，v2版本，直接写入数据库
     *
     * @param lcOrderDOList
     */
    public void saveOrderListV2(List<LcOrderDO> lcOrderDOList){
        long start =  System.currentTimeMillis();
        if(CollectionUtils.isEmpty(lcOrderDOList)){
            return;
        }
        List<LcOrderEntity> lcOrderEntityList = new ArrayList<>(lcOrderDOList.size());
        try{
            for(LcOrderDO orderDO : lcOrderDOList){
                LcOrderEntity orderEntity = new LcOrderEntity();
                BeanUtils.copyProperties(orderDO,orderEntity,"addressType");
                orderEntity.setAddress_type(orderDO.getAddressType());
                lcOrderEntityList.add(orderEntity);
            }
            iLcApiRepository.saveAll(lcOrderEntityList);
        }catch (Throwable t){
            log.fetal("写入良仓订单数据出现异常",t);
            throw t;
        }finally {
            log.info("写入良仓订单数据耗时 " + (System.currentTimeMillis() - start) + " ms");
        }
    }
}
