package com.example.bank.product.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.bank.product.dao.IProductDao;
import com.example.bank.product.dto.ProductCurrencyDto;
import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.dto.ProductListDto;
import com.example.bank.product.dto.ProductListPageDto;
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
    public ProductListPageDto getForeignProductPage(String keyword, String type, int page, int size) {
        // 검색어 정규화: 공백 제거, 빈값이면 null(=전체)
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        // 카테고리 화이트리스트: DEPOSIT/SAVINGS 만 허용, 그 외는 null(=전체)
        String normalizedType = "DEPOSIT".equals(type) || "SAVINGS".equals(type) ? type : null;
        int pageSize = size <= 0 ? 10 : size;
        int currentPage = Math.max(page, 1);

        long totalCount = productDao.countForeignProduct(normalizedKeyword, normalizedType);
        int totalPages = (int) Math.max(1, Math.ceil((double) totalCount / pageSize));
        if (currentPage > totalPages) {
            currentPage = totalPages;
        }
        int offset = (currentPage - 1) * pageSize;

        List<ProductListDto> products =
                productDao.selectForeignProductPage(normalizedKeyword, normalizedType, offset, pageSize);

        ProductListPageDto result = new ProductListPageDto();
        result.setProducts(products);
        result.setTotalCount(totalCount);
        result.setPage(currentPage);
        result.setSize(pageSize);
        result.setTotalPages(totalPages);
        result.setKeyword(normalizedKeyword);
        result.setType(normalizedType);
        return result;
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

    @Override
    public boolean canWriteProductReview(Long userNo, Long productNo) {
        if (userNo == null || productNo == null) {
            return false;
        }
        return productDao.countActiveProductSubscription(userNo, productNo) > 0
                && productDao.countMyProductReview(userNo, productNo) == 0;
    }

    @Override
    public Long writeProductReview(
            Long userNo,
            Long productNo,
            com.example.bank.product.dto.ProductReviewWriteRequestDto request
    ) {
        if (!canWriteProductReview(userNo, productNo)) {
            throw new IllegalArgumentException("가입한 상품에만 리뷰를 한 번 작성할 수 있습니다.");
        }
        if (request == null || request.getReviewText() == null || request.getReviewText().isBlank()) {
            throw new IllegalArgumentException("리뷰 내용을 입력해주세요.");
        }
        if (request.getRating() == null || request.getRating().compareTo(java.math.BigDecimal.ONE) < 0
                || request.getRating().compareTo(new java.math.BigDecimal("5")) > 0) {
            throw new IllegalArgumentException("평점은 1점에서 5점 사이로 입력해주세요.");
        }

        Long reviewNo = productDao.selectNextProductReviewNo();
        productDao.insertProductReview(reviewNo, userNo, productNo, request);
        return reviewNo;
    }
}
