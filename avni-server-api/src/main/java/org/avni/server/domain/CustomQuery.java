package org.avni.server.domain;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity(name = "custom_query")
public class CustomQuery extends OrganisationAwareEntity {

    @NotNull
    @Column(name = "name")
    private String name;

    @NotNull
    @Column(name = "query")
    private String query;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
