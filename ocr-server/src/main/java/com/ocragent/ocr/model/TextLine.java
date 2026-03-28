package com.ocragent.ocr.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextLine {
    private String text;
    private double confidence;
    private int left;
    private int top;
    private int width;
    private int height;
}
