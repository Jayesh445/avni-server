package org.avni.server.importer.batch.csv.creator;

import org.avni.server.application.Form;
import org.avni.server.domain.Encounter;
import org.avni.server.domain.Individual;
import org.avni.server.domain.ProgramEncounter;
import org.avni.server.domain.ProgramEnrolment;
import org.avni.server.importer.batch.csv.contract.UploadRuleServerRequestContract;
import org.avni.server.importer.batch.csv.contract.UploadRuleServerResponseContract;
import org.avni.server.importer.batch.model.Row;
import org.avni.server.service.EntityApprovalStatusService;
import org.avni.server.service.ObservationService;
import org.avni.server.web.external.RuleServiceClient;
import org.avni.server.web.request.rules.RulesContractWrapper.EncounterContract;
import org.avni.server.web.request.rules.RulesContractWrapper.IndividualContract;
import org.avni.server.web.request.rules.RulesContractWrapper.ProgramEncounterContract;
import org.avni.server.web.request.rules.RulesContractWrapper.ProgramEnrolmentContract;
import org.avni.server.web.request.rules.constructWrappers.IndividualConstructionService;
import org.avni.server.web.request.rules.constructWrappers.ProgramEncounterConstructionService;
import org.avni.server.web.request.rules.constructWrappers.ProgramEnrolmentConstructionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RuleServerInvoker {
    private final RuleServiceClient restClient;
    private final ProgramEnrolmentConstructionService programEnrolmentConstructionService;
    private final IndividualConstructionService individualConstructionService;
    private final ObservationService observationService;
    private final ProgramEncounterConstructionService programEncounterConstructionService;
    private final EntityApprovalStatusService entityApprovalStatusService;

    @Autowired
    public RuleServerInvoker(RuleServiceClient restClient,
                             ProgramEnrolmentConstructionService programEnrolmentConstructionService,
                             IndividualConstructionService individualConstructionService, ObservationService observationService,
                             ProgramEncounterConstructionService programEncounterConstructionService,
                             EntityApprovalStatusService entityApprovalStatusService) {
        this.restClient = restClient;
        this.programEnrolmentConstructionService = programEnrolmentConstructionService;
        this.individualConstructionService = individualConstructionService;
        this.observationService = observationService;
        this.programEncounterConstructionService = programEncounterConstructionService;
        this.entityApprovalStatusService = entityApprovalStatusService;
    }

    private UploadRuleServerResponseContract invokeRuleServer(Row row, Form form, Object entity, List<String> allErrorMsgs) throws Exception {
        UploadRuleServerRequestContract contract = UploadRuleServerRequestContract.buildRuleServerContract(row, form, entity);
        UploadRuleServerResponseContract ruleResponse = (UploadRuleServerResponseContract) restClient.post("/api/upload", contract, UploadRuleServerResponseContract.class);
        allErrorMsgs.addAll(ruleResponse.getErrors());
        if (allErrorMsgs.size() > 0) {
            throw new Exception(String.join(", ", allErrorMsgs));
        }
        return ruleResponse;
    }

    public UploadRuleServerResponseContract getRuleServerResult(Row row, Form form, Individual individual, List<String> allErrorMsgs) throws Exception {
        IndividualContract entity = individualConstructionService.constructBasicSubject(individual);
        return invokeRuleServer(row, form, entity, allErrorMsgs);
    }

    public UploadRuleServerResponseContract getRuleServerResult(Row row, Form form, ProgramEnrolment programEnrolment, List<String> allErrorMsgs) throws Exception {
        ProgramEnrolmentContract entity = programEnrolmentConstructionService.constructProgramEnrolmentContract(programEnrolment);
        return invokeRuleServer(row, form, entity, allErrorMsgs);
    }

    public UploadRuleServerResponseContract getRuleServerResult(Row row, Form form, ProgramEncounter programEncounter, List<String> allErrorMsgs) throws Exception {
        ProgramEncounterContract entity = programEncounterConstructionService.constructProgramEncounterContractWrapper(programEncounter);
        entity.setProgramEnrolment(programEncounterConstructionService.constructEnrolments(programEncounter.getProgramEnrolment(), programEncounter.getUuid()));
        return invokeRuleServer(row, form, entity, allErrorMsgs);
    }

    public UploadRuleServerResponseContract getRuleServerResult(Row row, Form form, Encounter encounter, List<String> allErrorMsgs) throws Exception {
        EncounterContract entity = EncounterContract.fromEncounter(encounter, observationService, entityApprovalStatusService);
        entity.setSubject(individualConstructionService.getSubjectInfo(encounter.getIndividual()));
        return invokeRuleServer(row, form, entity, allErrorMsgs);
    }
}
