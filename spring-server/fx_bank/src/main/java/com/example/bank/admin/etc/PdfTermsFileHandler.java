package com.example.bank.admin.etc;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 약관 PDF 파일 저장 + 텍스트 추출 담당
 *
 * 흐름:
 *  1) 업로드된 PDF를 지정 폴더(기본: /data/terms-pdf)에 저장
 *  2) 저장된 PDF에서 PDFBox로 텍스트 추출
 *  3) (경로, 추출 텍스트)를 호출부에 반환 -> AdminTermsDto.pdfPath / termsText 에 세팅
 *
 * 약관코드(termsCode)별로 하위 폴더를 만들고, 버전이 바뀔 때마다(수정/재제출 등)
 * 새 파일명으로 저장하여 이전 버전 파일이 덮어써지지 않도록 한다.
 */
@Slf4j
@Component
public class PdfTermsFileHandler {

    // TODO: 운영 환경에서는 application.yml의 외부 설정값으로 분리 권장
    // 예: @Value("${terms.upload.base-dir}")
    // macOS 로컬 테스트용 경로 (홈 디렉터리 하위 - 권한 문제 없음)
    private static final String BASE_DIR = System.getProperty("user.home") + "/terms-pdf";

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024; // 20MB

    public PdfSaveResult saveAndExtract(String termsCode, MultipartFile file) {
        validate(file);

        Path savedPath = saveFile(termsCode, file);
        String extractedText = extractText(savedPath);

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
            Path dirPath = Paths.get(BASE_DIR, termsCode);
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

    private String extractText(Path pdfPath) {
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            if (document.isEncrypted()) {
                throw new IllegalStateException("암호화된 PDF는 텍스트를 추출할 수 없습니다.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String text = stripper.getText(document);
            log.info("[약관 PDF 텍스트 추출 완료] path={}, length={}", pdfPath, text.length());
            return text;
        } catch (IOException e) {
            throw new RuntimeException("PDF 텍스트 추출 중 오류가 발생했습니다.", e);
        }
    }

    /** PDF 저장 경로 + 추출된 텍스트를 담는 결과 객체 */
    public record PdfSaveResult(String pdfPath, String extractedText) {
    }
}