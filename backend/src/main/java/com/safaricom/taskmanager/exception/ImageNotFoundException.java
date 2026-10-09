package com.safaricom.taskmanager.exception;

public class ImageNotFoundException extends RuntimeException {

    public ImageNotFoundException(Long id) {
        super("Image with id " + id + " was not found");
    }
}
