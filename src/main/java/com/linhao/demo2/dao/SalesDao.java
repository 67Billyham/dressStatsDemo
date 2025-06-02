package com.linhao.demo2.dao;

import com.linhao.demo2.dto.SalesTrendDTO;

import java.io.IOException;
import java.util.List;

public interface SalesDao {
    /**
     * 搜索销售数据列表
     * @param shopId
     * @return List<SalesTrendDTO> 销售数据列表
     * @throws IOException
     */
    List<SalesTrendDTO> getSalesTrend(String shopId) throws IOException;

    /**
     * 搜索店名列表
     * @return List<String> 所有店名列表
     * @throws IOException
     */
    List<String> getAllShopIds() throws IOException;
}