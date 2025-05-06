package org.avni.messaging.service;

import org.avni.messaging.domain.MessageReceiver;
import org.avni.messaging.domain.ReceiverType;
import org.avni.messaging.domain.exception.GlificNotConfiguredException;
import org.avni.messaging.repository.GlificContactRepository;
import org.avni.messaging.repository.MessageReceiverRepository;
import org.avni.server.domain.Individual;
import org.avni.server.domain.User;
import org.avni.server.service.IndividualService;
import org.avni.server.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MessageReceiverService {

    private final MessageReceiverRepository messageReceiverRepository;

    private final GlificContactRepository glificContactRepository;

    private final IndividualService individualService;
    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(MessageReceiverService.class);

    @Autowired
    public MessageReceiverService(MessageReceiverRepository messageReceiverRepository, GlificContactRepository glificContactRepository, IndividualService individualService, UserService userService) {
        this.messageReceiverRepository = messageReceiverRepository;
        this.glificContactRepository = glificContactRepository;
        this.individualService = individualService;
        this.userService = userService;
    }

    public MessageReceiver saveReceiverIfRequired(ReceiverType receiverType, Long entityId) {
        Optional<MessageReceiver> messageReceiverOptional = messageReceiverRepository.findByReceiverIdAndReceiverType(entityId, receiverType);
        return messageReceiverOptional.orElseGet(() -> {
            MessageReceiver messageReceiver = new MessageReceiver(receiverType, entityId);
            messageReceiver.assignUUIDIfRequired();
            return messageReceiverRepository.save(messageReceiver);
        });
    }

    public MessageReceiver saveReceiverIfRequired(ReceiverType receiverType, String receiverId) {
        if (receiverType != ReceiverType.Group) return saveReceiverIfRequired(receiverType, Long.parseLong(receiverId));
        Optional<MessageReceiver> messageReceiverOptional = messageReceiverRepository.findByReceiverTypeAndExternalId(receiverType, receiverId);
        return messageReceiverOptional.orElseGet(() -> {
            MessageReceiver messageReceiver = new MessageReceiver(receiverType, receiverId);
            messageReceiver.assignUUIDIfRequired();
            return messageReceiverRepository.save(messageReceiver);
        });
    }

    public MessageReceiver ensureExternalIdPresent(MessageReceiver messageReceiver) throws PhoneNumberNotAvailableOrIncorrectException, GlificNotConfiguredException {
        if (messageReceiver.getExternalId() != null) {
            return messageReceiver;
        }

        String phoneNumber = null, fullName = null;
        if (messageReceiver.getReceiverType() == ReceiverType.Subject) {
            Individual individual = individualService.findById(messageReceiver.getReceiverId());
            phoneNumber = individualService.findPhoneNumber(individual);
            fullName = individual.getFullName();
        } else if (messageReceiver.getReceiverType() == ReceiverType.User) {
            User user = userService.findById(messageReceiver.getReceiverId()).get();
            phoneNumber = user.getPhoneNumber();
            fullName = user.getName();
        }

        String externalId = glificContactRepository.getOrCreateContact(phoneNumber, fullName);
        messageReceiver.setExternalId(externalId);
        return messageReceiverRepository.save(messageReceiver);
    }

    public void voidMessageReceiver(Long receiverId) {
        messageReceiverRepository.updateVoided(true, receiverId);
    }

    public Optional<MessageReceiver> findMessageReceiver(Long receiverId, ReceiverType receiverType) {
        return messageReceiverRepository.findByReceiverIdAndReceiverType(receiverId, receiverType);
    }

    public Optional<MessageReceiver> findExternalMessageReceiver(String externalId) {
        return messageReceiverRepository.findByExternalId(externalId);
    }
}
