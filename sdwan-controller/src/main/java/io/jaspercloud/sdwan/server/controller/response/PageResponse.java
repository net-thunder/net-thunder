package io.jaspercloud.sdwan.server.controller.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PageResponse<T> {

    private Long total;
    private Long current;
    private Long size;
    private List<T> data;

    public static <T> PageResponse<T> build(List<T> data, Long total, Long size, Long current) {
        PageResponse<T> response = new PageResponse<>();
        response.setData(data);
        response.setTotal(total);
        response.setSize(size);
        response.setCurrent(current);
        return response;
    }
}
