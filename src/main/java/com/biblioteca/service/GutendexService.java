package com.biblioteca.service;

import com.biblioteca.dto.GutendexResponseDto;

public interface GutendexService {

    GutendexResponseDto buscarLibros(String query, Integer page);
}