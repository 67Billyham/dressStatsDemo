package com.linhao.demo2.controller;

import com.linhao.demo2.dto.SalesTrendDTO;
import com.linhao.demo2.service.SalesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*")
public class SalesController {

    private final SalesService salesService;

    public SalesController(SalesService salesService) {
        this.salesService = salesService;
    }

    /**
     * 搜索商家id对应的销售数据
     * @param shopId
     * @return 销售数据列表
     * @throws IOException
     */
    @GetMapping("/trend")
    public ResponseEntity<List<SalesTrendDTO>> getSalesTrend(@RequestParam String shopId) throws IOException {
        return ResponseEntity.ok(salesService.getSalesTrend(shopId));
    }

    /**
     * 搜索商家id列表
     * @return 商家id列表
     * @throws IOException
     */
    @GetMapping("/shopIds")
    public ResponseEntity<List<String>> getAllShopIds() throws IOException {
        return ResponseEntity.ok(salesService.getAllShopIds());
    }
}