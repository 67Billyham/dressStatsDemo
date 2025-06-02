package com.linhao.demo2.dao.impl;

import com.linhao.demo2.dao.SalesDao;
import com.linhao.demo2.dto.SalesTrendDTO;
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
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Sum;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ElasticsearchSalesDao implements SalesDao {

    private final RestHighLevelClient esClient;

    @Autowired
    public ElasticsearchSalesDao(RestHighLevelClient esClient) {
        this.esClient = esClient;
    }

    @Override
    public List<SalesTrendDTO> getSalesTrend(String shopId) throws IOException {
        SearchRequest searchRequest = new SearchRequest("shop_sales");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery()
                .must(QueryBuilders.termQuery("shop_id", shopId));

        sourceBuilder.query(boolQuery);
        sourceBuilder.size(0);

        DateHistogramAggregationBuilder dateAgg = AggregationBuilders
                .dateHistogram("sales_by_day")
                .field("sales_day")
                .calendarInterval(DateHistogramInterval.DAY)
                .format("yyyyMMdd");

        dateAgg.subAggregation(AggregationBuilders.sum("total_sales").field("sales_volume"));
        sourceBuilder.aggregation(dateAgg);

        searchRequest.source(sourceBuilder);
        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

        Histogram salesByDay = response.getAggregations().get("sales_by_day");
        List<SalesTrendDTO> result = new ArrayList<>();

        for (Histogram.Bucket bucket : salesByDay.getBuckets()) {
            String date = bucket.getKeyAsString();
            Sum totalSales = bucket.getAggregations().get("total_sales");
            int total = (int) totalSales.getValue();

            result.add(new SalesTrendDTO(date, total));
        }

        return result;
    }

    @Override
    public List<String> getAllShopIds() throws IOException {
        SearchRequest searchRequest = new SearchRequest("shop_sales");
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        TermsAggregationBuilder shopIdAgg = AggregationBuilders.terms("shop_ids")
                .field("shop_id")
                .size(10000);

        sourceBuilder.aggregation(shopIdAgg);
        sourceBuilder.size(0);
        searchRequest.source(sourceBuilder);
        SearchResponse response = esClient.search(searchRequest, RequestOptions.DEFAULT);

        List<String> shopIds = new ArrayList<>();
        Terms terms = response.getAggregations().get("shop_ids");
        for (Terms.Bucket bucket : terms.getBuckets()) {
            shopIds.add(bucket.getKeyAsString());
        }

        return shopIds;
    }
}