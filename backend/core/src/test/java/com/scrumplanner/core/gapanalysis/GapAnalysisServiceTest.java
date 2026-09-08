package com.scrumplanner.core.gapanalysis;

import com.scrumplanner.core.intakesession.IntakeSessionDocument;
import com.scrumplanner.core.intakesession.IntakeSessionService;
import com.scrumplanner.core.llm.LlmClient;
import com.scrumplanner.core.workspace.RepositoryContextReader;
import com.scrumplanner.core.workspace.RepositoryWorkspaceService;
import com.scrumplanner.core.workspace.WorkspaceResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers AISC-19 Scenarios 1 & 2: a gap analysis is built from the intake conversation
 * plus repo context when a repo is available (Cloned/Pulled), and still produces a gap
 * analysis — deterministically noting the absence of repo context, regardless of what
 * the LLM itself says — when it isn't (Skipped/Failed workspace outcomes).
 *
 * <p>Also covers AISC-20 Scenario 1: a follow-up question is answered using the prior
 * gap analysis and conversation as context, and is rejected outright when no gap
 * analysis has been generated yet for the session.
 */
@ExtendWith(MockitoExtension.class)
class GapAnalysisServiceTest {

    private static final String PROJECT_ID = UUID.randomUUID().toString();

    @Mock
    private IntakeSessionService intakeSessionService;

    @Mock
    private RepositoryWorkspaceService workspaceService;

    @Mock
    private RepositoryContextReader contextReader;

    @Mock
    private LlmClient llmClient;

    private GapAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new GapAnalysisService(intakeSessionService, workspaceService, contextReader, llmClient);
        when(intakeSessionService.getOrCreateSession(PROJECT_ID)).thenReturn(sessionWithOneMessage());
    }

    @Test
    void includesRepoContextInPromptWhenWorkspaceWasCloned() {
        Path workspacePath = Path.of("workspaces", PROJECT_ID);
        when(workspaceService.ensureWorkspace(UUID.fromString(PROJECT_ID)))
                .thenReturn(new WorkspaceResult.Cloned(workspacePath));
        when(contextReader.summarize(workspacePath)).thenReturn("Repository file structure:\n- README.md");
        when(llmClient.complete(anyString(), anyString())).thenReturn("Here is the gap analysis.");

        String result = service.generateGapAnalysis(PROJECT_ID);

        assertThat(capturedUserPrompt())
                .contains("Repository context:")
                .contains("README.md")
                .contains("We need a login page");
        assertThat(result).isEqualTo("Here is the gap analysis.");
        assertThat(result).doesNotContain("no repository context was available");
    }

    @Test
    void includesRepoContextInPromptWhenWorkspaceWasPulled() {
        Path workspacePath = Path.of("workspaces", PROJECT_ID);
        when(workspaceService.ensureWorkspace(UUID.fromString(PROJECT_ID)))
                .thenReturn(new WorkspaceResult.Pulled(workspacePath));
        when(contextReader.summarize(workspacePath)).thenReturn("Repository file structure:\n- README.md");
        when(llmClient.complete(anyString(), anyString())).thenReturn("Here is the gap analysis.");

        String result = service.generateGapAnalysis(PROJECT_ID);

        assertThat(capturedUserPrompt()).contains("Repository context:");
        assertThat(result).doesNotContain("no repository context was available");
    }

    @Test
    void notesAbsenceOfRepoContextWhenWorkspaceWasSkipped() {
        when(workspaceService.ensureWorkspace(UUID.fromString(PROJECT_ID)))
                .thenReturn(new WorkspaceResult.Skipped("No repository URL configured for project " + PROJECT_ID));
        when(llmClient.complete(anyString(), anyString())).thenReturn("Gap analysis based only on conversation.");

        String result = service.generateGapAnalysis(PROJECT_ID);

        assertThat(capturedUserPrompt()).contains("No repository context is available for this project.");
        assertThat(result)
                .startsWith("_Note: no repository context was available for this analysis._")
                .contains("Gap analysis based only on conversation.");
        verify(contextReader, never()).summarize(any());
    }

    @Test
    void notesAbsenceOfRepoContextWhenWorkspaceFailed() {
        when(workspaceService.ensureWorkspace(UUID.fromString(PROJECT_ID)))
                .thenReturn(new WorkspaceResult.Failed("connection refused"));
        when(llmClient.complete(anyString(), anyString())).thenReturn("Gap analysis based only on conversation.");

        String result = service.generateGapAnalysis(PROJECT_ID);

        assertThat(result).startsWith("_Note: no repository context was available for this analysis._");
        verify(contextReader, never()).summarize(any());
    }

    @Test
    void answersFollowUpUsingPriorGapAnalysisAndConversationAsContext() {
        IntakeSessionDocument session = new IntakeSessionDocument(PROJECT_ID);
        session.setMessages(List.of(
                new IntakeSessionDocument.IntakeMessage(
                        UUID.randomUUID().toString(), "You", "We need a login page", OffsetDateTime.now()),
                new IntakeSessionDocument.IntakeMessage(
                        UUID.randomUUID().toString(), "Assistant", "Existing gap analysis text", OffsetDateTime.now(),
                        IntakeSessionDocument.MessageKind.GAP_ANALYSIS)
        ));
        when(intakeSessionService.getOrCreateSession(PROJECT_ID)).thenReturn(session);
        when(llmClient.complete(anyString(), anyString())).thenReturn("Here is the follow-up answer.");

        String result = service.answerFollowUp(PROJECT_ID, "What about password reset?");

        assertThat(capturedUserPrompt())
                .contains("Existing gap analysis text")
                .contains("We need a login page")
                .contains("What about password reset?");
        assertThat(result).isEqualTo("Here is the follow-up answer.");
    }

    @Test
    void rejectsFollowUpWhenNoGapAnalysisExistsYet() {
        // setUp() stubs a session with only a plain USER message, no gap analysis.
        assertThatThrownBy(() -> service.answerFollowUp(PROJECT_ID, "What about password reset?"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No gap analysis exists yet");
        verify(llmClient, never()).complete(anyString(), anyString());
    }

    private String capturedUserPrompt() {
        ArgumentCaptor<String> userPromptCaptor = ArgumentCaptor.forClass(String.class);
        verify(llmClient).complete(anyString(), userPromptCaptor.capture());
        return userPromptCaptor.getValue();
    }

    private IntakeSessionDocument sessionWithOneMessage() {
        IntakeSessionDocument session = new IntakeSessionDocument(PROJECT_ID);
        session.setMessages(List.of(
                new IntakeSessionDocument.IntakeMessage(
                        UUID.randomUUID().toString(), "You", "We need a login page", OffsetDateTime.now())
        ));
        return session;
    }
}
