package com.biblioteca.dto;

import lombok.Data;
import java.util.List;

@Data
public class GutendexResponseDto {

    private Integer count;
    private String next;
    private String previous;

    private List<GutendexBookDto> results;
}