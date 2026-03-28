package com.ocragent.service;

import com.ocragent.model.enums.FileType;
import com.ocragent.model.enums.RecognizeMode;
import com.ocragent.ocr.BaiduOcrClient;
import com.ocragent.ocr.model.OcrRawResult;
import com.ocragent.ocr.model.TextLine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrDispatchService {

    private final BaiduOcrClient ocrClient;
    private final FileProcessingService fileProcessingService;

    public OcrRawResult dispatch(byte[] fileData, FileType fileType, RecognizeMode mode) {
        if (fileType == FileType.PDF) {
            return handlePdf(fileData, mode);
        }
        return handleImage(fileData, mode);
    }

    private OcrRawResult handleImage(byte[] imageData, RecognizeMode mode) {
        return switch (mode) {
            case GENERAL -> ocrClient.accurate(imageData);
            case HANDWRITING -> ocrClient.handwriting(imageData);
            case AUTO -> autoRecognize(imageData);
            default -> ocrClient.accurate(imageData);
        };
    }

    private OcrRawResult autoRecognize(byte[] imageData) {
        OcrRawResult result = ocrClient.accurate(imageData);
        if (!result.isSuccess()) {
            return result;
        }

        double avgConfidence = result.getLines().stream()
                .mapToDouble(TextLine::getConfidence)
                .average()
                .orElse(0.9);

        if (avgConfidence < 0.75 && result.getLines().size() < 20) {
            log.info("通用识别置信度较低({}), 尝试手写识别", avgConfidence);
            OcrRawResult handwritingResult = ocrClient.handwriting(imageData);
            if (handwritingResult.isSuccess()) {
                double hwAvg = handwritingResult.getLines().stream()
                        .mapToDouble(TextLine::getConfidence)
                        .average()
                        .orElse(0);
                if (hwAvg > avgConfidence) {
                    return handwritingResult;
                }
            }
        }
        return result;
    }

    private OcrRawResult handlePdf(byte[] pdfData, RecognizeMode mode) {
        try {
            List<byte[]> pageImages = fileProcessingService.splitPdfToImages(pdfData);
            if (pageImages.isEmpty()) {
                return OcrRawResult.fail("PDF文件无内容页");
            }

            OcrRawResult merged = new OcrRawResult();
            merged.setSuccess(true);
            merged.setApiUsed("pdf_multi_page");
            merged.setLines(new ArrayList<>());

            for (int i = 0; i < pageImages.size(); i++) {
                OcrRawResult pageResult = handleImage(pageImages.get(i), mode);
                if (pageResult.isSuccess() && pageResult.getLines() != null) {
                    merged.getLines().addAll(pageResult.getLines());
                    if (pageResult.getTableRows() != null) {
                        if (merged.getTableRows() == null) {
                            merged.setTableRows(new ArrayList<>());
                        }
                        merged.getTableRows().addAll(pageResult.getTableRows());
                    }
                }
            }
            return merged;
        } catch (Exception e) {
            log.error("PDF处理异常", e);
            return OcrRawResult.fail("PDF处理失败: " + e.getMessage());
        }
    }
}
