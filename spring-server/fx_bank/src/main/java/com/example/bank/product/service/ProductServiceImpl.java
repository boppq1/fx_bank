package com.example.bank.product.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bank.product.dao.IProductDao;
import com.example.bank.product.dto.ProductCurrencyDto;
import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.dto.ProductListDto;
import com.example.bank.product.dto.ProductPreferentialRateDto;
import com.example.bank.product.dto.ProductRateDto;
import com.example.bank.product.dto.ProductReviewDto;
import com.example.bank.product.dto.ProductTermDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final IProductDao productDao;

    @Override
    public List<ProductListDto> getForeignProductList() {
        return productDao.selectForeignProductList();
    }

    @Override
    public ProductDetailDto getProductDetail(Long productNo) {
        return productDao.selectProductDetail(productNo);
    }

    @Override
    public List<ProductCurrencyDto> getProductCurrencies(Long productNo) {
        return productDao.selectProductCurrencies(productNo);
    }

    @Override
    public List<ProductRateDto> getProductRates(Long productNo) {
        return productDao.selectProductRates(productNo);
    }

    @Override
    public List<ProductPreferentialRateDto> getProductPreferentialRates(Long productNo) {
        return productDao.selectProductPreferentialRates(productNo);
    }

    @Override
    public List<ProductTermDto> getProductTerms(Long productNo) {
        return productDao.selectProductTerms(productNo);
    }

    @Override
    public List<ProductReviewDto> getProductReviews(Long productNo) {
        return productDao.selectProductReviews(productNo);
    }
}