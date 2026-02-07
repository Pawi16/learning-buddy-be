package dev.pawin.backend_learning_buddy.common.enumeration;

public enum JobStatus {
    QUEUED,       // Initial state
    PROCESSING,   // Generating quiz
    COMPLETED,    // Success
    FAILED        // Error occurred
}
