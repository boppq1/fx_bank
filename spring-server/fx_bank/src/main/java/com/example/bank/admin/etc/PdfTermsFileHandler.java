package com.example.bank.admin.etc;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import technology.tabula.*;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;
import technology.tabula.extractors.BasicExtractionAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class PdfTermsFileHandler {

    @Value("${app.file.terms-dir:uploads/terms}")
    private String termsDir;

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;

    public PdfSaveResult saveAndExtract(String termsCode, MultipartFile file) {
        validate(file);
        Path savedPath = saveFile(termsCode, file);
        String extractedText = extractTextWithTables(savedPath);
        return new PdfSaveResult(savedPath.toString(), extractedText);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드된 PDF 파일이 없습니다.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("PDF 파일 크기는 20MB를 초과할 수 없습니다.");
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("PDF 파일만 업로드할 수 있습니다.");
        }
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equalsIgnoreCase("application/pdf")) {
            throw new IllegalArgumentException("PDF 형식의 파일만 업로드할 수 있습니다.");
        }
    }

    private Path saveFile(String termsCode, MultipartFile file) {
        try {
            Path dirPath = Paths.get(termsDir, termsCode);
            Files.createDirectories(dirPath);

            String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            String randomPart = UUID.randomUUID().toString().substring(0, 8);
            String fileName = termsCode + "_" + datePart + "_" + randomPart + ".pdf";
            Path targetPath = dirPath.resolve(fileName);

            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("[약관 PDF 저장] termsCode={}, path={}", termsCode, targetPath);
            return targetPath;
        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 저장 중 오류가 발생했습니다.", e);
        }
    }

    private String extractTextWithTables(Path pdfPath) {
        try (PDDocument document = PDDocument.load(pdfPath.toFile())) {
            if (document.isEncrypted()) {
                throw new IllegalStateException("암호화된 PDF는 텍스트를 추출할 수 없습니다.");
            }

            ObjectExtractor extractor = new ObjectExtractor(document);
            SpreadsheetExtractionAlgorithm sea = new SpreadsheetExtractionAlgorithm();
            BasicExtractionAlgorithm bea = new BasicExtractionAlgorithm();
            StringBuilder result = new StringBuilder();
            int totalPages = document.getNumberOfPages();

            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                Page page = extractor.extract(pageNum);

                List<Table> tables = sea.extract(page);
                if (tables.isEmpty()) {
                    tables = bea.extract(page);
                }

                if (!tables.isEmpty()) {
                    String pageText = extractPageText(document, pageNum);
                    String tableText = tablesToText(tables);
                    result.append(postProcess(pageText));
                    result.append("\n\n[표]\n").append(tableText).append("\n");
                } else {
                    String pageText = extractPageText(document, pageNum);
                    result.append(postProcess(pageText));
                }

                result.append("\n\n");
            }

            extractor.close();
            String finalText = result.toString().strip();
            log.info("[약관 PDF 추출 완료] path={}, length={}", pdfPath, finalText.length());
            return finalText;

        } catch (IOException e) {
            throw new RuntimeException("PDF 텍스트 추출 중 오류가 발생했습니다.", e);
        }
    }

    private String extractPageText(PDDocument document, int pageNum) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setSpacingTolerance(0.4f);
        stripper.setAverageCharTolerance(0.3f);
        stripper.setLineSeparator("\n");
        stripper.setParagraphEnd("\n\n");
        stripper.setStartPage(pageNum);
        stripper.setEndPage(pageNum);
        return stripper.getText(document);
    }

    private String tablesToText(List<Table> tables) {
        StringBuilder sb = new StringBuilder();
        for (Table table : tables) {
            List<List<RectangularTextContainer>> rows = table.getRows();
            for (List<RectangularTextContainer> row : rows) {
                StringBuilder rowSb = new StringBuilder();
                for (RectangularTextContainer cell : row) {
                    String cellText = cell.getText().trim().replaceAll("\\s+", " ");
                    rowSb.append(cellText).append(" | ");
                }
                if (rowSb.length() > 3) {
                    rowSb.setLength(rowSb.length() - 3);
                }
                sb.append(rowSb).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String postProcess(String raw) {
        return raw
                .replace("\t", " ")
                .replaceAll("[ ]{2,}", " ")
                .replaceAll("(\n\\s*){3,}", "\n\n")
                .replaceAll("(?m)(?<!\n)(제\\d+조|\\d+\\.\\s|[①-⑳]|[가-힣]\\s*\\.)", "\n$1")
                .replaceAll("-\n([가-힣a-zA-Z])", "$1")
                .strip();
    }

    public record PdfSaveResult(String pdfPath, String extractedText) {}
}