package com.biblioteca.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryBookDto {

    @JsonProperty("key")
    private String key;

    @JsonProperty("title")
    private String title;

    @JsonProperty("author_name")
    private List<String> authorName;

    @JsonProperty("language")
    private List<String> languages;

    @JsonProperty("first_publish_year")
    private Integer firstPublishYear;

    @JsonProperty("edition_count")
    private Integer editionCount;

    @JsonProperty("cover_i") 
    private Long coverId;
}