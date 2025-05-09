package org.avni.server.importer.batch.csv.creator;

import org.avni.server.application.*;
import org.avni.server.common.ValidationResult;
import org.avni.server.dao.ConceptRepository;
import org.avni.server.dao.application.FormElementRepository;
import org.avni.server.dao.application.FormRepository;
import org.avni.server.domain.Concept;
import org.avni.server.domain.ConceptDataType;
import org.avni.server.domain.ObservationCollection;
import org.avni.server.domain.ValidationException;
import org.avni.server.importer.batch.csv.writer.header.HeaderCreator;
import org.avni.server.importer.batch.model.Row;
import org.avni.server.service.*;
import org.avni.server.util.*;
import org.avni.server.web.request.ObservationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;

@Component
public class ObservationCreator {
    private static final Logger logger = LoggerFactory.getLogger(ObservationCreator.class);
    private final ConceptRepository conceptRepository;
    private final FormRepository formRepository;
    private final ObservationService observationService;
    private final S3Service s3Service;
    private final IndividualService individualService;
    private final LocationService locationService;
    private final FormElementRepository formElementRepository;
    private final EnhancedValidationService enhancedValidationService;

    @Autowired
    public ObservationCreator(ConceptRepository conceptRepository,
                              FormRepository formRepository,
                              ObservationService observationService,
                              S3Service s3Service,
                              IndividualService individualService,
                              LocationService locationService,
                              FormElementRepository formElementRepository,
                              EnhancedValidationService enhancedValidationService) {
        this.conceptRepository = conceptRepository;
        this.formRepository = formRepository;
        this.observationService = observationService;
        this.s3Service = s3Service;
        this.individualService = individualService;
        this.locationService = locationService;
        this.formElementRepository = formElementRepository;
        this.enhancedValidationService = enhancedValidationService;
    }

    public Set<Concept> getConceptsInHeader(HeaderCreator headers, FormMapping formMapping, String[] fileHeaders) {
        String[] conceptHeaders = headers.getConceptHeaders(formMapping, fileHeaders);
        return Arrays.stream(conceptHeaders)
                .map(name -> this.findConcept(S.unDoubleQuote(name), false))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private Concept findConcept(String name, boolean isChildQuestionGroup) {
        Concept concept = conceptRepository.findByName(name);
        if (concept == null && name.contains("|")) {
            String[] parentChildNameArray = name.split("\\|");
            String questionGroupConceptName = isChildQuestionGroup ? parentChildNameArray[1] : parentChildNameArray[0];
            concept = conceptRepository.findByName(questionGroupConceptName);
        }
        return concept;
    }

    public ObservationCollection getObservations(Row row,
                                                 HeaderCreator headers,
                                                 List<String> errorMsgs, FormType formType, ObservationCollection oldObservations, FormMapping formMapping) throws ValidationException {
        ObservationCollection observationCollection = constructObservations(row, headers, errorMsgs, formType, oldObservations, formMapping, row.getHeaders());
        ValidationUtil.handleErrors(errorMsgs);
        return observationCollection;
    }

    private boolean isNonEmptyQuestionGroup(FormElement formElement, Row row) {
        Concept concept = formElement.getConcept();
        if (ConceptDataType.isQuestionGroup(concept.getDataType())) {
            List<FormElement> allChildQuestions = formElementRepository.findAllByGroupId(formElement.getId());
            return allChildQuestions.stream().anyMatch(fe -> {
                String parentChildName = concept.getName() + "|" + fe.getConcept().getName();
                String headerName = formElement.isRepeatable() ? String.format("%s|1", parentChildName) : parentChildName;
                String rowValue = row.get(headerName);
                return !(rowValue == null || rowValue.trim().equals(""));
            });
        }
        return false;
    }

    private String getRowValue(FormElement formElement, Row row, Integer questionGroupIndex) {
        Concept concept = formElement.getConcept();
        if (formElement.getGroup() != null) {
            Concept parentConcept = formElement.getGroup().getConcept();
            String parentChildName = parentConcept.getName() + "|" + concept.getName();
            String headerName = questionGroupIndex == null ? parentChildName : String.format("%s|%d", parentChildName, questionGroupIndex);
            return row.get(headerName);
        }
        return row.getObservation(concept.getName());
    }

    private ObservationCollection constructObservations(Row row, HeaderCreator headers, List<String> errorMsgs, FormType formType, ObservationCollection oldObservations, FormMapping formMapping, String[] fileHeaders) {
        List<ObservationRequest> observationRequests = new ArrayList<>();
        Set<Concept> conceptsInHeader = getConceptsInHeader(headers, formMapping, fileHeaders);
        for (Concept concept : conceptsInHeader) {
            FormElement formElement = getFormElementForObservationConcept(concept, formType);
            String rowValue = getRowValue(formElement, row, null);
            if (!isNonEmptyQuestionGroup(formElement, row) && (rowValue == null || rowValue.trim().isEmpty()))
                continue;
            ObservationRequest observationRequest = new ObservationRequest();
            observationRequest.setConceptName(concept.getName());
            observationRequest.setConceptUUID(concept.getUuid());
            try {
                Object observationValue = getObservationValue(formElement, rowValue, formType, errorMsgs, row, headers, oldObservations);
                observationRequest.setValue(observationValue);
            } catch (RuntimeException ex) {
                logger.error(String.format("Error processing observation %s in row %s", rowValue, row), ex);
                errorMsgs.add(String.format("Invalid answer '%s' for '%s'", rowValue, concept.getName()));
            }
            observationRequests.add(observationRequest);
        }
        return observationService.createObservations(observationRequests);
    }

    // For the repeatable question group columns should be "Question group concept"|"Child concept"|"order(1,2,3...)"
    private Object constructChildObservations(Row row, HeaderCreator headers, List<String> errorMsgs, FormElement parentFormElement, FormType formType, ObservationCollection oldObservations) {
        List<FormElement> allChildQuestions = formElementRepository.findAllByGroupId(parentFormElement.getId());
        if (parentFormElement.isRepeatable()) {
            Pattern repeatableQuestionGroupPattern = Pattern.compile(String.format("%s\\|.*\\|\\d", parentFormElement.getConcept().getName()));
            List<String> repeatableQuestionGroupHeaders = Stream.of(row.getHeaders())
                    .filter(repeatableQuestionGroupPattern.asPredicate())
                    .collect(Collectors.toList());
            int maxIndex = repeatableQuestionGroupHeaders.stream().map(fen -> Integer.valueOf(fen.split("\\|")[2]))
                    .mapToInt(v -> v)
                    .max().orElse(1);
            List<ObservationCollection> repeatableObservationRequest = new ArrayList<>();
            for (int i = 1; i <= maxIndex; i++) {
                ObservationCollection questionGroupObservations = getQuestionGroupObservations(row, headers, errorMsgs, formType, oldObservations, allChildQuestions, i);
                if (!questionGroupObservations.isEmpty()) {
                    repeatableObservationRequest.add(questionGroupObservations);
                }
            }
            return repeatableObservationRequest;
        }
        return getQuestionGroupObservations(row, headers, errorMsgs, formType, oldObservations, allChildQuestions, null);
    }

    private ObservationCollection getQuestionGroupObservations(Row row, HeaderCreator headers, List<String> errorMsgs, FormType formType, ObservationCollection oldObservations, List<FormElement> allChildQuestions, Integer questionGroupIndex) {
        List<ObservationRequest> observationRequests = new ArrayList<>();
        for (FormElement formElement : allChildQuestions) {
            Concept concept = formElement.getConcept();
            String rowValue = getRowValue(formElement, row, questionGroupIndex);
            if (rowValue == null || rowValue.trim().equals(""))
                continue;
            ObservationRequest observationRequest = new ObservationRequest();
            observationRequest.setConceptName(concept.getName());
            observationRequest.setConceptUUID(concept.getUuid());
            try {
                observationRequest.setValue(getObservationValue(formElement, rowValue, formType, errorMsgs, row, headers, oldObservations));
            } catch (Exception ex) {
                logger.error(String.format("Error processing observation %s in row %s", rowValue, row), ex);
                errorMsgs.add(String.format("Invalid answer '%s' for '%s'", rowValue, concept.getName()));
            }
            observationRequests.add(observationRequest);
        }
        return observationService.createObservations(observationRequests);
    }

    private List<FormElement> createDecisionFormElement(Set<Concept> concepts) {
        return concepts.stream().map(dc -> {
            FormElement formElement = new FormElement();
            formElement.setType(dc.getDataType().equals(ConceptDataType.Coded.name()) ? FormElementType.MultiSelect.name() : FormElementType.SingleSelect.name());
            formElement.setConcept(dc);
            return formElement;
        }).collect(Collectors.toList());
    }

    private FormElement getFormElementForObservationConcept(Concept concept, FormType formType) {
        List<Form> applicableForms = formRepository.findByFormTypeAndIsVoidedFalse(formType);
        if (applicableForms.isEmpty())
            throw new RuntimeException(String.format("No forms of type %s found", formType));

        return applicableForms.stream()
                .map(f -> {
                    List<FormElement> formElements = f.getAllFormElements();
                    formElements.addAll(createDecisionFormElement(f.getDecisionConcepts()));
                    return formElements;
                })
                .flatMap(List::stream)
                .filter(formElement -> formElement.getConcept().equals(concept))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No form element linked to concept found"));
    }

    private Object getObservationValue(FormElement formElement, String answerValue, FormType formType, List<String> errorMsgs, Row row, HeaderCreator headers, ObservationCollection oldObservations) {
        Concept concept = formElement.getConcept();
        Object oldValue = oldObservations == null ? null : oldObservations.getOrDefault(concept.getUuid(), null);
        switch (ConceptDataType.valueOf(concept.getDataType())) {
            case Coded:
                if (formElement.getType().equals(FormElementType.MultiSelect.name())) {
                    String[] providedAnswers = S.splitMultiSelectAnswer(answerValue);
                    return Stream.of(providedAnswers)
                            .map(answer -> {
                                Concept answerConcept = concept.findAnswerConcept(answer);
                                if (answerConcept == null) {
                                    errorMsgs.add(format("Invalid answer '%s' for '%s'", answer, concept.getName()));
                                    return null;
                                }
                                return answerConcept.getUuid();
                            })
                            .collect(Collectors.toList());
                } else {
                    Concept answerConcept = concept.findAnswerConcept(answerValue);
                    if (answerConcept == null) {
                        errorMsgs.add(format("Invalid answer '%s' for '%s'", answerValue, concept.getName()));
                        return null;
                    }
                    return answerConcept.getUuid();
                }
            case Numeric:
                try {
                    return Double.parseDouble(answerValue);
                } catch (NumberFormatException e) {
                    errorMsgs.add(format("Invalid value '%s' for '%s'", answerValue, concept.getName()));
                    return null;
                }
            case Date:
                try {
                    String trimmed = answerValue.trim();
                    return (trimmed.isEmpty()) ? null : DateTimeUtil.parseFlexibleDate(trimmed);
                } catch (IllegalArgumentException e) {
                    errorMsgs.add(format("Invalid value '%s' for '%s'", answerValue, concept.getName()));
                    return null;
                }
            case DateTime:
                try {
                    return (answerValue.trim().isEmpty()) ? null : toISODateFormat(answerValue);
                } catch (DateTimeParseException e) {
                    errorMsgs.add(format("Invalid value '%s' for '%s'", answerValue, concept.getName()));
                    return null;
                }
            case Image:
            case ImageV2:
            case Video:
                if (formElement.getType().equals(FormElementType.MultiSelect.name())) {
                    String[] providedURLs = S.splitMultiSelectAnswer(answerValue);
                    return Stream.of(providedURLs)
                            .map(url -> getMediaObservationValue(url, errorMsgs, null))
                            .collect(Collectors.toList());
                } else {
                    return getMediaObservationValue(answerValue, errorMsgs, oldValue);
                }
            case Subject:
                return individualService.getObservationValueForUpload(formElement, answerValue);
            case Location:
                return locationService.getObservationValueForUpload(formElement, answerValue);
            case PhoneNumber:
                return (answerValue.trim().equals("")) ? null : toPhoneNumberFormat(answerValue.trim(), errorMsgs, concept.getName());
            case QuestionGroup:
                return this.constructChildObservations(row, headers, errorMsgs, formElement, formType, null);
            default:
                return answerValue;
        }
    }

    private Object getMediaObservationValue(String answerValue, List<String> errorMsgs, Object oldValue) {
        try {
            return s3Service.getObservationValueForUpload(answerValue, oldValue);
        } catch (Exception e) {
            errorMsgs.add(e.getMessage());
            return null;
        }
    }

    private Map<String, Object> toPhoneNumberFormat(String phoneNumber, List<String> errorMsgs, String conceptName) {
        Map<String, Object> phoneNumberObs = new HashMap<>();
        if (!PhoneNumberUtil.isValidPhoneNumber(phoneNumber, RegionUtil.getCurrentUserRegion())) {
            errorMsgs.add(format("Invalid %s provided %s. Please provide valid phone number.", conceptName, phoneNumber));
            return null;
        }
        phoneNumberObs.put("phoneNumber", PhoneNumberUtil.getNationalPhoneNumber(phoneNumber, RegionUtil.getCurrentUserRegion()));
        phoneNumberObs.put("verified", false);
        return phoneNumberObs;
    }

    private String toISODateFormat(String dateStr) {
        DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        DateTimeFormatter parseFmt = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd[ HH:mm:ss]")
                .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                .toFormatter();
        ZonedDateTime dt = null;
        if (parseFmt.parseBest(dateStr, ZonedDateTime::from, java.time.LocalDate::from) instanceof ZonedDateTime) {
            dt = (ZonedDateTime) parseFmt.parseBest(dateStr, ZonedDateTime::from, java.time.LocalDate::from);
        } else if (parseFmt.parseBest(dateStr, ZonedDateTime::from, java.time.LocalDate::from) instanceof java.time.LocalDate) {
            dt = ((java.time.LocalDate) parseFmt.parseBest(dateStr, ZonedDateTime::from, java.time.LocalDate::from)).atStartOfDay(ZoneId.systemDefault());
        }
        return dt.format(outputFmt);
    }
}
