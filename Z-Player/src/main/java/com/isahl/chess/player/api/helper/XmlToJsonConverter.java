package com.isahl.chess.player.api.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import org.springframework.stereotype.Component;

/**
 * @author xiaojiang.lxj at 2024-09-18 10:40.
 */
@Component
public class XmlToJsonConverter {

    public JsonNode convertXmlToJson(String xml) throws IOException {
        XmlMapper xmlMapper = new XmlMapper();
        ObjectMapper jsonMapper = new ObjectMapper();

        // 将XML转换为JsonNode树
        JsonNode xmlTree = xmlMapper.readTree(xml);

        // 将JsonNode树转换为JSON字符串
        String json = jsonMapper.writeValueAsString(xmlTree);

        // 返回JsonNode表示JSON字符串
        return jsonMapper.readTree(json);
    }

}
