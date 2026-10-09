package com.safaricom.taskmanager.service;

import com.safaricom.taskmanager.exception.ImageNotFoundException;
import com.safaricom.taskmanager.exception.InvalidImageException;
import com.safaricom.taskmanager.model.Image;
import com.safaricom.taskmanager.repository.ImageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Transactional
public class ImageService {

    static final long MAX_BYTES = 2 * 1024 * 1024;

    private final ImageRepository imageRepository;

    public ImageService(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    public Image store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException("file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidImageException("image must be at most 2 MB");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new InvalidImageException("image could not be read");
        }
        // Trust the file's bytes, not the client's Content-Type header.
        String contentType = detectContentType(data);
        if (contentType == null) {
            throw new InvalidImageException("image must be a PNG, JPEG, GIF or WebP file");
        }
        return imageRepository.save(new Image(contentType, data));
    }

    @Transactional(readOnly = true)
    public Image get(Long id) {
        return imageRepository.findById(id).orElseThrow(() -> new ImageNotFoundException(id));
    }

    private static String detectContentType(byte[] d) {
        if (startsWith(d, 0x89, 'P', 'N', 'G')) {
            return "image/png";
        }
        if (startsWith(d, 0xFF, 0xD8, 0xFF)) {
            return "image/jpeg";
        }
        if (startsWith(d, 'G', 'I', 'F', '8')) {
            return "image/gif";
        }
        if (startsWith(d, 'R', 'I', 'F', 'F') && d.length >= 12
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
