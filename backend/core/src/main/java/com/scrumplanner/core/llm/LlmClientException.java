package com.scrumplanner.core.llm;

/**
 * Raised when an {@link LlmClient} isn't configured (e.g. no API key) or a call to the
 * underlying provider fails. Mapped to HTTP 502 by {@code ApiExceptionHandler} — the
 * caller's own request was fine, it's the upstream LLM provider that failed.
 */
public class LlmClientException extends RuntimeException {
    public LlmClientException(String message) {
        super(message);
    }

    public LlmClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
