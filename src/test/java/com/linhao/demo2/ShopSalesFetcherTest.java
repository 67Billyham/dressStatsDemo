package com.linhao.demo2;

import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
public class ShopSalesFetcherTest {

    @Autowired
    private RestHighLevelClient esClient;

    @Test
    public void fetchTenShopSalesDocuments() throws IOException {
        // 1. 创建搜索请求
        SearchRequest searchRequest = new SearchRequest("shop_sales");

        // 2. 构建搜索源
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchAllQuery()); // 匹配所有文档
        searchSourceBuilder.size(10); // 获取前10个文档
        searchSourceBuilder.from(0); // 从第一条文档开始

        // 添加排序（按销售日期倒序，最新的在前）
        searchSourceBuilder.sort("sales_day");

        // 指定返回哪些字段（可选）
        String[] includeFields = new String[]{"shop_id", "item_id", "item_title", "sales_day", "sales_volume"};
        String[] excludeFields = new String[]{};
        searchSourceBuilder.fetchSource(includeFields, excludeFields);

        searchRequest.source(searchSourceBuilder);

        System.out.println("正在查询 Elasticsearch...");

        // 3. 执行搜索请求
        SearchResponse searchResponse = esClient.search(searchRequest, RequestOptions.DEFAULT);

        // 4. 处理响应结果
        SearchHits hits = searchResponse.getHits();
        long totalHits = hits.getTotalHits().value;
        System.out.println("获取到 " + totalHits + " 个文档，显示前10个:");

        // 5. 打印每个文档
        int count = 1;
        for (SearchHit hit : hits) {
            System.out.println("\n文档 #" + count++ + ":");
            System.out.println("  文档ID: " + hit.getId());

            // 获取并打印文档内容
            String shopId = getFieldAsString(hit, "shop_id");
            String itemId = getFieldAsString(hit, "item_id");
            String itemTitle = getFieldAsString(hit, "item_title");
            String salesDay = getFieldAsString(hit, "sales_day");
            String salesVolume = getFieldAsString(hit, "sales_volume");

            System.out.println("  店铺ID: " + shopId);
            System.out.println("  商品ID: " + itemId);
            System.out.println("  商品标题: " + itemTitle);
            System.out.println("  销售日期: " + salesDay);
            System.out.println("  销售数量: " + salesVolume);
        }

        System.out.println("\n查询完成");
    }

    // 辅助方法：安全获取字段值
    private String getFieldAsString(SearchHit hit, String fieldName) {
        Object value = hit.getSourceAsMap().get(fieldName);
        return value != null ? value.toString() : "N/A";
    }
}