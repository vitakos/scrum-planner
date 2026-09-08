package com.scrumplanner.core.intakesession;

import com.scrumplanner.core.gapanalysis.GapAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers AISC-19: POST /api/projects/{projectId}/intake-session/gap-analysis calls
 * GapAnalysisService, persists its result as a GAP_ANALYSIS-kind message via
 * IntakeSessionService, and returns the updated session.
 *
 * <p>Also covers AISC-20: POST .../gap-analysis/follow-up persists both the user's
 * follow-up and the assistant's reply, and surfaces a 400 (via the shared
 * ApiExceptionHandler, loaded as part of this @WebMvcTest slice) when
 * GapAnalysisService rejects a follow-up asked before any gap analysis exists.
 *
 * <p>Also covers AISC-21: GET .../intake-session (the restore path used when the
 * popover is reopened) carries a persisted gap-analysis message's kind through
 * intact, and defaults it to USER for messages saved before the kind field existed.
 */
@WebMvcTest(IntakeSessionController.class)
class IntakeSessionControllerTest {

    private static final String PROJECT_ID = "project-1";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IntakeSessionService sessionService;

    @MockBean
    private IntakeAttachmentStorage attachmentStorage;

    @MockBean
    private GapAnalysisService gapAnalysisService;

    @Test
    void generateGapAnalysisPersistsResultAndReturnsUpdatedSession() throws Exception {
        when(gapAnalysisService.generateGapAnalysis(PROJECT_ID)).thenReturn("Here is the gap analysis.");

        IntakeSessionDocument session = new IntakeSessionDocument(PROJECT_ID);
        session.setMessages(List.of(
                new IntakeSessionDocument.IntakeMessage(
                        "msg-1", "Assistant", "Here is the gap analysis.", OffsetDateTime.now(),
                        IntakeSessionDocument.MessageKind.GAP_ANALYSIS)
        ));
        when(sessionService.addMessage(
                eq(PROJECT_ID), eq("Assistant"), eq("Here is the gap analysis."),
                eq(IntakeSessionDocument.MessageKind.GAP_ANALYSIS)))
                .thenReturn(session);

        mockMvc.perform(post("/api/projects/{projectId}/intake-session/gap-analysis", PROJECT_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messages[0].sender").value("Assistant"))
                .andExpect(jsonPath("$.messages[0].text").value("Here is the gap analysis."));
    }

    @Test
    void answerFollowUpPersistsBothMessagesAndReturnsUpdatedSession() throws Exception {
        when(gapAnalysisService.answerFollowUp(PROJECT_ID, "What about password reset?"))
                .thenReturn("Here is the follow-up answer.");

        IntakeSessionDocument sessionAfterFollowUp = new IntakeSessionDocument(PROJECT_ID);
        sessionAfterFollowUp.setMessages(List.of(
                new IntakeSessionDocument.IntakeMessage(
                        "msg-1", "You", "What about password reset?", OffsetDateTime.now(),
                        IntakeSessionDocument.MessageKind.USER),
                new IntakeSessionDocument.IntakeMessage(
                        "msg-2", "Assistant", "Here is the follow-up answer.", OffsetDateTime.now(),
                        IntakeSessionDocument.MessageKind.FOLLOW_UP)
        ));
        when(sessionService.addMessage(
                eq(PROJECT_ID), eq("Assistant"), eq("Here is the follow-up answer."),
                eq(IntakeSessionDocument.MessageKind.FOLLOW_UP)))
                .thenReturn(sessionAfterFollowUp);

        mockMvc.perform(post("/api/projects/{projectId}/intake-session/gap-analysis/follow-up", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sender\":\"You\",\"text\":\"What about password reset?\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messages[0].sender").value("You"))
                .andExpect(jsonPath("$.messages[1].sender").value("Assistant"))
                .andExpect(jsonPath("$.messages[1].text").value("Here is the follow-up answer."));

        verify(sessionService).addMessage(
                PROJECT_ID, "You", "What about password reset?", IntakeSessionDocument.MessageKind.USER);
        verify(sessionService).addMessage(
                PROJECT_ID, "Assistant", "Here is the follow-up answer.", IntakeSessionDocument.MessageKind.FOLLOW_UP);
    }

    @Test
    void answerFollowUpReturnsBadRequestWhenNoGapAnalysisExistsYet() throws Exception {
        when(gapAnalysisService.answerFollowUp(eq(PROJECT_ID), anyString()))
                .thenThrow(new IllegalArgumentException(
                        "No gap analysis exists yet for this session; generate one before asking follow-up questions."));

        mockMvc.perform(post("/api/projects/{projectId}/intake-session/gap-analysis/follow-up", PROJECT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sender\":\"You\",\"text\":\"What about password reset?\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "No gap analysis exists yet for this session; generate one before asking follow-up questions."));

        verify(sessionService, never()).addMessage(anyString(), anyString(), anyString(), any());
    }

    @Test
    void getSessionCarriesKindThroughForARestoredGapAnalysisMessage() throws Exception {
        IntakeSessionDocument session = new IntakeSessionDocument(PROJECT_ID);
        session.setMessages(List.of(
                new IntakeSessionDocument.IntakeMessage(
                        "msg-1", "You", "We need a login page", OffsetDateTime.now()),
                new IntakeSessionDocument.IntakeMessage(
                        "msg-2", "Assistant", "Here is the gap analysis.", OffsetDateTime.now(),
                        IntakeSessionDocument.MessageKind.GAP_ANALYSIS)
        ));
        when(sessionService.getOrCreateSession(PROJECT_ID)).thenReturn(session);

        mockMvc.perform(get("/api/projects/{projectId}/intake-session", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].kind").value("USER"))
                .andExpect(jsonPath("$.messages[1].kind").value("GAP_ANALYSIS"))
                .andExpect(jsonPath("$.messages[1].text").value("Here is the gap analysis."));
    }

    @Test
    void getSessionDefaultsMissingKindToUserForMessagesPersistedBeforeTheFieldExisted() throws Exception {
        // Simulates a Mongo document saved before AISC-162 added the kind field: built
        // via the legacy 4-arg constructor, so the underlying field is null.
        IntakeSessionDocument session = new IntakeSessionDocument(PROJECT_ID);
        session.setMessages(List.of(
                new IntakeSessionDocument.IntakeMessage(
                        "msg-1", "You", "An old plain message", OffsetDateTime.now())
        ));
        when(sessionService.getOrCreateSession(PROJECT_ID)).thenReturn(session);

        mockMvc.perform(get("/api/projects/{projectId}/intake-session", PROJECT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages[0].kind").value("USER"));
    }
}
