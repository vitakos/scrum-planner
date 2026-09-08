package com.scrumplanner.core.gapanalysis;

import com.scrumplanner.core.intakesession.IntakeSessionDocument;
import com.scrumplanner.core.intakesession.IntakeSessionService;
import com.scrumplanner.core.llm.LlmClient;
import com.scrumplanner.core.workspace.RepositoryContextReader;
import com.scrumplanner.core.workspace.RepositoryWorkspaceService;
import com.scrumplanner.core.workspace.WorkspaceResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * GapAnalysisService (AISC-19, AISC-20): orchestrates generating a gap analysis from an
 * intake conversation and (if available) the project's cloned repo, and answering
 * follow-up questions about a previously generated gap analysis.
 */
@Service
public class GapAnalysisService {

    private static final String GAP_ANALYSIS_SYSTEM_PROMPT = """
            You are a requirements analyst assistant. Given an intake conversation describing a
            change request, and (if available) a summary of the target repository, produce a gap
            analysis: what the request implies versus what's already built. Be concise and
            concrete, and organize the analysis around what already exists, what's missing, and
            open questions.""";

    private static final String FOLLOW_UP_SYSTEM_PROMPT = """
            You are a requirements analyst assistant continuing a conversation about a gap
            analysis you previously generated. Use that gap analysis and the conversation history
            as context to answer the user's follow-up question directly and concisely.""";

    private static final String NO_REPO_CONTEXT_NOTE =
            "_Note: no repository context was available for this analysis._\n\n";

    private final IntakeSessionService intakeSessionService;
    private final RepositoryWorkspaceService workspaceService;
    private final RepositoryContextReader contextReader;
    private final LlmClient llmClient;

    public GapAnalysisService(
            IntakeSessionService intakeSessionService,
            RepositoryWorkspaceService workspaceService,
            RepositoryContextReader contextReader,
            LlmClient llmClient
    ) {
        this.intakeSessionService = intakeSessionService;
        this.workspaceService = workspaceService;
        this.contextReader = contextReader;
        this.llmClient = llmClient;
    }

    /**
     * Generates a gap analysis from the project's intake conversation and, if available,
     * its cloned repository (AISC-19). When no repo context is available, the returned
     * text always notes that explicitly (Scenario 2), regardless of what the LLM itself
     * says.
     */
    public String generateGapAnalysis(String projectId) {
        IntakeSessionDocument session = intakeSessionService.getOrCreateSession(projectId);
        String conversation = formatConversation(session);

        WorkspaceResult workspaceResult = workspaceService.ensureWorkspace(UUID.fromString(projectId));
        String repoContext = switch (workspaceResult) {
            case WorkspaceResult.Cloned cloned -> contextReader.summarize(cloned.workspacePath());
            case WorkspaceResult.Pulled pulled -> contextReader.summarize(pulled.workspacePath());
            case WorkspaceResult.Skipped skipped -> null;
            case WorkspaceResult.Failed failed -> null;
        };

        String userPrompt = """
                Intake conversation:
                %s

                %s

                Produce a gap analysis comparing the request to the current repo state.""".formatted(
                conversation,
                repoContext != null
                        ? "Repository context:\n" + repoContext
                        : "No repository context is available for this project."
        );

        String result = llmClient.complete(GAP_ANALYSIS_SYSTEM_PROMPT, userPrompt);
        return repoContext == null ? NO_REPO_CONTEXT_NOTE + result : result;
    }

    /**
     * Answers a follow-up question about the most recently generated gap analysis in the
     * session (AISC-20), using it plus the conversation so far as context.
     *
     * @throws IllegalArgumentException if no gap analysis has been generated yet for this session.
     */
    public String answerFollowUp(String projectId, String followUpQuestion) {
        IntakeSessionDocument session = intakeSessionService.getOrCreateSession(projectId);
        String priorGapAnalysis = findLatestGapAnalysis(session)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No gap analysis exists yet for this session; generate one before asking follow-up questions."));

        String userPrompt = """
                Gap analysis:
                %s

                Conversation so far:
                %s

                Follow-up question:
                %s""".formatted(priorGapAnalysis, formatConversation(session), followUpQuestion);

        return llmClient.complete(FOLLOW_UP_SYSTEM_PROMPT, userPrompt);
    }

    private Optional<String> findLatestGapAnalysis(IntakeSessionDocument session) {
        List<IntakeSessionDocument.IntakeMessage> messages = session.getMessages();
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getKind() == IntakeSessionDocument.MessageKind.GAP_ANALYSIS) {
                return Optional.of(messages.get(i).getText());
            }
        }
        return Optional.empty();
    }

    private String formatConversation(IntakeSessionDocument session) {
        return session.getMessages().stream()
                .map(message -> message.getSender() + ": " + message.getText())
                .collect(Collectors.joining("\n"));
    }
}
