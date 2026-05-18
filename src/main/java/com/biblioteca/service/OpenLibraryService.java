package com.biblioteca.service;

import com.biblioteca.dto.OpenLibraryResponseDto;

public interface OpenLibraryService {

    OpenLibraryResponseDto buscarLibros(String query, Integer page);

}