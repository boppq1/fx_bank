package com.example.bank.product.controller;

import com.example.bank.product.dto.ProductDetailDto;
import com.example.bank.product.dto.ProductTermDto;
import com.example.bank.product.service.ProductJoinService;
import com.example.bank.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

    @GetMapping("/product/join/{productNo}/terms/{termsCode}")
    public String readTerms(
            @PathVariable("productNo") Long productNo,
            @PathVariable("termsCode") String termsCode,
            Model model
    ) {
        ProductDetailDto product = productService.getProductDetail(productNo);
        if (product == null) {
            return "redirect:/product/list";
        }

        ProductTermDto term = productJoinService.getJoinTerms(productNo).stream()
                .filter(item -> termsCode.equals(item.getTermsCode()))
                .findFirst()
                .orElse(null);
        if (term == null) {
            return "redirect:/product/join/" + productNo + "/terms";
        }

        model.addAttribute("product", product);
        model.addAttribute("term", term);
        model.addAttribute("pdfPageNumbers", getPdfPageNumbers(term));
        return "product/join/terms-reader";
    }

    @GetMapping("/product/join/{productNo}/form")
    public String form(@PathVariable("productNo") Long productNo, Model model) {
        ProductDetailDto product = productService.getProductDetail(productNo);
        if (product == null) {
            return "redirect:/product/list";
        }

        model.addAttribute("product", product);
        model.addAttribute("demandDepositProduct", isDemandDepositProduct(product));
        model.addAttribute("currencies", productService.getProductCurrencies(productNo));
        model.addAttribute("rates", productService.getProductRates(productNo));
        return "product/join/form";
    }

    @GetMapping("/product/join/{productNo}/coupon")
    public String coupon(@PathVariable("productNo") Long productNo, Model model) {
        ProductDetailDto product = productService.getProductDetail(productNo);
        if (product == null) {
            return "redirect:/product/list";
        }

        model.addAttribute("product", product);
        return "product/join/coupon";
    }

    @GetMapping("/product/join/{productNo}/signature")
    public String signature(@PathVariable("productNo") Long productNo, Model model) {
        ProductDetailDto product = productService.getProductDetail(productNo);
        if (product == null) {
            return "redirect:/product/list";
        }

        model.addAttribute("product", product);
        return "product/join/signature";
    }

    @GetMapping("/product/my-subscriptions")
    public String mySubscriptions() {
        return "product/my-subscriptions";
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
    private List<Integer> getPdfPageNumbers(ProductTermDto term) {
        List<Integer> pageNumbers = new ArrayList<>();
        if (term == null || term.getPdfPath() == null || term.getPdfPath().isBlank()) {
            return pageNumbers;
        }

        Path pdfPath = Path.of(term.getPdfPath()).normalize();
        if (!Files.isRegularFile(pdfPath)) {
            return pageNumbers;
        }

        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            for (int page = 1; page <= document.getNumberOfPages(); page++) {
                pageNumbers.add(page);
            }
        } catch (Exception ignored) {
            pageNumbers.clear();
        }
        return pageNumbers;
    }
    private boolean isDemandDepositProduct(ProductDetailDto product) {
        if (product == null) {
            return false;
        }
        String productType = product.getProductType() == null ? "" : product.getProductType();
        String productName = product.getProductName() == null ? "" : product.getProductName();
        String productText = productType + " " + productName;

        if (productText.contains("적금") || productText.contains("정기")) {
            return false;
        }

        boolean hasJoinPeriod = (product.getMinPeriodMonth() != null && product.getMinPeriodMonth() > 0)
                || (product.getMaxPeriodMonth() != null && product.getMaxPeriodMonth() > 0);
        return productText.contains("통장")
                || productText.contains("입출금")
                || productText.contains("예금")
                || !hasJoinPeriod;
    }
}
