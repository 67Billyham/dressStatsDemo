package com.linhao.demo2.dto;

public class SalesTrendDTO {
    private String date;
    private int sales;

    public SalesTrendDTO(String date, int sales) {
        this.date = date;
        this.sales = sales;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getSales() {
        return sales;
    }

    public void setSales(int sales) {
        this.sales = sales;
    }
}