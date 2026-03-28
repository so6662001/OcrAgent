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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class FileProcessingService {

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    private static final List<String> IMAGE_TYPES = List.of(
            "image/jpeg", "image/png", "image/bmp", "image/tiff", "image/webp"
    );
    private static final String PDF_TYPE = "application/pdf";

    public FileType detectFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.equals(PDF_TYPE)) {
            return FileType.PDF;
        }
        String fileName = file.getOriginalFilename();
        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            return FileType.PDF;
        }
        return FileType.IMAGE;
    }

    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        boolean valid = (contentType != null && (IMAGE_TYPES.contains(contentType) || PDF_TYPE.equals(contentType)));
        if (!valid && fileName != null) {
            String lower = fileName.toLowerCase();
            valid = lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
                    || lower.endsWith(".bmp") || lower.endsWith(".tiff") || lower.endsWith(".pdf");
        }
        if (!valid) {
            throw new IllegalArgumentException("不支持的文件格式，请上传 JPG/PNG/BMP/TIFF/PDF 文件");
        }
    }

    public String saveFile(MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        String ext = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }
        String savedName = UUID.randomUUID() + ext;
        Path target = dir.resolve(savedName);
        file.transferTo(target.toFile());
        return target.toString();
    }

    public List<byte[]> splitPdfToImages(byte[] pdfData) throws IOException {
        List<byte[]> images = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdfData)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 200);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                images.add(baos.toByteArray());
            }
        }
        return images;
    }
}
