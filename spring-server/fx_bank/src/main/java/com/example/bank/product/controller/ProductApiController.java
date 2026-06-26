package com.example.bank.product.controller;

import com.example.bank.admin.dao.IProductTermVersionDao;
import com.example.bank.admin.dto.ProductTermVersionDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductApiController {

    private final IProductTermVersionDao termVersionDao;

    // ?ъ슜?먯슜 ?쎄? PDF ?ㅼ슫濡쒕뱶
    @GetMapping("/terms/{termVersionNo}/pdf")
    public ResponseEntity<Resource> downloadTermsPdf(@PathVariable("termVersionNo") long termVersionNo) {
        ProductTermVersionDto version = termVersionDao.selectVersionByNo(termVersionNo);
        if (version == null || version.getPdfPath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            Path filePath = Paths.get(version.getPdfPath());
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String fileName = filePath.getFileName().toString();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileName + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
