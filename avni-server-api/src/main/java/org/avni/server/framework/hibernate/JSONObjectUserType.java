package org.avni.server.framework.hibernate;

import org.avni.server.domain.JsonObject;

public class JSONObjectUserType extends AbstractJsonbUserType<JsonObject> {
    @Override
    public Class returnedClass() {
        return JsonObject.class;
    }
}
