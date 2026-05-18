package com.biblioteca.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GutendexBookDto {

    private Integer id;

    private String title;

    private List<AuthorDto> authors;

    private List<String> languages;

    private Map<String, String> formats;

    @JsonProperty("download_count")
    private Integer downloadCount;
}