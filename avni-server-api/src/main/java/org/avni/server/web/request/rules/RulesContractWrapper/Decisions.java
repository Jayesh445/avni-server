package org.avni.server.web.request.rules.RulesContractWrapper;


import java.util.ArrayList;
import java.util.List;

public class Decisions {
    private List<Decision> enrolmentDecisions;
    private List<Decision> encounterDecisions;
    private List<Decision> registrationDecisions;
    private boolean exit;
    private boolean cancel;

    public static Decisions nullObject() {
        Decisions decisions = new Decisions();
        decisions.setEnrolmentDecisions(new ArrayList<>());
        decisions.setEncounterDecisions(new ArrayList<>());
        decisions.setRegistrationDecisions(new ArrayList<>());
        return decisions;
    }

    public List<Decision> getEnrolmentDecisions() {
        return enrolmentDecisions;
    }

    public void setEnrolmentDecisions(List<Decision> enrolmentDecisions) {
        this.enrolmentDecisions = enrolmentDecisions;
    }

    public List<Decision> getEncounterDecisions() {
        return encounterDecisions;
    }

    public void setEncounterDecisions(List<Decision> encounterDecisions) {
        this.encounterDecisions = encounterDecisions;
    }

    public List<Decision> getRegistrationDecisions() {
        return registrationDecisions;
    }

    public void setRegistrationDecisions(List<Decision> registrationDecisions) {
        this.registrationDecisions = registrationDecisions;
    }

    public boolean isExit() {
        return exit;
    }

    public void setExit(boolean exit) {
        this.exit = exit;
    }

    public boolean isCancel() {
        return cancel;
    }

    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }
}
