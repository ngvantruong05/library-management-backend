package edu.uet.library_management.infrastructure.service;

import edu.uet.library_management.domain.dto.PublisherDto;
import edu.uet.library_management.domain.model.Publisher;
import edu.uet.library_management.domain.service.PublisherService;
import edu.uet.library_management.infrastructure.persistence.PublisherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {

    private final PublisherRepository publisherRepository;

    @Override
    public List<PublisherDto> getAllPublishers() {
        return publisherRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PublisherDto getPublisherById(Long id) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publisher not found with id: " + id));
        return toDto(publisher);
    }

    @Override
    public PublisherDto createPublisher(PublisherDto publisherDto) {
        if (publisherRepository.existsByName(publisherDto.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publisher already exists with name: " + publisherDto.getName());
        }
        Publisher publisher = Publisher.builder()
                .name(publisherDto.getName())
                .address(publisherDto.getAddress())
                .phoneNumber(publisherDto.getPhoneNumber())
                .build();
        Publisher savedPublisher = publisherRepository.save(publisher);
        return toDto(savedPublisher);
    }

    @Override
    public PublisherDto updatePublisher(Long id, PublisherDto publisherDto) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publisher not found with id: " + id));

        if (!publisher.getName().equalsIgnoreCase(publisherDto.getName()) && publisherRepository.existsByName(publisherDto.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Publisher already exists with name: " + publisherDto.getName());
        }

        publisher.setName(publisherDto.getName());
        publisher.setAddress(publisherDto.getAddress());
        publisher.setPhoneNumber(publisherDto.getPhoneNumber());
        Publisher updatedPublisher = publisherRepository.save(publisher);
        return toDto(updatedPublisher);
    }

    @Override
    public void deletePublisher(Long id) {
        Publisher publisher = publisherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Publisher not found with id: " + id));
        publisherRepository.delete(publisher);
    }

    private PublisherDto toDto(Publisher publisher) {
        return PublisherDto.builder()
                .id(publisher.getId())
                .name(publisher.getName())
                .address(publisher.getAddress())
                .phoneNumber(publisher.getPhoneNumber())
                .build();
    }
}
