package com.linhao.demo2;

import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.histogram.DateHistogramInterval;
import org.elasticsearch.search.aggregations.bucket.histogram.Histogram;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*") // 允许所有来源
public class SalesController {

    @Autowired
    private RestHighLevelClient esClient;

    @GetMapping("/trend")
    public ResponseEntity<?> getSalesTrend(@RequestParam String shopId) throws IOException {
        SearchRequest searchRequest = new SearchRequest("shop_sales");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 构建查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("shop_id", shopId));

        sourceBuilder.query(boolQuery);
        sourceBuilder.size(0); // 不需要原始文档

        // 按日期聚合
        DateHistogramAggregationBuilder dateAgg = AggregationBuilders
                .dateHistogram("sales_by_day")
                .field("sales_day")
                .calendarInterval(DateHistogramInterval.DAY)
                .format("yyyyMMdd");

        // 添加销量求和子聚合
        dateAgg.subAggregation(AggregationBuilders.sum("total_sales").field("sales_volume"));
        sourceBuilder.aggregation(dateAgg);

        searchRequest.source(sourceBuilder);
        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

        // 获取聚合结果
        Histogram salesByDay = response.getAggregations().get("sales_by_day");
        List<Map<String, Object>> result = new ArrayList<>();

        for (Histogram.Bucket bucket : salesByDay.getBuckets()) {
            // 获取日期
            String date = bucket.getKeyAsString();

            // 获取销售总和
            Sum totalSales = bucket.getAggregations().get("total_sales");
            int total = (int) totalSales.getValue();

            // 添加到结果集
            result.add(Map.of(
                    "date", date,
                    "sales", total
            ));
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/shopIds")
    public ResponseEntity<?> getAllShopIds() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shop_sales");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 使用聚合获取所有唯一的店铺ID
        TermsAggregationBuilder shopIdAgg = AggregationBuilders.terms("shop_ids")
                .field("shop_id")
                .size(10000); // 假设店铺不超过1万个

        sourceBuilder.aggregation(shopIdAgg);
        sourceBuilder.size(0);
        searchRequest.source(sourceBuilder);
        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

        List<String> shopIds = new ArrayList<>();
        Terms terms = response.getAggregations().get("shop_ids");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            shopIds.add(bucket.getKeyAsString());
        }

        return ResponseEntity.ok(shopIds);
    }
}
