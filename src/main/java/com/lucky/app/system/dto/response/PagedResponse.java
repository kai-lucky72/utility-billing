package com.lucky.app.system.dto.response;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** Success envelope for paginated lists: the data page plus total elements/pages and current page/size. */
@Getter
@Builder
public class PagedResponse<T> {
    private boolean success;
    private String message;
    private List<T> data;
    private long totalElements;
    private int totalPages;
    private int page;
    private int size;
}
