package org.avni.server.web.request;

import org.avni.server.domain.AddressLevel;
import org.avni.server.domain.ObservationCollection;
import org.avni.server.geo.Point;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class LocationContract extends ReferenceDataContract {
    private Double level;
    private ReferenceDataContract parent;
    @Deprecated
    private List<LocationContract> parents;
    private String type;
    private String organisationUUID;
    private String addressLevelTypeUUID;
    private String legacyId;
    private Point gpsCoordinates;
    private ObservationCollection locationProperties = new ObservationCollection();

    public LocationContract() {
    }

    public LocationContract(String uuid) {
        setUuid(uuid);
    }

    public Double getLevel() {
        return level;
    }

    public void setLevel(Double level) {
        this.level = level;
    }

    public String getType() {
        return type != null ? StringUtils.capitalize(type.trim().toLowerCase()) : null;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<LocationContract> getParents() {
        if (parents == null) {
            return parents = new ArrayList<>();
        }
        return parents;
    }

    public void setParents(List<LocationContract> parents) {
        this.parents = parents;
    }

    public void setParent(ReferenceDataContract parent) {
        this.parent = parent;
    }

    public String getOrganisationUUID() {
        return organisationUUID;
    }

    public void setOrganisationUUID(String organisationUUID) {
        this.organisationUUID = organisationUUID;
    }

    public ReferenceDataContract getParent() {
        if (null != parent) {
            return parent;
        } else {
            return getParents().stream().findFirst().orElse(null);
        }
    }

    public String getAddressLevelTypeUUID() {
        return addressLevelTypeUUID;
    }

    public void setAddressLevelTypeUUID(String addressLevelTypeUUID) {
        this.addressLevelTypeUUID = addressLevelTypeUUID;
    }

    public String getLegacyId() {
        return legacyId;
    }

    public void setLegacyId(String legacyId) {
        this.legacyId = legacyId;
    }

    public Point getGpsCoordinates() {
        return gpsCoordinates;
    }

    public void setGpsCoordinates(Point gpsCoordinates) {
        this.gpsCoordinates = gpsCoordinates;
    }

    public ObservationCollection getLocationProperties() {
        return locationProperties;
    }

    public void setLocationProperties(ObservationCollection locationProperties) {
        this.locationProperties = locationProperties;
    }

    public static LocationContract fromAddressLevel(AddressLevel addressLevel) {
        LocationContract contract = new LocationContract();
        if (addressLevel == null) return contract;
        contract.setUuid(addressLevel.getUuid());
        contract.setName(addressLevel.getTitle());
        contract.setType(addressLevel.getTypeString());
        contract.setLevel(addressLevel.getLevel());
        contract.setAddressLevelTypeUUID(addressLevel.getType().getUuid());
        contract.setVoided(addressLevel.isVoided());
        contract.setLegacyId(addressLevel.getLegacyId());
        contract.setGpsCoordinates(addressLevel.getGpsCoordinates());
        contract.setLocationProperties(addressLevel.getLocationProperties());
        AddressLevel parent = addressLevel.getParent();
        if (parent != null) {
            ReferenceDataContract parentContract = new ReferenceDataContract();
            parentContract.setUuid(parent.getUuid());
            contract.setParent(parentContract);
        }
        return contract;
    }
}
