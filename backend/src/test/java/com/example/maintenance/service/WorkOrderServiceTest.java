package com.example.maintenance.service;

import com.example.maintenance.domain.*;
import com.example.maintenance.dto.request.AddPartRequest;
import com.example.maintenance.dto.request.AssignRequest;
import com.example.maintenance.dto.request.CompleteWorkOrderRequest;
import com.example.maintenance.dto.request.WorkOrderRequest;
import com.example.maintenance.dto.response.WorkOrderResponse;
import com.example.maintenance.exception.InsufficientStockException;
import com.example.maintenance.exception.InvalidWorkOrderStateException;
import com.example.maintenance.repository.*;
import com.example.maintenance.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkOrderServiceTest {

    @Mock WorkOrderRepository workOrderRepository;
    @Mock WorkOrderStatusHistoryRepository historyRepository;
    @Mock MaintenanceRecordRepository recordRepository;
    @Mock EquipmentRepository equipmentRepository;
    @Mock MaintenanceTaskRepository taskRepository;
    @Mock MaintenanceScheduleRepository scheduleRepository;
    @Mock SparePartRepository sparePartRepository;
    @Mock UserRepository userRepository;
    @Mock ScheduleService scheduleService;

    @InjectMocks WorkOrderService service;

    private User superintendent;
    private User engineer;
    private User otherEngineer;
    private Equipment equipment;

    @BeforeEach
    void setUp() {
        superintendent = user(1L, "Sam", Role.SUPERINTENDENT);
        engineer = user(2L, "Eli", Role.ENGINEER);
        otherEngineer = user(3L, "Olu", Role.ENGINEER);

        Vessel vessel = Vessel.builder().id(10L).name("MV Test").imoNumber("1234567")
                .vesselType(VesselType.CARGO).status(VesselStatus.ACTIVE).build();
        equipment = Equipment.builder().id(20L).vessel(vessel).name("Main Engine").tagNumber("ME-01")
                .runningHours(new BigDecimal("1500.0")).operatingHours(new BigDecimal("1400.0")).build();

        for (User u : new User[]{superintendent, engineer, otherEngineer}) {
            when(userRepository.findById(u.getId())).thenReturn(Optional.of(u));
        }
        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workOrderRepository.saveAndFlush(any(WorkOrder.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------ permissions

    @Test
    void engineerCannotAssignWork() {
        WorkOrder wo = workOrder(WorkOrderStatus.OPEN, null);
        assertThrows(AccessDeniedException.class,
                () -> service.assign(wo.getId(), new AssignRequest(2L), principal(engineer)));
        verify(workOrderRepository, never()).save(any());
    }

    @Test
    void engineerCannotCreateAnAlreadyAssignedWorkOrder() {
        when(equipmentRepository.findById(20L)).thenReturn(Optional.of(equipment));
        WorkOrderRequest req = new WorkOrderRequest(20L, null, "Fix leak", null,
                WorkOrderType.CORRECTIVE, WorkOrderPriority.HIGH, null, 3L);
        assertThrows(AccessDeniedException.class, () -> service.create(req, principal(engineer)));
    }

    @Test
    void anEngineerWhoIsNotTheAssigneeCannotStartTheJob() {
        WorkOrder wo = workOrder(WorkOrderStatus.ASSIGNED, engineer);
        assertThrows(AccessDeniedException.class, () -> service.start(wo.getId(), principal(otherEngineer)));
        assertEquals(WorkOrderStatus.ASSIGNED, wo.getStatus());
    }

    // ------------------------------------------------------------ workflow

    @Test
    void assigneeStartsTheJob() {
        WorkOrder wo = workOrder(WorkOrderStatus.ASSIGNED, engineer);

        WorkOrderResponse response = service.start(wo.getId(), principal(engineer));

        assertEquals(WorkOrderStatus.IN_PROGRESS, response.status());
        assertNotNull(wo.getStartedAt());
        verify(historyRepository).save(any(WorkOrderStatusHistory.class));
    }

    @Test
    void managerAssigningAnOpenOrderMovesItToAssigned() {
        WorkOrder wo = workOrder(WorkOrderStatus.OPEN, null);

        WorkOrderResponse response = service.assign(wo.getId(), new AssignRequest(2L), principal(superintendent));

        assertEquals(WorkOrderStatus.ASSIGNED, response.status());
        assertEquals(2L, response.assignedToId());
    }

    @Test
    void cannotCompleteAnOrderThatWasNeverStarted() {
        WorkOrder wo = workOrder(WorkOrderStatus.ASSIGNED, engineer);
        assertThrows(InvalidWorkOrderStateException.class,
                () -> service.complete(wo.getId(), new CompleteWorkOrderRequest("Done", null), principal(engineer)));
        verify(recordRepository, never()).save(any());
    }

    @Test
    void completingWritesTheMaintenanceRecordAndRestartsTheSchedule() {
        WorkOrder wo = workOrder(WorkOrderStatus.IN_PROGRESS, engineer);
        MaintenanceSchedule schedule = MaintenanceSchedule.builder().id(30L).intervalType(IntervalType.CALENDAR_DAYS)
                .intervalValue(30).build();
        wo.setSchedule(schedule);

        WorkOrderResponse response = service.complete(wo.getId(),
                new CompleteWorkOrderRequest("Filters changed", new BigDecimal("2.5")), principal(engineer));

        assertEquals(WorkOrderStatus.COMPLETED, response.status());
        ArgumentCaptor<MaintenanceRecord> captor = ArgumentCaptor.forClass(MaintenanceRecord.class);
        verify(recordRepository).save(captor.capture());
        MaintenanceRecord record = captor.getValue();
        assertEquals("Filters changed", record.getWorkPerformed());
        assertEquals(new BigDecimal("1500.0"), record.getRunningHoursAtService());
        assertEquals(new BigDecimal("2.5"), record.getActualHours());
        verify(scheduleService).advanceAfterCompletion(eq(schedule), any(LocalDate.class));
    }

    // ------------------------------------------------------------ spare parts

    @Test
    void addingAPartDrawsItFromStockAndFreezesItsPrice() {
        WorkOrder wo = workOrder(WorkOrderStatus.IN_PROGRESS, engineer);
        SparePart part = part(5, "10.00");
        when(sparePartRepository.findById(40L)).thenReturn(Optional.of(part));

        WorkOrderResponse response = service.addPart(wo.getId(), new AddPartRequest(40L, 2), principal(engineer));

        assertEquals(3, part.getQuantityInStock());
        assertEquals(1, response.parts().size());
        assertEquals(new BigDecimal("20.00"), response.partsCost());
    }

    @Test
    void addingTheSamePartTwiceMergesIntoOneLine() {
        WorkOrder wo = workOrder(WorkOrderStatus.IN_PROGRESS, engineer);
        SparePart part = part(10, "5.00");
        when(sparePartRepository.findById(40L)).thenReturn(Optional.of(part));

        service.addPart(wo.getId(), new AddPartRequest(40L, 2), principal(engineer));
        WorkOrderResponse response = service.addPart(wo.getId(), new AddPartRequest(40L, 3), principal(engineer));

        assertEquals(1, response.parts().size());
        assertEquals(5, response.parts().get(0).quantityUsed());
        assertEquals(5, part.getQuantityInStock());
    }

    @Test
    void cannotUseMoreThanIsInStock() {
        WorkOrder wo = workOrder(WorkOrderStatus.IN_PROGRESS, engineer);
        SparePart part = part(1, "10.00");
        when(sparePartRepository.findById(40L)).thenReturn(Optional.of(part));

        assertThrows(InsufficientStockException.class,
                () -> service.addPart(wo.getId(), new AddPartRequest(40L, 2), principal(engineer)));
        assertEquals(1, part.getQuantityInStock());
    }

    @Test
    void partsCanOnlyBeAddedWhileTheJobIsInProgress() {
        WorkOrder wo = workOrder(WorkOrderStatus.ASSIGNED, engineer);
        assertThrows(InvalidWorkOrderStateException.class,
                () -> service.addPart(wo.getId(), new AddPartRequest(40L, 1), principal(engineer)));
    }

    // ------------------------------------------------------------ helpers

    private WorkOrder workOrder(WorkOrderStatus status, User assignee) {
        WorkOrder wo = WorkOrder.builder().id(50L).workOrderNumber("WO-2026-00050").equipment(equipment)
                .title("Service").type(WorkOrderType.PREVENTIVE).priority(WorkOrderPriority.MEDIUM)
                .status(status).createdBy(superintendent).assignedTo(assignee).build();
        when(workOrderRepository.findById(50L)).thenReturn(Optional.of(wo));
        return wo;
    }

    private SparePart part(int stock, String cost) {
        return SparePart.builder().id(40L).partNumber("P-1").name("Filter").unit("pcs")
                .quantityInStock(stock).reorderLevel(2).unitCost(new BigDecimal(cost)).build();
    }

    private User user(Long id, String name, Role role) {
        return User.builder().id(id).email(name.toLowerCase() + "@example.com").password("x")
                .firstName(name).lastName("Test").roles(Set.of(role)).build();
    }

    private UserPrincipal principal(User user) {
        return new UserPrincipal(user);
    }
}
