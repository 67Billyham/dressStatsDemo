package com.linhao.demo2.service;

import com.linhao.demo2.dto.SalesTrendDTO;

import java.io.IOException;
import java.util.List;

public interface SalesService {
    List<SalesTrendDTO> getSalesTrend(String shopId) throws IOException;
    List<String> getAllShopIds() throws IOException;
}