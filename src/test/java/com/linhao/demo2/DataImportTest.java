package com.linhao.demo2 ;
import com.linhao.demo2.util.ExcelToElasticsearchImporter;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class DataImportTest {

    @Autowired
    private RestHighLevelClient esClient;

    @Test
    public void importExcelData() throws Exception {
        String filePath = "D:\\20250506NewEdgeDownload\\（后端实习）商品每日销量样例数据.xlsx"; // 或使用ClassPathResource获取资源文件

        ExcelToElasticsearchImporter importer = new ExcelToElasticsearchImporter(esClient);
        importer.importData(filePath);

        System.out.println("数据导入完成！");
    }
}