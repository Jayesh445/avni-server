package org.avni.server.exporter.v2;

import org.avni.server.dao.ExportJobParametersRepository;
import org.avni.server.domain.*;
import org.avni.server.web.external.request.export.ExportEntityType;
import org.avni.server.web.external.request.export.ExportOutput;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@StepScope
public class ExportV2Processor implements ItemProcessor<Object, LongitudinalExportItemRow> {
    private final ExportJobParametersRepository exportJobParametersRepository;
    private final String exportJobParamsUUID;
    private ExportJobParameters exportJobParameters;
    private ExportOutput exportOutput;

    public ExportV2Processor(ExportJobParametersRepository exportJobParametersRepository,
                             @Value("#{jobParameters['exportJobParamsUUID']}") String exportJobParamsUUID) {
        this.exportJobParametersRepository = exportJobParametersRepository;
        this.exportJobParamsUUID = exportJobParamsUUID;
    }

    @PostConstruct
    public void init() {
        exportJobParameters = exportJobParametersRepository.findByUuid(exportJobParamsUUID);
    }

    @Override
    public LongitudinalExportItemRow process(Object exportItem) {
        LongitudinalExportItemRow exportItemRow = new LongitudinalExportItemRow();
        Individual individual = initIndividual((Individual) exportItem, exportItemRow);
        initGeneralEncounters(exportItemRow, individual);
        initProgramsAndTheirEncounters(exportItemRow, individual);
        initGroupSubjectsAndTheirEncounters(exportItemRow, individual);
        return exportItemRow;
    }

    private Individual initIndividual(Individual exportItem, LongitudinalExportItemRow exportItemRow) {
        // Individual would have already passed filters applicable for it
        Individual individual = exportItem;
        exportItemRow.setIndividual(individual);
        return individual;
    }

    private void initGroupSubjectsAndTheirEncounters(LongitudinalExportItemRow exportItemRow, Individual individual) {
        // filter GroupSubject by exportOutput
        Map<String, ExportEntityType> groupsToFiltersMap = Optional.ofNullable(exportOutput.getGroups()).orElse(new ArrayList<>())
                .stream().collect(Collectors.toMap(ExportEntityType::getUuid, Function.identity()));
        // filter GroupSubject encounters by exportOutput
        Map<String, ExportEntityType> groupsEncountersToFiltersMap = Optional.ofNullable(exportOutput.getGroups()).orElse(new ArrayList<>())
                .stream().flatMap(p -> p.getEncounters().stream())
                .collect(Collectors.toMap(ExportEntityType::getUuid, Function.identity()));
        Map<Individual, Map<String, List<Encounter>>> individualToEncountersMap = Optional.ofNullable(individual.getMemberGroupSubjects())
                .orElse(new HashSet<>()).stream()
                .filter(gr -> applyFilters(groupsToFiltersMap, gr.getGroupSubject().getSubjectType().getUuid(), gr.getGroupSubject().getRegistrationDate()
                        .toDateTimeAtStartOfDay(DateTimeZone.forID(exportJobParameters.getTimezone())), gr.isVoided()))
                .flatMap(gs -> gs.getGroupSubject().getEncounters(false))
                .filter(e -> e.getEncounterDateTime() != null || e.getCancelDateTime() != null)
                .filter(e -> applyFilters(groupsEncountersToFiltersMap, e.getEncounterType().getUuid(), Optional.ofNullable(e.getEncounterDateTime()).orElse(e.getCancelDateTime()), e.isVoided()))
                .sorted(this::compareEncounters)
                .collect(Collectors.groupingBy(Encounter::getIndividual, LinkedHashMap::new,
                        Collectors.groupingBy(e -> e.getEncounterType().getUuid(), LinkedHashMap::new, Collectors.toList())));
        exportItemRow.setGroupSubjectToEncountersMap(individualToEncountersMap);
    }

    private void initProgramsAndTheirEncounters(LongitudinalExportItemRow exportItemRow, Individual individual) {
        // filter ProgramEnrolment by exportOutput
        Map<String, ExportEntityType> programsToFiltersMap = Optional.ofNullable(exportOutput.getPrograms()).orElse(new ArrayList<>())
                .stream().collect(Collectors.toMap(ExportEntityType::getUuid, Function.identity()));
        // filter ProgramEncounters by exportOutput
        Map<String, ExportEntityType> encountersToFiltersMap = Optional.ofNullable(exportOutput.getPrograms()).orElse(new ArrayList<>())
                .stream().flatMap(p -> p.getEncounters().stream())
                .collect(Collectors.toMap(ExportEntityType::getUuid, Function.identity()));
        Map<ProgramEnrolment, Map<String, List<ProgramEncounter>>> programToEncountersMap = Optional.ofNullable(individual.getProgramEnrolments())
                .orElse(new HashSet<>()).stream()
                .filter(pe -> applyFilters(programsToFiltersMap, pe.getProgram().getUuid(), pe.getEnrolmentDateTime(), pe.isVoided()))
                .flatMap(pe -> pe.getEncounters(false))
                .filter(e -> e.getEncounterDateTime() != null || e.getCancelDateTime() != null)
                .filter(e -> applyFilters(encountersToFiltersMap, e.getEncounterType().getUuid(), Optional.ofNullable(e.getEncounterDateTime()).orElse(e.getCancelDateTime()), e.isVoided()))
                .sorted(this::compareEncounters)
                .collect(Collectors.groupingBy(ProgramEncounter::getProgramEnrolment, LinkedHashMap::new,
                        Collectors.groupingBy(pe -> pe.getEncounterType().getUuid(), LinkedHashMap::new, Collectors.toList())));
        exportItemRow.setProgramEnrolmentToEncountersMap(programToEncountersMap);
    }

    private void initGeneralEncounters(LongitudinalExportItemRow exportItemRow, Individual individual) {
        // filter Encounter by exportOutput
        Map<String, ExportEntityType> generalEncountersToFiltersMap = Optional.ofNullable(exportOutput.getEncounters()).orElse(new ArrayList<>())
                .stream().collect(Collectors.toMap(ExportEntityType::getUuid, Function.identity()));
        Map<String, List<Encounter>> generalEncounters = Optional.ofNullable(individual.getEncounters()).orElse(new HashSet<>())
                .stream()
                .filter(e -> e.getEncounterDateTime() != null || e.getCancelDateTime() != null)
                .filter(e -> applyFilters(generalEncountersToFiltersMap, e.getEncounterType().getUuid(), Optional.ofNullable(e.getEncounterDateTime()).orElse(e.getCancelDateTime()), e.isVoided()))
                .sorted(this::compareEncounters)
                .collect(Collectors.groupingBy(e -> e.getEncounterType().getUuid(), LinkedHashMap::new, Collectors.toList()));
        exportItemRow.setEncounterTypeToEncountersMap(generalEncounters);
    }

    public boolean applyFilters(Map<String, ExportEntityType> entityToFiltersMap, String typeUUID, DateTime entityDateTime, boolean isVoided) {
        ExportEntityType entity = entityToFiltersMap.get(typeUUID);
        if(entity == null) {
            return false;
        } else if(entity.isDateEmpty()) {
            return (entity.getFilters().includeVoided() || !isVoided);
        }
        return (entity.getFilters().includeVoided() || !isVoided) && (entity.getFilters().getDate().apply(entityDateTime));
    }

    public void setExportOutput(ExportOutput exportOutput) {
        this.exportOutput = exportOutput;
    }

    private int compareEncounters(AbstractEncounter enc1, AbstractEncounter enc2) {
        DateTime t1 = Optional.ofNullable(enc1.getEncounterDateTime()).orElse(enc1.getCancelDateTime());
        DateTime t2 = Optional.ofNullable(enc2.getEncounterDateTime()).orElse(enc2.getCancelDateTime());
        return t1.compareTo(t2);
    }
}
