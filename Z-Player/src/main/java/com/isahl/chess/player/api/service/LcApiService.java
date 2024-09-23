package com.isahl.chess.player.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.isahl.chess.king.base.log.Logger;
import com.isahl.chess.king.base.util.JsonUtil;
import com.isahl.chess.player.api.config.PlayerConfig;
import com.isahl.chess.player.api.helper.XmlToJsonConverter;
import com.isahl.chess.player.api.model.LcApiListResponse;
import com.isahl.chess.player.api.model.LcOrderDO;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
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


    public LcApiService(
        PlayerConfig playerConfig,
        AliothApiService aliothApiService,
        RestTemplateBuilder restTemplateBuilder,
        ObjectMapper objectMapper,
        XmlToJsonConverter xmlToJsonConverter) {
        this.playerConfig = playerConfig;
        this.aliothApiService = aliothApiService;
        this.restTemplate = restTemplateBuilder
            .setConnectTimeout(Duration.ofMillis(PlayerConfig.TIMEOUT))
            .setReadTimeout(Duration.ofMillis(10 * PlayerConfig.TIMEOUT))
            .build();
        this.objectMapper = objectMapper;
        this.xmlToJsonConverter = xmlToJsonConverter;
    }

    public void importOrderListFromLc(String appToken,String appKey){
        long start = System.currentTimeMillis();
        LcApiListResponse apiListResponse = fetchOrderList(appToken,appKey,1,PAGE_SIZE);
        if(apiListResponse == null){
            return;
        }
        saveOrderList(apiListResponse.getData());
        Integer totalCount = Integer.parseInt(apiListResponse.getCount());
        Integer totalPage = (totalCount % PAGE_SIZE == 0) ? totalCount / PAGE_SIZE : (totalCount / PAGE_SIZE) + 1;
        int page = 1;
        while(++page <=  totalPage){
            apiListResponse = fetchOrderList(appToken,appKey,page,PAGE_SIZE);
            saveOrderList(apiListResponse.getData());
        }
        long end = System.currentTimeMillis();
        log.info("从AppToken = "+appToken+" 导入 " + totalCount + " 条数据完成，耗时 " + (end-start)+ " ms");
    }


    /**
     * 从良仓oms api获取订单数据
     *
     * @param appToken
     * @param appKey
     * @param page
     * @param pageSize
     * @return
     */
    public LcApiListResponse fetchOrderList(String appToken,String appKey, Integer page,Integer pageSize){
        long start  = System.currentTimeMillis();
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
            + "                    \"create_date_from\":\"\",\n"
            + "                    \"create_date_to\":\"\",\n"
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
                for(int i=0;response.getData() != null && i < response.getData().size();i++){
                    LcOrderDO orderDO = objectMapper.convertValue(response.getData().get(i),LcOrderDO.class);
                    if("0000-00-00 00:00:00".equals(orderDO.getDate_create())){
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
     * 保存订单数据
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
}
