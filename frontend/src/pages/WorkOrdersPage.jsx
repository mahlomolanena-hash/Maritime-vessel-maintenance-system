import { Link } from "react-router-dom";
import { api } from "../api/client";
import { Alert, Empty, Field, Loading, PageHeader, Pagination, Tag } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useFetch } from "../hooks/useFetch";
import { useFilters } from "../hooks/useFilters";
import { WO_PRIORITIES, WO_STATUSES, fmtDate, label } from "../utils/format";

export default function WorkOrdersPage() {
  const { userId } = useAuth();
  const filters = useFilters();
  const status = filters.get("status");
  const priority = filters.get("priority");
  const q = filters.get("q");
  const overdue = filters.get("overdue") === "1";
  const mine = filters.get("mine") === "1";
  const equipmentId = filters.get("equipmentId");

  const list = useFetch(
    () => api.workOrders({ status, priority, q, overdue, equipmentId, assignedToId: mine ? userId : "", page: filters.page, size: 20 }),
    filters.key
  );

  return (
    <>
      <PageHeader title="Work orders" subtitle="Corrective, preventive and inspection jobs across the fleet.">
        <Link className="btn btn-primary" to="/work-orders/new">New work order</Link>
      </PageHeader>

      <div className="filters">
        <Field id="q" label="Search"><input id="q" type="search" placeholder="Title or number" value={q} onChange={(e) => filters.set("q", e.target.value)} /></Field>
        <Field id="status" label="Status">
          <select id="status" value={status} onChange={(e) => filters.set("status", e.target.value)}>
            <option value="">All</option>
            {WO_STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
          </select>
        </Field>
        <Field id="priority" label="Priority">
          <select id="priority" value={priority} onChange={(e) => filters.set("priority", e.target.value)}>
            <option value="">All</option>
            {WO_PRIORITIES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
          </select>
        </Field>
        <label className="inline-check"><input type="checkbox" checked={overdue} onChange={(e) => filters.set("overdue", e.target.checked)} /> Overdue only</label>
        <label className="inline-check"><input type="checkbox" checked={mine} onChange={(e) => filters.set("mine", e.target.checked)} /> Assigned to me</label>
      </div>

      <Alert error={list.error} />
      {list.loading && !list.data && <Loading />}
      {list.data && (
        <>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Number</th><th>Work order</th><th>Equipment</th><th>Priority</th><th>Status</th><th>Assigned to</th><th>Due</th></tr></thead>
              <tbody>
                {list.data.content.map((wo) => (
                  <tr key={wo.id}>
                    <td className="mono nowrap"><Link to={`/work-orders/${wo.id}`}>{wo.workOrderNumber}</Link></td>
                    <td><strong>{wo.title}</strong><span className="sub">{label(wo.type)}</span></td>
                    <td><Link to={`/equipment/${wo.equipmentId}`}>{wo.equipmentName}</Link><span className="sub">{wo.vesselName}</span></td>
                    <td><Tag value={wo.priority} /></td>
                    <td><Tag value={wo.status} /></td>
                    <td>{wo.assignedToName || <span className="muted">Unassigned</span>}</td>
                    <td className={`nowrap ${wo.overdue ? "tone-danger" : ""}`}>{wo.dueDate ? fmtDate(wo.dueDate) : ""}{wo.overdue && <span className="sub tone-danger">Overdue</span>}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {list.data.content.length === 0 && <Empty>No work orders match these filters.</Empty>}
          </div>
          <Pagination page={list.data} onPage={(p) => filters.set("page", String(p))} />
        </>
      )}
    </>
  );
}
