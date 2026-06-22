package com.example.bank.product.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.dto.ProductReviewDto;
import com.example.bank.product.service.ProductService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // 외화 상품 목록
    @GetMapping({"/product/list", "/products"})
    public String productList(Model model) {

        model.addAttribute("products", productService.getForeignProductList());

        return "product/list";
    }

    // 외화 상품 상세
    @GetMapping("/product/detail/{productNo}")
    public String productDetail(@PathVariable("productNo") Long productNo, Model model) {

        ProductDetailDto product = productService.getProductDetail(productNo);

        if (product == null) {
            return "redirect:/product/list";
        }

        model.addAttribute("product", product);
        model.addAttribute("currencies", productService.getProductCurrencies(productNo));
        model.addAttribute("rates", productService.getProductRates(productNo));
        model.addAttribute("preferentialRates", productService.getProductPreferentialRates(productNo));
        model.addAttribute("terms", productService.getProductTerms(productNo));
        List<ProductReviewDto> allReviews = productService.getProductReviews(productNo);
        model.addAttribute("reviews", allReviews.stream().limit(2).toList());
        model.addAttribute("hasMoreReviews", allReviews.size() > 2);

        return "product/detail";
    }

    @GetMapping("/product/{productNo}/reviews")
    public String productReviews(@PathVariable("productNo") Long productNo, Model model) {
        ProductDetailDto product = productService.getProductDetail(productNo);
        if (product == null) {
            return "redirect:/product/list";
        }
        model.addAttribute("product", product);
        model.addAttribute("reviews", productService.getProductReviews(productNo));
        return "product/reviews";
    }

    @GetMapping("/product/{productNo}/reviews/new")
    public String productReviewForm(@PathVariable("productNo") Long productNo, Model model) {
        ProductDetailDto product = productService.getProductDetail(productNo);
        if (product == null) {
            return "redirect:/product/list";
        }
        model.addAttribute("product", product);
        return "product/review-form";
    }
}
