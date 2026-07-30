package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.FineDto;

import java.util.List;

public interface FineService {
    List<FineDto> getMyFines(String email);
    List<FineDto> getAllFines();
    FineDto payFine(Long fineId);
}
