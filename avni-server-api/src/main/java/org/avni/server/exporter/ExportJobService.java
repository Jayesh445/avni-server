package org.avni.server.exporter;


import com.fasterxml.jackson.core.type.TypeReference;
import org.avni.server.dao.AvniJobRepository;
import org.avni.server.dao.ExportJobParametersRepository;
import org.avni.server.dao.JobStatus;
import org.avni.server.domain.ExportJobParameters;
import org.avni.server.domain.Organisation;
import org.avni.server.domain.User;
import org.avni.server.domain.UserContext;
import org.avni.server.framework.security.UserContextHolder;
import org.avni.server.service.ExportS3Service;
import org.avni.server.util.ObjectMapperSingleton;
import org.avni.server.web.external.request.export.ExportJobRequest;
import org.avni.server.web.external.request.export.ExportOutput;
import org.avni.server.web.external.request.export.ExportV2JobRequest;
import org.avni.server.web.util.ErrorBodyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Service
public class ExportJobService {
    private final Logger logger;

    private final AvniJobRepository avniJobRepository;
    private final Job exportVisitJob;
    private final Job exportV2Job;

    private final JobLauncher bgJobLauncher;
    private final ExportJobParametersRepository exportJobParametersRepository;
    private final ErrorBodyBuilder errorBodyBuilder;

    @Autowired
    public ExportJobService(Job exportVisitJob, JobLauncher bgJobLauncher, AvniJobRepository avniJobRepository,
                            Job exportV2Job, ExportJobParametersRepository exportJobParametersRepository, ErrorBodyBuilder errorBodyBuilder) {
        this.avniJobRepository = avniJobRepository;
        this.exportV2Job = exportV2Job;
        this.exportJobParametersRepository = exportJobParametersRepository;
        this.errorBodyBuilder = errorBodyBuilder;
        logger = LoggerFactory.getLogger(getClass());
        this.bgJobLauncher = bgJobLauncher;
        this.exportVisitJob = exportVisitJob;
    }

    @Transactional
    public Page<JobStatus> getAll(@NotNull Pageable pageable) {
        User user = UserContextHolder.getUserContext().getUser();
        String jobFilterCondition = " and subjectTypeUUID <> '' ";
        return avniJobRepository.getJobStatuses(user, jobFilterCondition, pageable);
    }

    public ExportOutput getExportOutput(String exportJobParamsUUID) {
        ExportJobParameters exportJobParameters = exportJobParametersRepository.findByUuid(exportJobParamsUUID);
        return ObjectMapperSingleton.getObjectMapper().convertValue(exportJobParameters.getReportFormat(), new TypeReference<ExportOutput>() {
        });
    }

    public ResponseEntity<?> runExportJob(@RequestBody ExportJobRequest exportJobRequest) {
        UserContext userContext = UserContextHolder.getUserContext();
        String mediaDirectory = userContext.getOrganisation().getMediaDirectory();
        if (mediaDirectory == null) {
            String errorMessage = "Information missing. Media Directory for Implementation absent";
            logger.error(errorMessage);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        }
        JobParameters jobParameters = getCommonJobParams(userContext)
                .addString("programUUID", exportJobRequest.getProgramUUID(), false)
                .addString("subjectTypeUUID", exportJobRequest.getSubjectTypeUUID(), false)
                .addString("encounterTypeUUID", exportJobRequest.getEncounterTypeUUID(), false)
                .addDate("startDate", exportJobRequest.getStartDate(), false)
                .addDate("endDate", exportJobRequest.getEndDate(), false)
                .addString("reportType", exportJobRequest.getReportType().name())
                .addString("addressIds", exportJobRequest.getAddressLevelString())
                .addString("timeZone", exportJobRequest.getTimeZone())
                .addString("includeVoided", String.valueOf(exportJobRequest.isIncludeVoided()))
                .toJobParameters();
        return launchJob(jobParameters, exportVisitJob);
    }

    public ResponseEntity<?> runExportV2Job(ExportV2JobRequest exportJobRequest) {
        ExportJobParameters exportJobParameters = exportJobRequest.buildJobParameters();
        exportJobParametersRepository.save(exportJobParameters);
        String subjectTypeUUID = ObjectMapperSingleton.getObjectMapper()
                .convertValue(exportJobParameters.getReportFormat(), new TypeReference<ExportOutput>() {}).getUuid();
        JobParameters jobParameters = getCommonJobParams(UserContextHolder.getUserContext())
                .addString("subjectTypeUUID", subjectTypeUUID, false)
                .addString("reportType", "Custom")
                .addString("exportJobParamsUUID", exportJobParameters.getUuid()).toJobParameters();
        return launchJob(jobParameters, exportV2Job);
    }

    private ResponseEntity<?> launchJob(JobParameters jobParameters, Job job) {
        try {
            bgJobLauncher.run(job, jobParameters);
        } catch (JobParametersInvalidException | JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException | JobRestartException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorBodyBuilder.getErrorBody(e));
        }
        return ResponseEntity.ok(true);
    }

    private JobParametersBuilder getCommonJobParams(UserContext userContext) {
        User user = userContext.getUser();
        Organisation organisation = userContext.getOrganisation();
        String jobUUID = UUID.randomUUID().toString();
        return new JobParametersBuilder()
                .addString("uuid", jobUUID)
                .addString("organisationUUID", organisation.getUuid())
                .addLong("userId", user.getId(), false)
                .addLong("organisationId", organisation.getId())
                .addString("fileName", jobUUID.concat(ExportS3Service.FILE_NAME_EXTENSION));
    }
}
