const currency = import.meta.env.VITE_CURRENCY || "USD";

export const label = (value) => {
  if (!value) return "";
  const text = String(value).toLowerCase().replace(/_/g, " ");
  return text.charAt(0).toUpperCase() + text.slice(1);
};

export const fmtDate = (value) => {
  if (!value) return "";
  const d = /^\d{4}-\d{2}-\d{2}$/.test(value) ? new Date(`${value}T00:00:00`) : new Date(value);
  return d.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
};

export const fmtDateTime = (value) =>
  value
    ? new Date(value).toLocaleString(undefined, { day: "numeric", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" })
    : "";

export const fmtHours = (value) =>
  value === null || value === undefined ? "" : Number(value).toLocaleString(undefined, { minimumFractionDigits: 1, maximumFractionDigits: 1 });

export const fmtMoney = (value) =>
  value === null || value === undefined ? "" : Number(value).toLocaleString(undefined, { style: "currency", currency });

export const fmtInterval = (schedule) => {
  const n = schedule.intervalValue;
  if (schedule.intervalType === "CALENDAR_DAYS") return `every ${n} day${n === 1 ? "" : "s"}`;
  if (schedule.intervalType === "RUNNING_HOURS") return `every ${n.toLocaleString()} running hours`;
  return `every ${n.toLocaleString()} operating hours`;
};

/** Human wording for how far away (or past) a schedule's due point is. */
export const dueText = (s) => {
  if (s.daysRemaining !== null && s.daysRemaining !== undefined) {
    const d = s.daysRemaining;
    if (d < 0) return `${-d} day${d === -1 ? "" : "s"} overdue`;
    if (d === 0) return "due today";
    return `in ${d} day${d === 1 ? "" : "s"}`;
  }
  if (s.hoursRemaining !== null && s.hoursRemaining !== undefined) {
    const h = Number(s.hoursRemaining);
    if (h <= 0) return `${fmtHours(-h)} h overdue`;
    return `in ${fmtHours(h)} h`;
  }
  return "";
};

/** Share of the interval already used, 0 to 1 (1 when overdue). */
export const dueProgress = (s) => {
  const interval = s.intervalValue || 1;
  let remaining = null;
  if (s.daysRemaining !== null && s.daysRemaining !== undefined) remaining = s.daysRemaining;
  else if (s.hoursRemaining !== null && s.hoursRemaining !== undefined) remaining = Number(s.hoursRemaining);
  if (remaining === null) return 0;
  return Math.min(1, Math.max(0, (interval - remaining) / interval));
};

export const toneOf = (value) => {
  switch (value) {
    case "OVERDUE":
    case "CRITICAL":
    case "OUT_OF_SERVICE":
    case "EMERGENCY":
    case "CANCELLED":
    case "DECOMMISSIONED":
      return "danger";
    case "DUE_SOON":
    case "HIGH":
    case "ON_HOLD":
    case "UNDER_MAINTENANCE":
    case "IN_DRYDOCK":
    case "LAID_UP":
      return "warn";
    case "OK":
    case "ACTIVE":
    case "OPERATIONAL":
    case "COMPLETED":
      return "ok";
    case "IN_PROGRESS":
    case "ASSIGNED":
    case "MEDIUM":
    case "PREVENTIVE":
      return "info";
    default:
      return "neutral";
  }
};

export const VESSEL_TYPES = ["CARGO", "TANKER", "CONTAINER", "BULK_CARRIER", "PASSENGER", "TUG", "OFFSHORE_SUPPORT", "FISHING", "OTHER"];
export const VESSEL_STATUSES = ["ACTIVE", "IN_DRYDOCK", "LAID_UP", "DECOMMISSIONED"];
export const CRITICALITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
export const EQUIPMENT_STATUSES = ["OPERATIONAL", "UNDER_MAINTENANCE", "OUT_OF_SERVICE"];
export const WO_TYPES = ["PREVENTIVE", "CORRECTIVE", "INSPECTION", "EMERGENCY"];
export const WO_PRIORITIES = ["LOW", "MEDIUM", "HIGH", "CRITICAL"];
export const WO_STATUSES = ["OPEN", "ASSIGNED", "IN_PROGRESS", "ON_HOLD", "COMPLETED", "CANCELLED"];
export const INTERVAL_TYPES = ["CALENDAR_DAYS", "RUNNING_HOURS", "OPERATING_HOURS"];
export const ROLES = ["ADMIN", "SUPERINTENDENT", "ENGINEER"];
