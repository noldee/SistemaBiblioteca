package com.biblioteca.service.impl;

import com.biblioteca.dto.GutendexResponseDto;
import com.biblioteca.service.GutendexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GutendexServiceImpl implements GutendexService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public GutendexResponseDto buscarLibros(String query, Integer page) {

        String url = "https://gutendex.com/books/?page=" + page;

        if (query != null && !query.isBlank()) {
            url += "&search=" + query;
        }

        return restTemplate.getForObject(url, GutendexResponseDto.class);
    }
}