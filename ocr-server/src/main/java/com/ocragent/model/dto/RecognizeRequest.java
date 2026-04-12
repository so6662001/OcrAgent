package com.ocragent.model.dto;

import com.ocragent.model.enums.RecognizeMode;
import lombok.Data;

@Data
public class RecognizeRequest {
    private RecognizeMode mode = RecognizeMode.AUTO;
}
