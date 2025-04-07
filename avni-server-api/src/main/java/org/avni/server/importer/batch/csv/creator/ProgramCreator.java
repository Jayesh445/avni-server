package org.avni.server.importer.batch.csv.creator;

import org.avni.server.dao.ProgramRepository;
import org.avni.server.domain.Program;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProgramCreator {
    private final ProgramRepository programRepository;

    @Autowired
    public ProgramCreator(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    public Program getProgram(String name, String header) {
        Program program = programRepository.findByName(name);
        if (program == null) {
            throw new RuntimeException(String.format("'%s' '%s' not found", header, name));
        }
        return program;
    }
}
