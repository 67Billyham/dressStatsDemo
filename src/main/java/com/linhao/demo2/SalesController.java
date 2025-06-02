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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
public class SalesController {

    @Autowired
    private RestHighLevelClient esClient;

    // 新的返回数据结构
    public static class SalesResponse {
        private List<DailySales> trends;
        private SalesStatistics stats;

        public SalesResponse(List<DailySales> trends, SalesStatistics stats) {
            this.trends = trends;
            this.stats = stats;
        }

        public List<DailySales> getTrends() {
            return trends;
        }

        public SalesStatistics getStats() {
            return stats;
        }
    }


    public static class DailySales {
        private String date;
        private int sales;
        private SalesChange change;

        public DailySales(String date, int sales, SalesChange change) {
            this.date = date;
            this.sales = sales;
            this.change = change;
        }

        public String getDate() {
            return date;
        }

        public int getSales() {
            return sales;
        }

        public SalesChange getChange() {
            return change;
        }
    }

    public static class SalesChange {
        private int amount;
        private double percent;

        public SalesChange(int amount, double percent) {
            this.amount = amount;
            this.percent = percent;
        }

        public int getAmount() {
            return amount;
        }

        public double getPercent() {
            return percent;
        }
    }

    public static class SalesStatistics {
        private int totalSales;
        private int averageSales;
        private SalesRecord maxSales;
        private SalesRecord minSales;
        private int increaseDays;
        private int decreaseDays;
        private int stableDays;

        public SalesStatistics() {
        }

        public int getTotalSales() {
            return totalSales;
        }

        public void setTotalSales(int totalSales) {
            this.totalSales = totalSales;
        }

        public int getAverageSales() {
            return averageSales;
        }

        public void setAverageSales(int averageSales) {
            this.averageSales = averageSales;
        }

        public SalesRecord getMaxSales() {
            return maxSales;
        }

        public void setMaxSales(SalesRecord maxSales) {
            this.maxSales = maxSales;
        }

        public SalesRecord getMinSales() {
            return minSales;
        }

        public void setMinSales(SalesRecord minSales) {
            this.minSales = minSales;
        }

        public int getIncreaseDays() {
            return increaseDays;
        }

        public void setIncreaseDays(int increaseDays) {
            this.increaseDays = increaseDays;
        }

        public int getDecreaseDays() {
            return decreaseDays;
        }

        public void setDecreaseDays(int decreaseDays) {
            this.decreaseDays = decreaseDays;
        }

        public int getStableDays() {
            return stableDays;
        }

        public void setStableDays(int stableDays) {
            this.stableDays = stableDays;
        }
    }


    public static class SalesRecord {
        private String date;
        private int value;

        public SalesRecord(String date, int value) {
            this.date = date;
            this.value = value;
        }

        public String getDate() {
            return date;
        }

        public int getValue() {
            return value;
        }
    }

    @GetMapping(value = "/trend", produces = "application/json")
    public ResponseEntity<?> getSalesTrend(@RequestParam String shopId) throws IOException {
        // 1. 从ES获取原始数据
        List<Map<String, Object>> rawData = getRawSalesData(shopId);

        if (rawData == null || rawData.isEmpty()) {
            // 返回空数据而不是 null
            return ResponseEntity.ok().body(Collections.emptyMap());
        }

        // 2. 按照日期排序
        Collections.sort(rawData, (a, b) -> a.get("date").toString().compareTo(b.get("date").toString()));

        // 3. 计算每日涨跌幅和统计数据
        SalesResponse response = calculateSalesTrendResponse(rawData);

        return ResponseEntity.ok(response);
    }

    // 获取原始销售数据的方法
    private List<Map<String, Object>> getRawSalesData(String shopId) throws IOException {
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

        return result;
    }

    // 计算销售趋势响应
    private SalesResponse calculateSalesTrendResponse(List<Map<String, Object>> rawData) {
        List<DailySales> trends = new ArrayList<>();
        SalesStatistics stats = new SalesStatistics();

        // 计算统计数据
        stats.totalSales = rawData.stream().mapToInt(day -> (Integer) day.get("sales")).sum();
        stats.averageSales = rawData.isEmpty() ? 0 : stats.totalSales / rawData.size();

        // 找到最大和最小销售额
        Map<String, Object> maxDay = Collections.max(rawData, Comparator.comparingInt(day -> (Integer) day.get("sales")));
        Map<String, Object> minDay = Collections.min(rawData, Comparator.comparingInt(day -> (Integer) day.get("sales")));
        stats.maxSales = new SalesRecord(maxDay.get("date").toString(), (Integer) maxDay.get("sales"));
        stats.minSales = new SalesRecord(minDay.get("date").toString(), (Integer) minDay.get("sales"));

        // 初始化计数器
        int increaseDays = 0;
        int decreaseDays = 0;
        int stableDays = 0;

        // 处理每日数据
        for (int i = 0; i < rawData.size(); i++) {
            String date = (String) rawData.get(i).get("date");
            int sales = (Integer) rawData.get(i).get("sales");
            SalesChange change = null;

            if (i > 0) {
                int prevSales = (Integer) rawData.get(i - 1).get("sales");
                int amount = sales - prevSales;
                double percent = calculatePercentageChange(prevSales, amount);

                // 计数
                if (amount > 0) increaseDays++;
                else if (amount < 0) decreaseDays++;
                else stableDays++;

                change = new SalesChange(amount, percent);
            } else {
                // 第一天没有前一天数据
                change = new SalesChange(0, 0.0);
                stableDays++;
            }

            trends.add(new DailySales(date, sales, change));
        }

        // 设置统计信息
        stats.increaseDays = increaseDays;
        stats.decreaseDays = decreaseDays;
        stats.stableDays = stableDays;

        return new SalesResponse(trends, stats);
    }

    // 计算百分比变化的方法
    private double calculatePercentageChange(int prevSales, int amount) {
        if (prevSales == 0) {
            // 防止除零错误
            return amount > 0 ? 100.0 : (amount < 0 ? -100.0 : 0.0);
        }
        double percent = (amount * 100.0) / prevSales;
        return round(percent, 2);
    }

    // 数字四舍五入方法
    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
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