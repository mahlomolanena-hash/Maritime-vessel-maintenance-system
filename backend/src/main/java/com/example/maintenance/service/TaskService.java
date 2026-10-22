package com.example.maintenance.service;

import com.example.maintenance.domain.Equipment;
import com.example.maintenance.domain.MaintenanceTask;
import com.example.maintenance.dto.request.TaskRequest;
import com.example.maintenance.dto.response.TaskResponse;
import com.example.maintenance.exception.BadRequestException;
import com.example.maintenance.exception.ConflictException;
import com.example.maintenance.exception.DuplicateResourceException;
import com.example.maintenance.exception.ResourceNotFoundException;
import com.example.maintenance.repository.EquipmentRepository;
import com.example.maintenance.repository.MaintenanceScheduleRepository;
import com.example.maintenance.repository.MaintenanceTaskRepository;
import com.example.maintenance.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final MaintenanceTaskRepository taskRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaintenanceScheduleRepository scheduleRepository;
    private final WorkOrderRepository workOrderRepository;

    @Transactional(readOnly = true)
    public List<TaskResponse> list(Long equipmentId) {
        List<MaintenanceTask> tasks = equipmentId != null
                ? taskRepository.findByEquipmentIdOrderByTitleAsc(equipmentId)
                : taskRepository.findAllByOrderByTitleAsc();
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse get(Long id) {
        return TaskResponse.from(find(id));
    }

    @Transactional
    public TaskResponse create(TaskRequest req) {
        Equipment equipment = equipmentRepository.findById(req.equipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Equipment not found with id " + req.equipmentId()));
        if (taskRepository.existsByEquipmentIdAndTitleIgnoreCase(equipment.getId(), req.title().trim())) {
            throw new DuplicateResourceException("This equipment already has a task called '" + req.title().trim() + "'");
        }
        MaintenanceTask task = MaintenanceTask.builder()
                .equipment(equipment)
                .title(req.title().trim())
                .description(req.description())
                .estimatedHours(req.estimatedHours())
                .active(req.active() == null || req.active())
                .build();
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskResponse update(Long id, TaskRequest req) {
        MaintenanceTask task = find(id);
        if (!task.getEquipment().getId().equals(req.equipmentId())) {
            throw new BadRequestException("A task cannot be moved to different equipment");
        }
        if (taskRepository.existsByEquipmentIdAndTitleIgnoreCaseAndIdNot(task.getEquipment().getId(), req.title().trim(), id)) {
            throw new DuplicateResourceException("This equipment already has a task called '" + req.title().trim() + "'");
        }
        task.setTitle(req.title().trim());
        task.setDescription(req.description());
        task.setEstimatedHours(req.estimatedHours());
        if (req.active() != null) {
            task.setActive(req.active());
        }
        return TaskResponse.from(taskRepository.save(task));
    }

    @Transactional
    public void delete(Long id) {
        MaintenanceTask task = find(id);
        if (scheduleRepository.existsByTaskId(id) || workOrderRepository.existsByTaskId(id)) {
            throw new ConflictException("This task has schedules or work orders. Deactivate it instead of deleting it.");
        }
        taskRepository.delete(task);
    }

    MaintenanceTask find(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id " + id));
    }
}
