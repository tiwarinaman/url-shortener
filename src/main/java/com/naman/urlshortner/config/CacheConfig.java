package com.naman.urlshortner.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Value("${cache.urls.maximum-size}")
    private long maximumSize;

    @Value("${cache.urls.expiration-time}")
    private long expirationTime;

    @Bean
    public CacheManager cacheManager() {
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(expirationTime, TimeUnit.SECONDS);

        return new CaffeineCacheManager("urls") {
            {
                setCaffeine(caffeineBuilder);
            }
        };
    }


}
