package org.avni.server.domain.accessControl;

public class AvniNoUserSessionException extends RuntimeException {
    public AvniNoUserSessionException(String message) {
        super(message);
    }

    public AvniNoUserSessionException(Throwable cause) {
        super(cause);
    }
}
