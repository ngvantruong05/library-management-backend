package edu.uet.library_management.domain.service;

import edu.uet.library_management.domain.dto.PublisherDto;

import java.util.List;

public interface PublisherService {
    List<PublisherDto> getAllPublishers();
    PublisherDto getPublisherById(Long id);
    PublisherDto createPublisher(PublisherDto publisherDto);
    PublisherDto updatePublisher(Long id, PublisherDto publisherDto);
    void deletePublisher(Long id);
}
