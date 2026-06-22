package com.example.bank.product.controller;

import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.service.ProductJoinService;
import com.example.bank.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProductJoinPageController {

    private final ProductService productService;
    private final ProductJoinService productJoinService;

    @GetMapping("/product/join/{productNo}/terms")
    public String terms(@PathVariable("productNo") Long productNo, Model model) {
        ProductDetailDto product = productService.getProductDetail(productNo);
        if (product == null) {
            return "redirect:/product/list";
        }

        model.addAttribute("product", product);
        model.addAttribute("terms", productJoinService.getJoinTerms(productNo));
        return "product/join/terms";
    }

    @GetMapping("/product/join/{productNo}/form")
    public String form(@PathVariable("productNo") Long productNo, Model model) {
        ProductDetailDto product = productService.getProductDetail(productNo);
        if (product == null) {
            return "redirect:/product/list";
        }

        model.addAttribute("product", product);
        model.addAttribute("currencies", productService.getProductCurrencies(productNo));
        model.addAttribute("rates", productService.getProductRates(productNo));
        return "product/join/form";
    }

    @GetMapping("/product/join/{productNo}/complete")
    public String complete(
            @PathVariable("productNo") Long productNo,
            @RequestParam("subscriptionNo") Long subscriptionNo,
            Model model
    ) {
        model.addAttribute("productNo", productNo);
        model.addAttribute("complete", productJoinService.getJoinComplete(subscriptionNo));
        return "product/join/complete";
    }
}
