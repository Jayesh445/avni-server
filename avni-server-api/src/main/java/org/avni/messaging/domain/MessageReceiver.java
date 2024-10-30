package org.avni.messaging.domain;

import org.avni.server.domain.OrganisationAwareEntity;

import javax.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "message_receiver")
public class MessageReceiver extends OrganisationAwareEntity {
    @Column
    @NotNull
    @Enumerated(EnumType.STRING)
    private ReceiverType receiverType;

    @Column
    private Long receiverId;

    @Column
    private String externalId;

    public MessageReceiver(ReceiverType receiverType, Long receiverId) {
        this.receiverType = receiverType;
        this.receiverId = receiverId;
    }

    public MessageReceiver(ReceiverType receiverType, String externalId) {
        this.receiverType = receiverType;
        this.externalId = externalId;
    }

    public MessageReceiver() {
        receiverType = null;
        receiverId = null;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public ReceiverType getReceiverType() {
        return receiverType;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setReceiverType(ReceiverType receiverType) {
        this.receiverType = receiverType;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
}
