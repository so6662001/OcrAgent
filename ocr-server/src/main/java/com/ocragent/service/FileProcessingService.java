package com.ocragent.service;

import com.ocragent.model.enums.FileType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class FileProcessingService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final int MAX_BATCH_SIZE = 20;
    private static final int MAX_PDF_PAGES = 50;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/bmp", "image/tiff", "image/webp", "application/pdf"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".bmp", ".tiff", ".pdf"
    );

    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] PDF_MAGIC = {0x25, 0x50, 0x44, 0x46}; // %PDF
    private static final byte[] BMP_MAGIC = {0x42, 0x4D};             // BM
    private static final byte[] TIFF_LE_MAGIC = {0x49, 0x49, 0x2A, 0x00};
    private static final byte[] TIFF_BE_MAGIC = {0x4D, 0x4D, 0x00, 0x2A};

    public FileType detectFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if ("application/pdf".equals(contentType)) {
            return FileType.PDF;
        }
        String ext = getSafeExtension(file.getOriginalFilename());
        if (".pdf".equals(ext)) {
            return FileType.PDF;
        }
        return FileType.IMAGE;
    }

    public void validateBatchSize(int fileCount) {
        if (fileCount > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("单次最多上传" + MAX_BATCH_SIZE + "个文件");
        }
    }

    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小超出限制（最大20MB）");
        }

        String ext = getSafeExtension(file.getOriginalFilename());
        String contentType = file.getContentType();

        boolean extValid = ext != null && ALLOWED_EXTENSIONS.contains(ext);
        boolean ctValid = contentType != null && ALLOWED_CONTENT_TYPES.contains(contentType);

        if (!extValid && !ctValid) {
            throw new IllegalArgumentException("不支持的文件格式，请上传 JPG/PNG/BMP/TIFF/PDF 文件");
        }

        try {
            byte[] header = peekHeader(file, 8);
            if (!isAllowedMagicBytes(header)) {
                throw new IllegalArgumentException("文件内容与声明格式不匹配");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("无法读取文件内容");
        }
    }

    public String saveFile(MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String ext = getSafeExtension(file.getOriginalFilename());
        if (ext == null) ext = ".bin";

        String savedName = UUID.randomUUID() + ext;
        Path target = dir.resolve(savedName).normalize();

        if (!target.startsWith(dir)) {
            throw new SecurityException("非法的文件路径");
        }

        file.transferTo(target.toFile());
        return savedName;
    }

    public List<byte[]> splitPdfToImages(byte[] pdfData) throws IOException {
        List<byte[]> images = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdfData)) {
            int pageCount = doc.getNumberOfPages();
            if (pageCount > MAX_PDF_PAGES) {
                throw new IllegalArgumentException("PDF页数超出限制（最大" + MAX_PDF_PAGES + "页）");
            }
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < pageCount; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 150);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                images.add(baos.toByteArray());
            }
        }
        return images;
    }

    private String getSafeExtension(String fileName) {
        if (fileName == null) return null;
        String safeName = fileName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        int dotIdx = safeName.lastIndexOf('.');
        if (dotIdx < 0) return null;
        String ext = safeName.substring(dotIdx).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext) ? ext : null;
    }

    private byte[] peekHeader(MultipartFile file, int size) throws IOException {
        try (var is = file.getInputStream()) {
            return is.readNBytes(size);
        }
    }

    private boolean isAllowedMagicBytes(byte[] header) {
        if (header.length < 2) return false;
        return startsWith(header, JPEG_MAGIC)
                || startsWith(header, PNG_MAGIC)
                || startsWith(header, PDF_MAGIC)
                || startsWith(header, BMP_MAGIC)
                || startsWith(header, TIFF_LE_MAGIC)
                || startsWith(header, TIFF_BE_MAGIC);
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }
}
