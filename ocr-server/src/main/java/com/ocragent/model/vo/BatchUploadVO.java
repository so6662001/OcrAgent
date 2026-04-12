package com.ocragent.model.vo;

import lombok.Data;

import java.util.List;

@Data
public class BatchUploadVO {
    private int totalFiles;
    private List<String> taskIds;
}
