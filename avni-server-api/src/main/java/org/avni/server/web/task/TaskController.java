package org.avni.server.web.task;

import org.avni.server.dao.task.TaskRepository;
import org.avni.server.domain.CHSEntity;
import org.avni.server.domain.User;
import org.avni.server.domain.accessControl.PrivilegeType;
import org.avni.server.domain.task.Task;
import org.avni.server.framework.security.UserContextHolder;
import org.avni.server.service.TaskService;
import org.avni.server.service.accessControl.AccessControlService;
import org.avni.server.web.response.AvniEntityResponse;
import org.avni.server.web.AbstractController;
import org.avni.server.web.RestControllerResourceProcessor;
import org.avni.server.web.request.TaskRequest;
import org.avni.server.web.response.slice.SlicedResources;
import org.joda.time.DateTime;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.transaction.Transactional;

import static org.avni.server.web.resourceProcessors.ResourceProcessor.addAuditFields;

@RestController
public class TaskController extends AbstractController<Task> implements RestControllerResourceProcessor<Task> {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TaskController.class);
    private final TaskRepository taskRepository;
    private final TaskService taskService;
    private final AccessControlService accessControlService;

    @Autowired
    public TaskController(TaskRepository taskRepository, TaskService taskService, AccessControlService accessControlService) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.accessControlService = accessControlService;
    }

    @RequestMapping(value = "/task", method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('user')")
    @Transactional
    public CollectionModel<EntityModel<Task>> getTasks(
            @RequestParam("lastModifiedDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime lastModifiedDateTime,
            @RequestParam("now") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime now,
            Pageable pageable) {
        User user = UserContextHolder.getUserContext().getUser();
        return wrap(taskRepository.findByAssignedToAndLastModifiedDateTimeIsBetweenOrderByLastModifiedDateTimeAscIdAsc(user, CHSEntity.toDate(lastModifiedDateTime), CHSEntity.toDate(now), pageable));
    }

    @RequestMapping(value = "/task/v2", method = RequestMethod.GET)
    @PreAuthorize(value = "hasAnyAuthority('user')")
    @Transactional
    public SlicedResources<EntityModel<Task>> getTasksAsSlice(
            @RequestParam("lastModifiedDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime lastModifiedDateTime,
            @RequestParam("now") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) DateTime now,
            Pageable pageable) {
        User user = UserContextHolder.getUserContext().getUser();
        return wrap(taskRepository.findSliceByAssignedToAndLastModifiedDateTimeIsBetweenOrderByLastModifiedDateTimeAscIdAsc(user, CHSEntity.toDate(lastModifiedDateTime), CHSEntity.toDate(now), pageable));
    }

    @RequestMapping(value = "/tasks", method = RequestMethod.POST)
    @Transactional
    public AvniEntityResponse save(@RequestBody TaskRequest taskRequest) {
        accessControlService.checkPrivilege(PrivilegeType.EditTask);
        logger.info(String.format("Saving task with UUID %s", taskRequest.getUuid()));
        Task savedTask = taskService.save(taskRequest);
        return new AvniEntityResponse(savedTask);
    }

    @Override
    public EntityModel<Task> process(EntityModel<Task> resource) {
        Task task = resource.getContent();
        resource.removeLinks();
        resource.add(Link.of(task.getTaskType().getUuid(), "taskTypeUUID"));
        resource.add(Link.of(task.getTaskStatus().getUuid(), "taskStatusUUID"));
        if (task.getSubject() != null) {
            resource.add(Link.of(task.getSubject().getUuid(), "subjectUUID"));
        }
        addAuditFields(task, resource);
        return resource;
    }
}
