package org.avni.server.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.avni.server.domain.organisation.OrganisationCategory;
import org.avni.server.domain.organisation.OrganisationStatus;
import org.hibernate.annotations.BatchSize;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "organisation")
@BatchSize(size = 100)
public class Organisation extends ETLEntity {
    @Column
    private String mediaDirectory;

    @Column
    private Long parentOrganisationId;

    @Column
    private String usernameSuffix;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private OrganisationCategory category;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private OrganisationStatus status;

    public Organisation() {
    }

    public Organisation(String name) {
        this.setName(name);
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getMediaDirectory() {
        return mediaDirectory;
    }

    public void setMediaDirectory(String mediaDirectory) {
        this.mediaDirectory = mediaDirectory;
    }

    public Long getParentOrganisationId() {
        return parentOrganisationId;
    }

    public void setParentOrganisationId(Long parentOrganisationId) {
        this.parentOrganisationId = parentOrganisationId;
    }

    /**
     * Use getEffectiveUsernameSuffix instead
     */
    @Deprecated
    public String getUsernameSuffix() {
        return usernameSuffix;
    }

    public void setUsernameSuffix(String usernameSuffix) {
        this.usernameSuffix = usernameSuffix;
    }

    @JsonIgnore
    public boolean isNew() {
        Long id = getId();
        return (id == null || id == 0);
    }

    public String getEffectiveUsernameSuffix() {
        return usernameSuffix == null ? getDbUser() : usernameSuffix;
    }

    @JsonIgnore
    public OrganisationCategory getCategory() {
        return category;
    }

    public Long getCategoryId() {
        return category.getId();
    }

    public void setCategory(OrganisationCategory organisationCategory) {
        this.category = organisationCategory;
    }

    @JsonIgnore
    public OrganisationStatus getStatus() {
        return status;
    }

    public void setStatus(OrganisationStatus status) {
        this.status = status;
    }

    public Long getStatusId() {
        return status.getId();
    }
}
