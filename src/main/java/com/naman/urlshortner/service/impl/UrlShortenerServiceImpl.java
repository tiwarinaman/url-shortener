package com.naman.urlshortner.service.impl;

import com.naman.urlshortner.domain.entity.UrlMapping;
import com.naman.urlshortner.domain.response.UrlShortenResponse;
import com.naman.urlshortner.encoder.Base64;
import com.naman.urlshortner.exception.ResourceNotFoundException;
import com.naman.urlshortner.repository.UrlMappingRepository;
import com.naman.urlshortner.service.UrlShortenerService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UrlShortenerServiceImpl implements UrlShortenerService {

    private final UrlMappingRepository urlMappingRepository;

    @CachePut(value = "urls", key = "#originalUrl")
    @Override
    public String shortenUrl(String originalUrl, int expirationHours) {

        // Remove any unnecessary prepares or repeated save calls
        var existingUrl = urlMappingRepository.findByOriginalUrl(originalUrl).orElse(null);

        if (existingUrl != null) {
            return Base64.encode(existingUrl.getId());
        }

        LocalDateTime expirationTime = LocalDateTime.now().plusHours(expirationHours);

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(originalUrl);
        urlMapping.setExpirationTime(expirationTime);

        try {
            // Save the URL entity, which auto-generates the ID
            urlMapping = urlMappingRepository.save(urlMapping);
            // Encode the ID in Base64
            return Base64.encode(urlMapping.getId());
        } catch (OptimisticLockingFailureException e) {
            throw new RuntimeException("Failed to shorten URL due to concurrent modification.");
        }
    }

    @Cacheable(value = "urls", key = "#shortUrl")
    @Override
    public Optional<UrlShortenResponse> fetchOriginalUrl(String shortUrl) {

        long id = Base64.decode(shortUrl);

        UrlMapping urlMapping = urlMappingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("URL mapping not found"));

        if (urlMapping.getExpirationTime().isBefore(LocalDateTime.now())) {
            urlMappingRepository.delete(urlMapping);
            throw new ResourceNotFoundException("URL mapping not found");
        }

        return Optional.ofNullable(
                UrlShortenResponse.builder()
                        .originalUrl(urlMapping.getOriginalUrl())
                        .expirationTime(urlMapping.getExpirationTime())
                        .build()
        );
    }
}
