package org.avni.server.framework.hibernate;

import org.avni.server.domain.RuleData;

public class RuleDataUserType extends AbstractJsonbUserType<RuleData> {
    @Override
    public Class returnedClass() {
        return RuleData.class;
    }
}
