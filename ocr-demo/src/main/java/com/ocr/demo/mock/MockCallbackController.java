package com.ocr.demo.mock;

import com.ocr.common.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RestController
@RequestMapping("/api/demo/callback")
public class MockCallbackController {

    private final List<Map<String, Object>> receivedCallbacks = new CopyOnWriteArrayList<>();

    @PostMapping("/receive")
    public R<String> receive(@RequestBody Map<String, Object> payload) {
        log.info("收到OCR回调数据: taskNo={}, fileId={}", payload.get("taskNo"), payload.get("fileId"));

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("receivedAt", LocalDateTime.now().toString());
        record.put("payload", payload);
        receivedCallbacks.add(record);

        return R.ok("success");
    }

    @GetMapping("/list")
    public R<List<Map<String, Object>>> list() {
        return R.ok(new ArrayList<>(receivedCallbacks));
    }

    @DeleteMapping("/clear")
    public R<String> clear() {
        receivedCallbacks.clear();
        return R.ok("已清空");
    }
}
