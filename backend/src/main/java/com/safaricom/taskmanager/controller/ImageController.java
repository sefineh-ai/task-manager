package com.safaricom.taskmanager.controller;

import com.safaricom.taskmanager.dto.ImageResponse;
import com.safaricom.taskmanager.model.Image;
import com.safaricom.taskmanager.service.ImageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;

/** Images embedded in task descriptions by the rich-text editor. */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> upload(@RequestParam(value = "file", required = false) MultipartFile file) {
        Image image = imageService.store(file);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/images/{id}")
                .buildAndExpand(image.getId())
                .toUri();
        return ResponseEntity.created(location).body(new ImageResponse(image.getId(), location.toString()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> get(@PathVariable Long id) {
        Image image = imageService.get(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getContentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)))
                .header("X-Content-Type-Options", "nosniff")
                .body(image.getData());
    }
}
