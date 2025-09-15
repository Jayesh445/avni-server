package org.avni.server.web.request;

import org.avni.server.domain.IdentifierSource;
import org.avni.server.domain.JsonObject;

public class IdentifierSourceContract extends CHSRequest {
    private Long batchGenerationSize;
    private Long minimumBalance;
    private String name;
    private JsonObject options;
    private String type;
    private String catchmentUUID;
    private Integer minLength;
    private Integer maxLength;

    public Long getBatchGenerationSize() {
        return batchGenerationSize;
    }

    public void setBatchGenerationSize(Long batchGenerationSize) {
        this.batchGenerationSize = batchGenerationSize;
    }

    public Long getMinimumBalance() {
        return minimumBalance;
    }

    public void setMinimumBalance(Long minimumBalance) {
        this.minimumBalance = minimumBalance;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public JsonObject getOptions() {
        return options;
    }

    public void setOptions(JsonObject options) {
        this.options = options;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCatchmentUUID() {
        return catchmentUUID;
    }

    public void setCatchmentUUID(String catchmentUUID) {
        this.catchmentUUID = catchmentUUID;
    }

    public Integer getMinLength() {
        return minLength;
    }

    public void setMinLength(Integer minLength) {
        this.minLength = minLength;
    }

    public Integer getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(Integer maxLength) {
        this.maxLength = maxLength;
    }

    public static IdentifierSourceContract fromIdentifierSource(IdentifierSource identifierSource) {
        IdentifierSourceContract contract = new IdentifierSourceContract();
        contract.setBatchGenerationSize(identifierSource.getBatchGenerationSize());
        if (identifierSource.getCatchment() != null)
            contract.setCatchmentUUID(identifierSource.getCatchment().getUuid());
        contract.setMaxLength(identifierSource.getMaxLength());
        contract.setMinLength(identifierSource.getMinLength());
        contract.setMinimumBalance(identifierSource.getMinimumBalance());
        contract.setName(identifierSource.getName());
        contract.setType(identifierSource.getType().name());
        contract.setOptions(identifierSource.getOptions());
        contract.setId(identifierSource.getId());
        contract.setVoided(identifierSource.isVoided());
        contract.setUuid(identifierSource.getUuid());
        return contract;
    }
}
