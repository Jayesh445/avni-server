package org.avni.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.avni.server.util.DateTimeUtil;
import org.hibernate.annotations.BatchSize;
import org.joda.time.DateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "checklist")
@JsonIgnoreProperties({"items", "programEnrolment"})
@BatchSize(size = 100)
public class Checklist extends OrganisationAwareEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JoinColumn(name = "checklist_detail_id")
    private ChecklistDetail checklistDetail;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_enrolment_id")
    private ProgramEnrolment programEnrolment;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "checklist")
    private List<ChecklistItem> items = new ArrayList<>();

    @NotNull
    @Column
    private Instant baseDate;

    public ChecklistDetail getChecklistDetail() {
        return checklistDetail;
    }

    public void setChecklistDetail(ChecklistDetail checklistDetail) {
        this.checklistDetail = checklistDetail;
    }

    public ProgramEnrolment getProgramEnrolment() {
        return programEnrolment;
    }

    public void setProgramEnrolment(ProgramEnrolment programEnrolment) {
        this.programEnrolment = programEnrolment;
    }

    public DateTime getBaseDate() {
        return DateTimeUtil.toJodaDateTime(baseDate);
    }

    public void setBaseDate(DateTime baseDate) {
        this.baseDate = DateTimeUtil.toInstant(baseDate);
    }

    public List<ChecklistItem> getItems() {
        return items;
    }

    public void setItems(List<ChecklistItem> items) {
        this.items = items;
    }
}
