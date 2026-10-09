package com.safaricom.taskmanager.dto;

/** Returned after an upload; {@code url} is what the editor puts in the image's src. */
public record ImageResponse(Long id, String url) {
}
