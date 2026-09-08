package com.scrumplanner.core.llm;

/**
 * LlmClient (AISC-19): minimal, provider-agnostic abstraction for a single LLM call.
 *
 * <p>Deliberately narrow — a system prompt, a user prompt, a text response — rather than
 * the full multi-provider/per-project routing described for the "AI Gateway" module in
 * the README roadmap. That module is explicitly out of scope for this Epic; this
 * interface exists so a concrete implementation can be swapped later (e.g. once the AI
 * Gateway lands) without touching {@code GapAnalysisService}.
 */
public interface LlmClient {

    /**
     * Sends a single prompt to the configured LLM and returns its text response.
     *
     * @throws LlmClientException if the client isn't configured (e.g. no API key) or the
     *                            call to the provider fails.
     */
    String complete(String systemPrompt, String userPrompt);
}
