package org.avni.server.dao.search;

import org.avni.server.domain.SubjectType;
import org.avni.server.web.request.webapp.search.SubjectSearchRequest;

public interface SearchBuilder {
    SqlQuery getSQLResultQuery(SubjectSearchRequest searchRequest, SubjectType subjectType);

    SqlQuery getSQLCountQuery(SubjectSearchRequest searchRequest, SubjectType subjectType);
}
