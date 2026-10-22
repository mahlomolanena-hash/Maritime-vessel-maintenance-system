import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../context/AuthContext";

/** Managers can turn a due schedule into a preventive work order in one click. */
export default function GenerateWorkOrderButton({ schedule, onError }) {
  const { isManager } = useAuth();
  const navigate = useNavigate();
  const [busy, setBusy] = useState(false);

  if (!isManager || !schedule.active) return null;

  const handleClick = async () => {
    setBusy(true);
    try {
      const wo = await api.generateWorkOrder(schedule.id, { priority: schedule.dueStatus === "OVERDUE" ? "HIGH" : "MEDIUM" });
      navigate(`/work-orders/${wo.id}`);
    } catch (err) {
      onError?.(err);
      setBusy(false);
    }
  };

  return (
    <button className="btn btn-quiet btn-small" disabled={busy} onClick={handleClick}>
      {busy ? "Creating…" : "Create work order"}
    </button>
  );
}
