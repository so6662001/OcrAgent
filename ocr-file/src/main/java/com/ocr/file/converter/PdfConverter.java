package com.ocr.file.converter;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class PdfConverter {

    /**
     * 将PDF每页转为PNG图片字节数组
     */
    public List<byte[]> pdfToImages(InputStream pdfStream, int dpi) {
        List<byte[]> images = new ArrayList<>();
        try (PDDocument document = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int i = 0; i < document.getNumberOfPages(); i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, dpi);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                images.add(baos.toByteArray());
            }
        } catch (Exception e) {
            log.error("PDF转图片失败", e);
        }
        return images;
    }

    public int getPageCount(InputStream pdfStream) {
        try (PDDocument document = Loader.loadPDF(pdfStream.readAllBytes())) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * 检查PDF是否包含可提取文本（非扫描PDF）
     */
    public boolean hasExtractableText(InputStream pdfStream) {
        try (PDDocument document = Loader.loadPDF(pdfStream.readAllBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document).trim();
            return !text.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
