package com.linhao.demo2.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.DateUtil;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ExcelToElasticsearchImporter {

    private final RestHighLevelClient esClient;

    public ExcelToElasticsearchImporter(RestHighLevelClient esClient) {
        this.esClient = esClient;
    }

    public void importData(String filePath) throws Exception {
        File file = new File(filePath);
        FileInputStream fis = new FileInputStream(file);
        Workbook workbook = WorkbookFactory.create(fis);

        BulkRequest bulkRequest = new BulkRequest();
        Sheet sheet = workbook.getSheetAt(0); // 第一个sheet

        // 遍历行（跳过标题行）
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Map<String, Object> doc = new HashMap<>();
            doc.put("shop_id", getLongValue(row.getCell(0))); // 店铺ID
            doc.put("item_id", getLongValue(row.getCell(1))); // 商品ID
            doc.put("item_title", getStringValue(row.getCell(2))); // 商品标题
            doc.put("sales_day", getStringValue(row.getCell(3))); // 销售日期
            doc.put("sales_volume", getIntValue(row.getCell(4))); // 销售数量

            bulkRequest.add(new IndexRequest("shop_sales")
                    .source(doc, XContentType.JSON));
        }

        // 批量导入ES
        if (bulkRequest.numberOfActions() > 0) {
            esClient.bulk(bulkRequest, RequestOptions.DEFAULT);
        }

        workbook.close();
        fis.close();
    }

    // 处理数值类型的单元格
    private Long getLongValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (long) cell.getNumericCellValue();
            case STRING:
                try {
                    return Long.parseLong(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    // 处理数值类型的单元格
    private Integer getIntValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                return (int) cell.getNumericCellValue();
            case STRING:
                try {
                    return Integer.parseInt(cell.getStringCellValue().trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            default:
                return null;
        }
    }

    // 通用方法，处理所有类型的单元格
    private String getStringValue(Cell cell) {
        if (cell == null) return null;

        switch (cell.getCellType()) {
            case NUMERIC:
                // 如果是日期格式，特殊处理
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    return new SimpleDateFormat("yyyyMMdd").format(date);
                } else {
                    // 数值型内容转为字符串
                    return String.valueOf((int) cell.getNumericCellValue());
                }
            case STRING:
                return cell.getStringCellValue().trim();
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                // 处理公式计算结果
                switch (cell.getCachedFormulaResultType()) {
                    case NUMERIC:
                        return String.valueOf((int) cell.getNumericCellValue());
                    case STRING:
                        return cell.getStringCellValue().trim();
                    default:
                        return "";
                }
            default:
                return "";
        }
    }
}