package com.safaricom.taskmanager.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safaricom.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TaskControllerTest {

    private static final String TASKS = "/api/tasks";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void clearDatabase() {
        taskRepository.deleteAll();
    }

    // ---- POST /api/tasks ----

    @Test
    void createReturns201WithBodyAndLocation() throws Exception {
        postTask("{\"title\":\"Prepare report\",\"description\":\"Draft weekly report\",\"status\":\"TODO\"}")
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/tasks/" + idOfLatest())))
                .andExpect(jsonPath("$.id").value(idOfLatest()))
                .andExpect(jsonPath("$.title").value("Prepare report"))
                .andExpect(jsonPath("$.description").value("Draft weekly report"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    @Test
    void createDefaultsStatusToTodo() throws Exception {
        postTask("{\"title\":\"No status\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void createTrimsTitleAndStoresBlankDescriptionAsNull() throws Exception {
        postTask("{\"title\":\"  Padded title  \",\"description\":\"   \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Padded title"))
                .andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void createAcceptsBoundaryLengths() throws Exception {
        postTask(json("abc", "d".repeat(500), "DONE")).andExpect(status().isCreated());
        postTask(json("t".repeat(100), null, "IN_PROGRESS")).andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"title\":\"\"}",
            "{\"title\":\"   \"}",
            "{\"title\":\"ab\"}",
            "{\"title\":\"  ab  \"}",
            "{\"title\":\"Valid\",\"status\":\"UNKNOWN\"}",
            "{\"title\":\"Valid\",\"status\":\"todo\"}",
            "{not json"
    })
    void createRejectsInvalidInputWith400(String body) throws Exception {
        expectError(postTask(body), 400);
    }

    @Test
    void createReportsOneClearMessagePerTitleProblem() throws Exception {
        expectError(postTask("{\"description\":\"no title\"}"), 400)
                .andExpect(jsonPath("$.message").value("title is required"));
        expectError(postTask("{\"title\":\"   \"}"), 400)
                .andExpect(jsonPath("$.message").value("title must be between 3 and 100 characters"));
    }

    @Test
    void createRejectsTitleOver100AndDescriptionOver500() throws Exception {
        expectError(postTask(json("t".repeat(101), null, null)), 400);
        expectError(postTask(json("Valid", "d".repeat(501), null)), 400);
    }

    // ---- Rich-text description ----

    @Test
    void createKeepsAllowedFormatting() throws Exception {
        String html = "<p><strong>Bold</strong> <em>italic</em> <u>under</u> <s>strike</s> <code>code</code></p>"
                + "<ul><li><p>item</p></li></ul>"
                + "<p><span style=\"font-family: Georgia, serif; font-size: 18px\">styled</span></p>";

        postTask(json("Formatted task", html, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(html));
    }

    @Test
    void createKeepsHeadingsQuotesAndDividers() throws Exception {
        String html = "<h1>Plan</h1><h2>Goals</h2><h3>Details</h3>"
                + "<blockquote><p>Ship on Friday</p></blockquote><hr><p>Done</p>";

        postTask(json("Structured task", html, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(html));
    }

    @Test
    void createRemovesUnsafeMarkup() throws Exception {
        String html = "<p onclick=\"alert(1)\">Hello<script>alert(1)</script>"
                + "<a href=\"javascript:alert(1)\">link</a>"
                + "<span style=\"background: url(javascript:alert(1)); font-size: 18px\">x</span></p>";

        postTask(json("Unsafe task", html, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("<p>Hellolink<span style=\"font-size: 18px\">x</span></p>"));
    }

    @Test
    void createKeepsImagesFromTheImageEndpointOnly() throws Exception {
        String html = "<p>Screenshot:</p>"
                + "<img src=\"http://localhost:8080/api/images/7\" alt=\"chart\">"
                + "<img src=\"http://localhost:8080/somewhere/else.png\">"
                + "<img src=\"javascript:alert(1)\">"
                + "<img src=\"data:image/png;base64,AAAA\">";

        postTask(json("With images", html, null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description")
                        .value("<p>Screenshot:</p><img src=\"http://localhost:8080/api/images/7\" alt=\"chart\">"));
    }

    @Test
    void createKeepsDescriptionThatIsOnlyAnImage() throws Exception {
        postTask(json("Image only", "<p><img src=\"http://localhost:8080/api/images/3\"></p>", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("<p><img src=\"http://localhost:8080/api/images/3\"></p>"));
    }

    @Test
    void createStoresEmptyEditorContentAsNull() throws Exception {
        postTask(json("Empty editor", "<p></p>", null))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value(nullValue()));
    }

    @Test
    void descriptionLimitCountsVisibleTextNotMarkup() throws Exception {
        postTask(json("Exactly 500", "<p><strong>" + "d".repeat(500) + "</strong></p>", null))
                .andExpect(status().isCreated());

        expectError(postTask(json("Over 500", "<p><strong>" + "d".repeat(501) + "</strong></p>", null)), 400)
                .andExpect(jsonPath("$.message").value("description must be at most 500 characters"));
    }

    // ---- GET /api/tasks and /api/tasks/{id} ----

    @Test
    void listReturnsAllTasksInCreationOrder() throws Exception {
        postTask(json("First task", null, null));
        postTask(json("Second task", null, null));

        mockMvc.perform(get(TASKS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title").value("First task"))
                .andExpect(jsonPath("$[1].title").value("Second task"));
    }

    @Test
    void getOneReturns200() throws Exception {
        long id = createTask("Read me");

        mockMvc.perform(get(TASKS + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Read me"));
    }

    @Test
    void getMissingReturns404() throws Exception {
        expectError(mockMvc.perform(get(TASKS + "/999")), 404)
                .andExpect(jsonPath("$.message").value("Task with id 999 was not found"));
    }

    @Test
    void getWithNonNumericIdReturns400() throws Exception {
        expectError(mockMvc.perform(get(TASKS + "/abc")), 400);
    }

    // ---- PUT /api/tasks/{id} ----

    @Test
    void updateReturns200AndPersistsChanges() throws Exception {
        long id = createTask("Old title");

        putTask(id, json("New title", "Now with details", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.description").value("Now with details"))
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(get(TASKS + "/" + id))
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    void updateWithoutStatusKeepsCurrentStatus() throws Exception {
        long id = createTask("Some task");
        putTask(id, json("Some task", null, "IN_PROGRESS")).andExpect(status().isOk());

        putTask(id, json("Renamed task", null, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void updateWithInvalidBodyReturns400() throws Exception {
        long id = createTask("Valid title");
        expectError(putTask(id, json("x", null, null)), 400);
    }

    @Test
    void updateMissingReturns404() throws Exception {
        expectError(putTask(999, json("Valid title", null, null)), 404);
    }

    // ---- DELETE /api/tasks/{id} ----

    @Test
    void deleteReturns204AndRemovesTask() throws Exception {
        long id = createTask("Delete me");

        mockMvc.perform(delete(TASKS + "/" + id))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        mockMvc.perform(get(TASKS + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteMissingReturns404() throws Exception {
        expectError(mockMvc.perform(delete(TASKS + "/999")), 404);
    }

    // ---- CORS ----

    @Test
    void corsAllowsReactDevServer() throws Exception {
        mockMvc.perform(options(TASKS)
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }

    // ---- helpers ----

    private ResultActions postTask(String body) throws Exception {
        return mockMvc.perform(post(TASKS).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions putTask(long id, String body) throws Exception {
        return mockMvc.perform(put(TASKS + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private long createTask(String title) throws Exception {
        String response = postTask(json(title, null, null)).andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return node.get("id").asLong();
    }

    private long idOfLatest() {
        return taskRepository.findAll().stream()
                .mapToLong(task -> task.getId())
                .max()
                .orElseThrow();
    }

    private String json(String title, String description, String status) throws Exception {
        var body = new java.util.LinkedHashMap<String, Object>();
        body.put("title", title);
        body.put("description", description);
        body.put("status", status);
        return objectMapper.writeValueAsString(body);
    }

    /** Asserts the status code and the consistent error body shape. */
    private ResultActions expectError(ResultActions result, int expectedStatus) throws Exception {
        return result
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.error").value(notNullValue()))
                .andExpect(jsonPath("$.message").value(notNullValue()))
                .andExpect(jsonPath("$.path").value(notNullValue()))
                .andExpect(jsonPath("$.timestamp").value(notNullValue()));
    }
}
