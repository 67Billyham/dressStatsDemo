package com.linhao.demo2.service.impl;

import com.linhao.demo2.dao.SalesDao;
import com.linhao.demo2.dto.SalesTrendDTO;
import com.linhao.demo2.service.SalesService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class SalesServiceImpl implements SalesService {

    private final SalesDao salesDao;

    public SalesServiceImpl(SalesDao salesDao) {
        this.salesDao = salesDao;
    }

    @Override
    public List<SalesTrendDTO> getSalesTrend(String shopId) throws IOException {
        return salesDao.getSalesTrend(shopId);
    }

    @Override
    public List<String> getAllShopIds() throws IOException {
        return salesDao.getAllShopIds();
    }
}