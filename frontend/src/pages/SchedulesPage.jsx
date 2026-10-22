import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import GenerateWorkOrderButton from "../components/GenerateWorkOrderButton";
import { Alert, DueGauge, Empty, Field, Loading, PageHeader } from "../components/ui";
import { useFetch } from "../hooks/useFetch";
import { useFilters } from "../hooks/useFilters";
import { fmtDate, fmtHours, fmtInterval } from "../utils/format";

export default function SchedulesPage() {
  const filters = useFilters();
  const dueStatus = filters.get("dueStatus");
  const vesselId = filters.get("vesselId");
  const [actionError, setActionError] = useState(null);

  const vessels = useFetch(() => api.vessels({ size: 100 }));
  const list = useFetch(() => api.schedules({ dueStatus, vesselId }), filters.key);

  return (
    <>
      <PageHeader title="Planned maintenance" subtitle="Preventive schedules by calendar days, running hours or operating hours. Most urgent first." />

      <div className="filters">
        <Field id="due" label="Due status">
          <select id="due" value={dueStatus} onChange={(e) => filters.set("dueStatus", e.target.value)}>
            <option value="">All</option>
            <option value="OVERDUE">Overdue</option>
            <option value="DUE_SOON">Due soon</option>
            <option value="OK">Not yet due</option>
          </select>
        </Field>
        <Field id="vessel" label="Vessel">
          <select id="vessel" value={vesselId} onChange={(e) => filters.set("vesselId", e.target.value)}>
            <option value="">All vessels</option>
            {vessels.data?.content.map((v) => <option key={v.id} value={v.id}>{v.name}</option>)}
          </select>
        </Field>
      </div>

      <Alert error={list.error || actionError} />
      {list.loading && !list.data && <Loading />}
      {list.data && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Task</th><th>Equipment</th><th>Repeats</th><th>Next due</th><th>Status</th><th /></tr></thead>
            <tbody>
              {list.data.map((sc) => (
                <tr key={sc.id}>
                  <td><strong>{sc.taskTitle}</strong>{!sc.active && <span className="sub">Inactive</span>}</td>
                  <td><Link to={`/equipment/${sc.equipmentId}`}>{sc.equipmentName}</Link><span className="sub">{sc.vesselName}</span></td>
                  <td>{fmtInterval(sc)}</td>
                  <td className="nowrap">{sc.nextDueDate ? fmtDate(sc.nextDueDate) : <span className="mono">{fmtHours(sc.nextDueHours)} h</span>}</td>
                  <td><DueGauge schedule={sc} /></td>
                  <td><GenerateWorkOrderButton schedule={sc} onError={setActionError} /></td>
                </tr>
              ))}
            </tbody>
          </table>
          {list.data.length === 0 && <Empty>No schedules match these filters.</Empty>}
        </div>
      )}
    </>
  );
}
