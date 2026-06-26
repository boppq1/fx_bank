package com.example.bank.product.dto;

import java.util.List;

import lombok.Data;

/**
 * 상품 목록(검색·카테고리·페이징) 결과 + 화면 상태를 함께 담는 DTO.
 * 페이지 이동/검색/필터 시 현재 조건(keyword·type·page)을 화면이 유지하도록 같이 내려준다.
 */
@Data
public class ProductListPageDto {

    private List<ProductListDto> products; // 현재 페이지 목록
    private long totalCount;               // 조건에 맞는 전체 건수
    private int page;                       // 현재 페이지(1부터)
    private int size;                       // 페이지당 건수
    private int totalPages;                 // 총 페이지 수
    private String keyword;                 // 검색어(없으면 null)
    private String type;                    // 카테고리(DEPOSIT/SAVINGS, 없으면 null=전체)
}
