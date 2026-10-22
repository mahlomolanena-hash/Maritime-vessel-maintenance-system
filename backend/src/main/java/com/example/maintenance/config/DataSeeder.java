package com.example.maintenance.config;

import com.example.maintenance.domain.*;
import com.example.maintenance.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Seeds the dev/H2 database with demo users, two vessels, equipment, preventive schedules in every
 * due state (overdue, due soon, ok), spare parts and a few work orders, so every screen has data.
 * Only active on the "dev" profile, never runs against prod.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final VesselRepository vesselRepository;
    private final EquipmentRepository equipmentRepository;
    private final MaintenanceTaskRepository taskRepository;
    private final MaintenanceScheduleRepository scheduleRepository;
    private final SparePartRepository sparePartRepository;
    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderStatusHistoryRepository historyRepository;
    private final MaintenanceRecordRepository recordRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        LocalDate today = LocalDate.now();

        // ---- users
        User admin = user("admin@example.com", "Admin123!", "Ada", "Admin", Role.ADMIN);
        User superintendent = user("superintendent@example.com", "Super123!", "Sam", "Superintendent", Role.SUPERINTENDENT);
        User engineer = user("engineer@example.com", "Engineer123!", "Eli", "Engineer", Role.ENGINEER);

        // ---- vessels
        Vessel atlantic = vesselRepository.save(Vessel.builder()
                .name("MV Atlantic Star").imoNumber("9387421").vesselType(VesselType.CONTAINER)
                .flag("Panama").yearBuilt(2012).grossTonnage(new BigDecimal("54200.00")).build());
        Vessel coastal = vesselRepository.save(Vessel.builder()
                .name("MT Coastal Dawn").imoNumber("9521086").vesselType(VesselType.TANKER)
                .flag("Liberia").yearBuilt(2018).grossTonnage(new BigDecimal("29800.00")).build());

        // ---- equipment (a main engine with a child pump, plus a generator and a purifier)
        Equipment mainEngine = equipment(atlantic, null, "Main Engine", "ME-01", "Propulsion", "MAN B&W", "6S60ME-C",
                Criticality.CRITICAL, "4980.0", "4700.0");
        Equipment coolingPump = equipment(atlantic, mainEngine, "Jacket Cooling Water Pump", "ME-01-P1", "Pumps", "Alfa Laval", "LKH-40",
                Criticality.HIGH, "4980.0", "4700.0");
        Equipment generator = equipment(atlantic, null, "Auxiliary Generator No.1", "AG-01", "Power generation", "Wartsila", "W20",
                Criticality.HIGH, "12040.0", "11800.0");
        Equipment purifier = equipment(coastal, null, "Fuel Oil Purifier", "FO-PUR-01", "Purification", "Alfa Laval", "P625",
                Criticality.MEDIUM, "2210.0", "2100.0");

        // ---- tasks
        MaintenanceTask lubeOil = task(mainEngine, "Lube oil sample and filter change", "Take lube oil sample, change filters, record pressures.", "3.0");
        MaintenanceTask overhaul = task(mainEngine, "Cylinder unit inspection", "Inspect liners, rings and piston crowns through scavenge ports.", "12.0");
        MaintenanceTask pumpSeal = task(coolingPump, "Mechanical seal inspection", "Check for leakage, replace seal if weeping.", "2.0");
        MaintenanceTask genService = task(generator, "Generator 1000h service", "Change oil and filters, check valve clearances.", "8.0");
        MaintenanceTask purifierClean = task(purifier, "Bowl cleaning and disc stack inspection", "Strip and clean bowl, inspect discs and seals.", "4.0");

        // ---- schedules: one overdue, two due soon, one ok
        scheduleCalendar(lubeOil, 30, today.minusDays(40));
        scheduleHours(overhaul, IntervalType.RUNNING_HOURS, 6000, new BigDecimal("0.0"));
        scheduleHours(genService, IntervalType.OPERATING_HOURS, 1000, new BigDecimal("10850.0"));
        scheduleCalendar(pumpSeal, 90, today.minusDays(85));
        scheduleCalendar(purifierClean, 60, today.minusDays(10));

        // ---- spare parts
        SparePart filter = part("LF-6S60-01", "Lube oil filter element", "pcs", 6, 4, "182.50", "Store A / Shelf 2");
        SparePart seal = part("MS-LKH40", "Mechanical seal kit LKH-40", "kit", 1, 2, "640.00", "Store A / Shelf 5");
        part("GSK-W20-HD", "Cylinder head gasket W20", "pcs", 12, 4, "95.00", "Store B / Bin 7");

        // ---- work orders
        WorkOrder overdueJob = workOrder(1, mainEngine, lubeOil, "Lube oil sample and filter change", WorkOrderType.PREVENTIVE,
                WorkOrderPriority.HIGH, WorkOrderStatus.ASSIGNED, superintendent, engineer, today.minusDays(3));
        history(overdueJob, null, WorkOrderStatus.OPEN, superintendent, "Work order created");
        history(overdueJob, WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, superintendent, "Assigned to " + engineer.getFullName());

        WorkOrder leak = workOrder(2, coolingPump, null, "Coolant leak at pump seal", WorkOrderType.CORRECTIVE,
                WorkOrderPriority.CRITICAL, WorkOrderStatus.OPEN, engineer, null, today.plusDays(1));
        history(leak, null, WorkOrderStatus.OPEN, engineer, "Work order created");

        WorkOrder done = workOrder(3, generator, genService, "Generator 1000h service", WorkOrderType.PREVENTIVE,
                WorkOrderPriority.MEDIUM, WorkOrderStatus.COMPLETED, superintendent, engineer, today.minusDays(20));
        done.setStartedAt(Instant.now().minus(21, ChronoUnit.DAYS));
        done.setCompletedAt(Instant.now().minus(20, ChronoUnit.DAYS));
        done.setCompletionRemarks("Oil and filters changed, valve clearances within limits.");
        done.setActualHours(new BigDecimal("7.5"));
        done.getParts().add(WorkOrderPart.builder().workOrder(done).sparePart(filter).quantityUsed(2)
                .unitCostAtUse(filter.getUnitCost()).build());
        workOrderRepository.save(done);
        history(done, null, WorkOrderStatus.OPEN, superintendent, "Work order created");
        history(done, WorkOrderStatus.OPEN, WorkOrderStatus.ASSIGNED, superintendent, "Assigned to " + engineer.getFullName());
        history(done, WorkOrderStatus.ASSIGNED, WorkOrderStatus.IN_PROGRESS, engineer, "Work started");
        history(done, WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.COMPLETED, engineer, done.getCompletionRemarks());
        recordRepository.save(MaintenanceRecord.builder()
                .workOrder(done).equipment(generator).performedBy(engineer).completedAt(done.getCompletedAt())
                .runningHoursAtService(new BigDecimal("11900.0")).operatingHoursAtService(new BigDecimal("11650.0"))
                .actualHours(done.getActualHours()).partsCost(done.partsCost()).workPerformed(done.getCompletionRemarks())
                .build());
    }

    private User user(String email, String password, String first, String last, Role role) {
        return userRepository.save(User.builder().email(email).password(passwordEncoder.encode(password))
                .firstName(first).lastName(last).roles(Set.of(role)).build());
    }

    private Equipment equipment(Vessel vessel, Equipment parent, String name, String tag, String category, String maker,
                                String model, Criticality criticality, String running, String operating) {
        return equipmentRepository.save(Equipment.builder().vessel(vessel).parent(parent).name(name).tagNumber(tag)
                .category(category).manufacturer(maker).model(model).criticality(criticality)
                .runningHours(new BigDecimal(running)).operatingHours(new BigDecimal(operating)).build());
    }

    private MaintenanceTask task(Equipment equipment, String title, String description, String hours) {
        return taskRepository.save(MaintenanceTask.builder().equipment(equipment).title(title)
                .description(description).estimatedHours(new BigDecimal(hours)).build());
    }

    private void scheduleCalendar(MaintenanceTask task, int days, LocalDate lastDone) {
        scheduleRepository.save(MaintenanceSchedule.builder().task(task).intervalType(IntervalType.CALENDAR_DAYS)
                .intervalValue(days).lastDoneDate(lastDone).nextDueDate(DueCalculator.nextDueDate(lastDone, days)).build());
    }

    private void scheduleHours(MaintenanceTask task, IntervalType type, int hours, BigDecimal lastDone) {
        scheduleRepository.save(MaintenanceSchedule.builder().task(task).intervalType(type).intervalValue(hours)
                .lastDoneHours(lastDone).nextDueHours(DueCalculator.nextDueHours(lastDone, hours)).build());
    }

    private SparePart part(String number, String name, String unit, int stock, int reorder, String cost, String location) {
        return sparePartRepository.save(SparePart.builder().partNumber(number).name(name).unit(unit)
                .quantityInStock(stock).reorderLevel(reorder).unitCost(new BigDecimal(cost)).storageLocation(location).build());
    }

    private WorkOrder workOrder(int seq, Equipment equipment, MaintenanceTask task, String title, WorkOrderType type,
                                WorkOrderPriority priority, WorkOrderStatus status, User creator, User assignee, LocalDate due) {
        return workOrderRepository.save(WorkOrder.builder()
                .workOrderNumber(String.format("WO-%d-%05d", LocalDate.now().getYear(), seq))
                .equipment(equipment).task(task).title(title).type(type).priority(priority).status(status)
                .createdBy(creator).assignedTo(assignee).dueDate(due).build());
    }

    private void history(WorkOrder wo, WorkOrderStatus from, WorkOrderStatus to, User by, String notes) {
        historyRepository.save(WorkOrderStatusHistory.builder().workOrder(wo).fromStatus(from).toStatus(to)
                .changedBy(by).notes(notes).changedAt(Instant.now()).build());
    }
}
