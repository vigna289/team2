package com.dbtraining.reconx.exception;

/**
 * Abstract parent for every domain-level exception raised by the
 * reconciliation service. Extends RuntimeException (unchecked) so
 * controller signatures stay clean. One root lets a single
 * @RestControllerAdvice catch (ReconException) and map every subtype
 * to an RFC-7807 ProblemDetail.
 */
public abstract class ReconException extends RuntimeException {
    protected ReconException(String message) { super(message); }
    protected ReconException(String message, Throwable cause) { super(message, cause); }
}
