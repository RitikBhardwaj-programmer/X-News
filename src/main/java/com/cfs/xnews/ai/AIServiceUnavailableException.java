package com.cfs.xnews.ai;

/**
 * The AI provider is temporarily unavailable (overloaded, rate limited or
 * erroring) even after retries. Mapped to HTTP 503 so clients know to retry
 * later rather than change their request.
 */
public class AIServiceUnavailableException extends RuntimeException {

    public AIServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
