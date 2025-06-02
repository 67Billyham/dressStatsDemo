package com.linhao.demo2;

import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.admin.indices.mapping.put.PutMappingRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource("classpath:application.yml")
@Import(TestElasticConfig.class)
public class ShopSalesMappingTest {

    @Autowired
    private RestHighLevelClient esClient;

    @Test
    void createMappingForShopSales() throws IOException {
        String indexName = "shop_sales";

        // 1. 检查索引是否存在
        if (!esClient.indices().exists(new GetIndexRequest().indices(indexName), RequestOptions.DEFAULT)) {
            CreateIndexRequest createRequest = new CreateIndexRequest(indexName)
                    .settings(Settings.builder()
                            .put("index.number_of_shards", 1)
                            .put("index.number_of_replicas", 0));

            esClient.indices().create(createRequest, RequestOptions.DEFAULT);
        }

        // 2. 定义映射的JSON
        String mappingJson = "{\n" +
                "  \"properties\": {\n" +
                "    \"shop_id\": {\"type\": \"keyword\"},\n" +
                "    \"item_id\": {\"type\": \"keyword\"},\n" +
                "    \"item_title\": {\n" +
                "      \"type\": \"text\",\n" +
                "      \"fields\": {\n" +
                "        \"keyword\": {\n" +
                "          \"type\": \"keyword\",\n" +
                "          \"ignore_above\": 256\n" +
                "        }\n" +
                "      }\n" +
                "    },\n" +
                "    \"sales_day\": {\"type\": \"date\", \"format\": \"yyyyMMdd\"},\n" +
                "    \"sales_volume\": {\"type\": \"integer\"}\n" +
                "  }\n" +
                "}";

        // 3. 创建映射请求
        PutMappingRequest putMappingRequest = new PutMappingRequest(indexName)
                .type("_doc")
                .source(mappingJson, XContentType.JSON);

        // 4. 执行映射更新
        boolean acknowledged = esClient.indices()
                .putMapping(putMappingRequest, RequestOptions.DEFAULT)
                .isAcknowledged();

        assertTrue(acknowledged, "映射创建成功");
    }
}