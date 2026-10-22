import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import GenerateWorkOrderButton from "../components/GenerateWorkOrderButton";
import { Alert, DueGauge, Empty, Loading, PageHeader, Tag } from "../components/ui";
import { fmtDate, label } from "../utils/format";
import { useFetch } from "../hooks/useFetch";

export default function DashboardPage() {
  const summary = useFetch(() => api.dashboard());
  const due = useFetch(() => api.dueSchedules());
  const overdue = useFetch(() => api.workOrders({ overdue: true, size: 6 }));
  const [actionError, setActionError] = useState(null);

  const s = summary.data;
  const openWork = s ? ["OPEN", "ASSIGNED", "IN_PROGRESS", "ON_HOLD"].reduce((n, k) => n + (s.workOrdersByStatus[k] || 0), 0) : 0;

  return (
    <>
      <PageHeader title="Fleet watch" subtitle="What needs attention across the fleet right now." />
      <Alert error={summary.error} />

      {s && (
        <div className="figures">
          <Link className="figure" to="/vessels">
            <span className="value">{s.activeVessels}</span>
            <span className="what">Active vessels</span>
          </Link>
          <Link className="figure" to="/work-orders">
            <span className="value">{openWork}</span>
            <span className="what">Open work orders</span>
          </Link>
          <Link className="figure" to="/work-orders?overdue=1">
            <span className={`value ${s.overdueWorkOrders ? "tone-danger" : ""}`}>{s.overdueWorkOrders}</span>
            <span className="what">Overdue work orders</span>
          </Link>
          <Link className="figure" to="/schedules?dueStatus=OVERDUE">
            <span className={`value ${s.overdueSchedules ? "tone-danger" : ""}`}>{s.overdueSchedules}</span>
            <span className="what">Overdue maintenance</span>
          </Link>
          <Link className="figure" to="/schedules?dueStatus=DUE_SOON">
            <span className={`value ${s.dueSoonSchedules ? "tone-warn" : ""}`}>{s.dueSoonSchedules}</span>
            <span className="what">Due soon</span>
          </Link>
          <Link className="figure" to="/spare-parts?lowStock=1">
            <span className={`value ${s.lowStockParts ? "tone-warn" : ""}`}>{s.lowStockParts}</span>
            <span className="what">Parts low on stock</span>
          </Link>
        </div>
      )}

      <div className="cols section">
        <section>
          <header>
            <h2>Maintenance due</h2>
            <Link to="/schedules">All planned maintenance</Link>
          </header>
          <Alert error={actionError} />
          {due.loading && <Loading />}
          {due.error && <Alert error={due.error} />}
          {due.data && due.data.length === 0 && <div className="panel"><Empty>Nothing is overdue or due soon.</Empty></div>}
          {due.data && due.data.length > 0 && (
            <div className="watch">
              {due.data.slice(0, 10).map((sc) => (
                <div className="watch-row" key={sc.id}>
                  <div className="what">
                    <strong>{sc.taskTitle}</strong>
                    <span>
                      <Link to={`/equipment/${sc.equipmentId}`}>{sc.equipmentName}</Link> on {sc.vesselName}
                    </span>
                  </div>
                  <DueGauge schedule={sc} />
                  <GenerateWorkOrderButton schedule={sc} onError={setActionError} />
                </div>
              ))}
            </div>
          )}
        </section>

        <section>
          <header>
            <h2>Overdue work orders</h2>
            <Link to="/work-orders?overdue=1">View all</Link>
          </header>
          {overdue.loading && <Loading />}
          {overdue.error && <Alert error={overdue.error} />}
          {overdue.data && overdue.data.content.length === 0 && <div className="panel"><Empty>No work orders are past their due date.</Empty></div>}
          {overdue.data && overdue.data.content.length > 0 && (
            <div className="watch">
              {overdue.data.content.map((wo) => (
                <div className="watch-row" style={{ gridTemplateColumns: "1fr auto" }} key={wo.id}>
                  <div className="what">
                    <strong>
                      <Link to={`/work-orders/${wo.id}`}>{wo.title}</Link>
                    </strong>
                    <span>
                      <span className="mono">{wo.workOrderNumber}</span> · {wo.equipmentName}, {wo.vesselName}
                    </span>
                    <span>
                      Due {fmtDate(wo.dueDate)} · {wo.assignedToName ? `assigned to ${wo.assignedToName}` : "unassigned"}
                    </span>
                  </div>
                  <div className="right">
                    <Tag value={wo.priority} text={label(wo.priority)} />
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </>
  );
}
