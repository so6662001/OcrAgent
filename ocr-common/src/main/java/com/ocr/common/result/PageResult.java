package com.ocr.common.result;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private long pages;
    private List<T> list;

    public static <T> PageResult<T> of(long total, long pages, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPages(pages);
        result.setList(list);
        return result;
    }
}
