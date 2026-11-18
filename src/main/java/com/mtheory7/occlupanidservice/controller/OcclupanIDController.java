package com.mtheory7.occlupanidservice.controller;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import com.mtheory7.occlupanidservice.entites.PredictionResponse;
import com.mtheory7.occlupanidservice.service.OcclupanIDService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class OcclupanIDController {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final OcclupanIDService classifierService;

    public OcclupanIDController(OcclupanIDService classifierService) {
        this.classifierService = classifierService;
    }

    @PostMapping(value = "/predict", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public PredictionResponse predict(@RequestParam("image") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded image is empty.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Only JPEG, PNG, or WebP images are supported.");
        }

        try (InputStream is = file.getInputStream()) {
            return classifierService.classify(is);
        }
    }
}
