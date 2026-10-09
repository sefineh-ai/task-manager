package com.safaricom.taskmanager.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ImageControllerTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    @Autowired
    private MockMvc mockMvc;

    @Test
    void uploadReturns201WithUrlAndServesTheImage() throws Exception {
        String url = upload("photo.png", "image/png", PNG)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("http://localhost/api/images/\\d+")))
                .andExpect(jsonPath("$.id").value(notNullValue()))
                .andExpect(jsonPath("$.url").value(matchesPattern("http://localhost/api/images/\\d+")))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void uploadUsesDetectedTypeNotClaimedType() throws Exception {
        String url = upload("photo.jpg", "image/jpeg", PNG)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(get(url)).andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void uploadRejectsNonImages() throws Exception {
        expectError(upload("notes.png", "image/png", "just text".getBytes(StandardCharsets.UTF_8)), 400);
    }

    @Test
    void uploadRejectsSvg() throws Exception {
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>"
                .getBytes(StandardCharsets.UTF_8);
        expectError(upload("evil.svg", "image/svg+xml", svg), 400);
    }

    @Test
    void uploadRejectsEmptyAndMissingFile() throws Exception {
        expectError(upload("empty.png", "image/png", new byte[0]), 400);
        expectError(mockMvc.perform(multipart("/api/images")), 400);
    }

    @Test
    void uploadRejectsFilesOver2Mb() throws Exception {
        byte[] big = new byte[2 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, big, 0, PNG.length);
        expectError(upload("big.png", "image/png", big), 400);
    }

    @Test
    void getMissingImageReturns404() throws Exception {
        expectError(mockMvc.perform(get("/api/images/999999")), 404);
    }

    private ResultActions upload(String name, String contentType, byte[] bytes) throws Exception {
        return mockMvc.perform(multipart("/api/images").file(new MockMultipartFile("file", name, contentType, bytes)));
    }

    private ResultActions expectError(ResultActions result, int expectedStatus) throws Exception {
        return result
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.message").value(notNullValue()));
    }
}
