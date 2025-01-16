package org.avni.server.domain;

public interface MessageableEntity {
    public Long getEntityTypeId();

    public Long getEntityId();

    public Individual getIndividual();

    public User getCreatedBy();

    public boolean isVoided();
}
