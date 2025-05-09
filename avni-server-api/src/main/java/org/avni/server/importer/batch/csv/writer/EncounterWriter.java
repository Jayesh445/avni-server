package org.avni.server.importer.batch.csv.writer;

import org.avni.server.application.FormMapping;
import org.avni.server.application.FormType;
import org.avni.server.dao.EncounterRepository;
import org.avni.server.dao.application.FormMappingRepository;
import org.avni.server.domain.Encounter;
import org.avni.server.domain.EntityApprovalStatus;
import org.avni.server.importer.batch.csv.contract.UploadRuleServerResponseContract;
import org.avni.server.importer.batch.csv.creator.*;
import org.avni.server.importer.batch.csv.writer.header.EncounterHeaderStrategyFactory;
import org.avni.server.importer.batch.csv.writer.header.EncounterHeadersCreator;
import org.avni.server.importer.batch.model.Row;
import org.avni.server.service.EncounterService;
import org.avni.server.service.ObservationService;
import org.avni.server.service.OrganisationConfigService;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Component
public class EncounterWriter extends EntityWriter implements ItemWriter<Row>, Serializable {
    private final EncounterRepository encounterRepository;
    private final BasicEncounterCreator basicEncounterCreator;
    private final FormMappingRepository formMappingRepository;
    private final ObservationService observationService;
    private final RuleServerInvoker ruleServerInvoker;
    private final VisitCreator visitCreator;
    private final DecisionCreator decisionCreator;
    private final ObservationCreator observationCreator;
    private final EncounterService encounterService;
    private final EntityApprovalStatusWriter entityApprovalStatusWriter;
    private final EncounterHeaderStrategyFactory strategyFactory;

    @Autowired
    public EncounterWriter(EncounterRepository encounterRepository,
                           BasicEncounterCreator basicEncounterCreator,
                           FormMappingRepository formMappingRepository,
                           ObservationService observationService,
                           RuleServerInvoker ruleServerInvoker,
                           VisitCreator visitCreator,
                           DecisionCreator decisionCreator,
                           ObservationCreator observationCreator,
                           EncounterService encounterService,
                           EntityApprovalStatusWriter entityApprovalStatusWriter,
                           OrganisationConfigService organisationConfigService, EncounterHeaderStrategyFactory strategyFactory) {
        super(organisationConfigService);
        this.encounterRepository = encounterRepository;
        this.basicEncounterCreator = basicEncounterCreator;
        this.formMappingRepository = formMappingRepository;
        this.observationService = observationService;
        this.ruleServerInvoker = ruleServerInvoker;
        this.visitCreator = visitCreator;
        this.decisionCreator = decisionCreator;
        this.observationCreator = observationCreator;
        this.encounterService = encounterService;
        this.entityApprovalStatusWriter = entityApprovalStatusWriter;
        this.strategyFactory = strategyFactory;
    }

    @Override
    public void write(Chunk<? extends Row> chunk) throws Exception {
        for (Row row : chunk.getItems()) write(row);
    }

    private void write(Row row) throws Exception {
        Encounter encounter = getOrCreateEncounter(row);

        List<String> allErrorMsgs = new ArrayList<>();
        basicEncounterCreator.updateEncounter(row, encounter, allErrorMsgs);
        encounter.setVoided(false);
        encounter.assignUUIDIfRequired();
        FormMapping formMapping = formMappingRepository.getRequiredFormMapping(null, null, encounter.getEncounterType().getUuid(), FormType.Encounter);
        if (formMapping == null) {
            throw new Exception(String.format("No form found for the encounter type %s", encounter.getEncounterType().getName()));
        }
        Encounter savedEncounter;

        if (skipRuleExecution()) {
            EncounterHeadersCreator encounterHeadersCreator = new EncounterHeadersCreator(strategyFactory);
            encounter.setObservations(observationCreator.getObservations(row, encounterHeadersCreator, allErrorMsgs, FormType.Encounter, encounter.getObservations(), formMapping));
            savedEncounter = encounterService.save(encounter);
        } else {
            UploadRuleServerResponseContract ruleResponse = ruleServerInvoker.getRuleServerResult(row, formMapping.getForm(), encounter, allErrorMsgs);
            encounter.setObservations(observationService.createObservations(ruleResponse.getObservations()));
            decisionCreator.addEncounterDecisions(encounter.getObservations(), ruleResponse.getDecisions());
            decisionCreator.addRegistrationDecisions(null, ruleResponse.getDecisions());
            savedEncounter = encounterService.save(encounter);
            visitCreator.saveScheduledVisits(formMapping.getType(), null, null, ruleResponse.getVisitSchedules(), savedEncounter.getUuid());
        }
        entityApprovalStatusWriter.saveStatus(formMapping, savedEncounter.getId(), EntityApprovalStatus.EntityType.Encounter, savedEncounter.getEncounterType().getUuid());
    }

    private Encounter getOrCreateEncounter(Row row) {
        String id = row.get(EncounterHeadersCreator.ID);
        Encounter existingEncounter = null;
        if (id != null && !id.isEmpty()) {
            existingEncounter = encounterRepository.findByLegacyIdOrUuid(id);
        }
        return existingEncounter == null ? createNewEncounter(id) : existingEncounter;
    }

    private Encounter createNewEncounter(String externalId) {
        Encounter encounter = new Encounter();
        encounter.setLegacyId(externalId);
        return encounter;
    }
}
