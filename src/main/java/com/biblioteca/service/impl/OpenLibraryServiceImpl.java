package com.biblioteca.service.impl;

import com.biblioteca.dto.OpenLibraryResponseDto;
import com.biblioteca.service.OpenLibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class OpenLibraryServiceImpl implements OpenLibraryService {

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String BASE_URL = "https://openlibrary.org/search.json";
    private static final int PAGE_SIZE = 15;

    @Override
    public OpenLibraryResponseDto buscarLibros(String query, Integer page) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(BASE_URL);

        // ✅ sin query, usar "subject:fiction" que sí está permitido
        if (query != null && !query.isBlank()) {
            builder.queryParam("q", query);
        } else {
            builder.queryParam("subject", "fiction"); // ✅ trae libros generales sin error
        }

        builder.queryParam("page", page);
        builder.queryParam("limit", PAGE_SIZE);

        String url = builder.toUriString();

        return restTemplate.getForObject(url, OpenLibraryResponseDto.class);
    }
}