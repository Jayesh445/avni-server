package org.avni.server.web;


import org.avni.server.dao.QueryRepository;
import org.avni.server.web.request.CustomQueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;

@RestController
public class CustomQueryController {

    private final QueryRepository queryRepository;

    @Autowired
    public CustomQueryController(QueryRepository queryRepository) {
        this.queryRepository = queryRepository;
    }

    @RequestMapping(value = "/executeQuery", method = RequestMethod.POST)
    @PreAuthorize(value = "hasAnyAuthority('user')")
    @ResponseBody
    @Transactional
    public ResponseEntity<?> executeQuery(@RequestBody CustomQueryRequest customQueryRequest) {
        if (customQueryRequest.getName() == null) {
            return ResponseEntity.badRequest().body("No query name passed in the request");
        }
        return queryRepository.runQuery(customQueryRequest);
    }
}
