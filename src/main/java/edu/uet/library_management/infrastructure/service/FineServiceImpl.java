package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.FineDto;
import edu.uet.library_management.domain.enums.FineStatus;
import edu.uet.library_management.domain.model.Fine;
import edu.uet.library_management.domain.service.FineService;
import edu.uet.library_management.infrastructure.persistence.FineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FineServiceImpl implements FineService {

    private final FineRepository fineRepository;

    @Override
    public List<FineDto> getMyFines(String email) {
        return fineRepository.findByBookLoanUserEmail(email).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<FineDto> getAllFines() {
        return fineRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public FineDto payFine(Long fineId) {
        Fine fine = fineRepository.findById(fineId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fine record not found with id: " + fineId));
        
        if (fine.getStatus() == FineStatus.PAID) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fine has already been paid");
        }

        fine.setStatus(FineStatus.PAID);
        Fine saved = fineRepository.save(fine);
        return toDto(saved);
    }

    private FineDto toDto(Fine fine) {
        return FineDto.builder()
                .id(fine.getId())
                .bookLoanId(fine.getBookLoan().getId())
                .bookId(fine.getBookLoan().getBook().getId())
                .bookThumbnail(fine.getBookLoan().getBook().getThumbnail())
                .userEmail(fine.getBookLoan().getUser().getEmail())
                .userDisplayName(fine.getBookLoan().getUser().getDisplayName())
                .bookTitle(fine.getBookLoan().getBook().getTitle())
                .fineAmount(fine.getFineAmount())
                .overdueDays(fine.getOverdueDays())
                .status(fine.getStatus())
                .createdAt(fine.getCreatedAt())
                .build();
    }
}
