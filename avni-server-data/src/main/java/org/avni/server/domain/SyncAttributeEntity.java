package org.avni.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

@MappedSuperclass
public class SyncAttributeEntity extends OrganisationAwareEntity {

    @Column(name = "sync_concept_1_value")
    private String syncConcept1Value;

    @Column(name = "sync_concept_2_value")
    private String syncConcept2Value;

    @JsonIgnore
    public String getSyncConcept1Value() {
        return syncConcept1Value;
    }

    public void setSyncConcept1Value(String syncConcept1Value) {
        this.syncConcept1Value = syncConcept1Value;
    }

    @JsonIgnore
    public String getSyncConcept2Value() {
        return syncConcept2Value;
    }

    public void setSyncConcept2Value(String syncConcept2Value) {
        this.syncConcept2Value = syncConcept2Value;
    }

    public void addConceptSyncAttributeValues(SubjectType subjectType, ObservationCollection observations) {
        if (subjectType.getSyncRegistrationConcept1() != null) {
            this.setSyncConcept1Value(observations.getObjectAsSingleStringValue(subjectType.getSyncRegistrationConcept1()));
        }
        if (subjectType.getSyncRegistrationConcept2() != null) {
            this.setSyncConcept2Value(observations.getObjectAsSingleStringValue(subjectType.getSyncRegistrationConcept2()));
        }
    }
}
