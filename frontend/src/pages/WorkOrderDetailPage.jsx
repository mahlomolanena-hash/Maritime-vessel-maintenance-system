import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import { Alert, Empty, Field, Loading, PageHeader, Tag } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useFetch } from "../hooks/useFetch";
import { fmtDate, fmtDateTime, fmtHours, fmtMoney, label } from "../utils/format";

export default function WorkOrderDetailPage() {
  const { id } = useParams();
  const { isManager, userId } = useAuth();
  const wo = useFetch(() => api.workOrder(id), id);
  const history = useFetch(() => api.workOrderHistory(id), id);
  const users = useFetch(() => (isManager ? api.users() : Promise.resolve([])));

  const [panel, setPanel] = useState(null); // "assign" | "hold" | "complete" | "cancel"
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const [text, setText] = useState("");
  const [hours, setHours] = useState("");
  const [assignee, setAssignee] = useState("");

  const w = wo.data;
  const status = w?.status;
  const parts = useFetch(
    () => (status === "IN_PROGRESS" ? api.spareParts({ size: 100 }) : Promise.resolve(null)),
    `${id}-${status}`
  );

  if (wo.loading && !w) return <Loading />;
  if (wo.error) return <Alert error={wo.error} />;

  const canWork = isManager || w.assignedToId === userId;

  const refresh = () => {
    wo.reload();
    history.reload();
  };

  const run = async (fn) => {
    setError(null);
    setBusy(true);
    try {
      await fn();
      setPanel(null);
      setText("");
      setHours("");
      refresh();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  const openPanel = (name) => {
    setError(null);
    setText("");
    setPanel(panel === name ? null : name);
  };

  return (
    <>
      <div className="breadcrumb"><Link to="/work-orders">Work orders</Link></div>
      <PageHeader title={w.title} subtitle={<span className="mono">{w.workOrderNumber}</span>}>
        <Tag value={w.status} />
      </PageHeader>

      <Alert error={error} />

      <dl className="panel facts">
        <div><dt>Priority</dt><dd><Tag value={w.priority} /></dd></div>
        <div><dt>Type</dt><dd>{label(w.type)}</dd></div>
        <div><dt>Equipment</dt><dd><Link to={`/equipment/${w.equipmentId}`}>{w.equipmentName}</Link></dd></div>
        <div><dt>Vessel</dt><dd><Link to={`/vessels/${w.vesselId}`}>{w.vesselName}</Link></dd></div>
        <div><dt>Raised by</dt><dd>{w.createdByName}</dd></div>
        <div><dt>Assigned to</dt><dd>{w.assignedToName || "Unassigned"}</dd></div>
        <div><dt>Due</dt><dd className={w.overdue ? "tone-danger" : ""}>{w.dueDate ? fmtDate(w.dueDate) : "No due date"}{w.overdue && " (overdue)"}</dd></div>
        <div><dt>Started</dt><dd>{w.startedAt ? fmtDateTime(w.startedAt) : "Not started"}</dd></div>
        {w.completedAt && <div><dt>Completed</dt><dd>{fmtDateTime(w.completedAt)}</dd></div>}
        {w.actualHours !== null && w.actualHours !== undefined && <div><dt>Time spent</dt><dd>{fmtHours(w.actualHours)} h</dd></div>}
      </dl>

      {w.description && (
        <section className="panel" style={{ marginTop: 14 }}>
          <h3 style={{ marginBottom: 6 }}>Instructions</h3>
          <p style={{ whiteSpace: "pre-wrap" }}>{w.description}</p>
        </section>
      )}

      {w.completionRemarks && (
        <section className="panel" style={{ marginTop: 14 }}>
          <h3 style={{ marginBottom: 6 }}>Work performed</h3>
          <p style={{ whiteSpace: "pre-wrap" }}>{w.completionRemarks}</p>
        </section>
      )}

      {/* ---------------------------------------------------------------- actions */}
      {!["COMPLETED", "CANCELLED"].includes(w.status) && (
        <section className="panel" style={{ marginTop: 14 }}>
          <h3 style={{ marginBottom: 10 }}>Actions</h3>
          <div className="action-bar">
            {w.status === "ASSIGNED" && canWork && (
              <button className="btn btn-primary" disabled={busy} onClick={() => run(() => api.startWorkOrder(w.id))}>Start work</button>
            )}
            {w.status === "IN_PROGRESS" && canWork && (
              <>
                <button className="btn btn-primary" onClick={() => openPanel("complete")}>Complete</button>
                <button className="btn btn-quiet" onClick={() => openPanel("hold")}>Put on hold</button>
              </>
            )}
            {w.status === "ON_HOLD" && canWork && (
              <button className="btn btn-primary" disabled={busy} onClick={() => run(() => api.resumeWorkOrder(w.id))}>Resume work</button>
            )}
            {isManager && (
              <button className="btn btn-quiet" onClick={() => openPanel("assign")}>{w.assignedToId ? "Reassign" : "Assign"}</button>
            )}
            {isManager && w.status === "ASSIGNED" && (
              <button className="btn btn-quiet" disabled={busy} onClick={() => run(() => api.unassignWorkOrder(w.id))}>Return to queue</button>
            )}
            {isManager && <button className="btn btn-danger" onClick={() => openPanel("cancel")}>Cancel work order</button>}
            {!canWork && !isManager && <span className="muted">Only the assigned engineer or a superintendent can progress this work order.</span>}
          </div>

          {panel === "assign" && (
            <form className="action-panel" onSubmit={(e) => { e.preventDefault(); run(() => api.assignWorkOrder(w.id, Number(assignee))); }}>
              <Field id="a-user" label="Assign to">
                <select id="a-user" required value={assignee} onChange={(e) => setAssignee(e.target.value)}>
                  <option value="">Choose a person</option>
                  {users.data?.filter((u) => u.enabled).map((u) => <option key={u.id} value={u.id}>{u.firstName} {u.lastName} ({u.roles.map(label).join(", ")})</option>)}
                </select>
              </Field>
              <div className="form-actions"><button className="btn btn-primary" disabled={busy}>Save assignment</button></div>
            </form>
          )}

          {panel === "hold" && (
            <form className="action-panel" onSubmit={(e) => { e.preventDefault(); run(() => api.holdWorkOrder(w.id, text)); }}>
              <Field id="h-notes" label="Why is it on hold?"><textarea id="h-notes" required value={text} onChange={(e) => setText(e.target.value)} placeholder="Waiting for spare part, vessel in heavy weather…" /></Field>
              <div className="form-actions"><button className="btn btn-primary" disabled={busy}>Put on hold</button></div>
            </form>
          )}

          {panel === "complete" && (
            <form className="action-panel" onSubmit={(e) => { e.preventDefault(); run(() => api.completeWorkOrder(w.id, { completionRemarks: text, actualHours: hours === "" ? null : Number(hours) })); }}>
              <div className="form-grid">
                <div className="field wide"><label htmlFor="c-remarks">Work performed</label><textarea id="c-remarks" required value={text} onChange={(e) => setText(e.target.value)} placeholder="What was done, what was found, readings taken…" /></div>
                <Field id="c-hours" label="Time spent (hours)"><input id="c-hours" type="number" step="0.1" min="0" value={hours} onChange={(e) => setHours(e.target.value)} /></Field>
              </div>
              <p className="hint" style={{ marginTop: 8 }}>
                Completing writes the maintenance record{w.scheduleId ? " and restarts the preventive schedule" : ""}. Record parts and hour-meter readings first.
              </p>
              <div className="form-actions"><button className="btn btn-primary" disabled={busy}>Complete work order</button></div>
            </form>
          )}

          {panel === "cancel" && (
            <form className="action-panel" onSubmit={(e) => { e.preventDefault(); run(() => api.cancelWorkOrder(w.id, text)); }}>
              <Field id="x-notes" label="Reason for cancelling"><textarea id="x-notes" required value={text} onChange={(e) => setText(e.target.value)} /></Field>
              <div className="form-actions"><button className="btn btn-danger" disabled={busy}>Cancel work order</button></div>
            </form>
          )}
        </section>
      )}

      {/* ---------------------------------------------------------------- parts */}
      <section className="section">
        <header>
          <h2>Spare parts used</h2>
          {w.parts.length > 0 && <span className="muted">Total {fmtMoney(w.partsCost)}</span>}
        </header>
        <div className="table-wrap">
          <table>
            <thead><tr><th>Part</th><th className="right">Quantity</th><th className="right">Unit cost</th><th className="right">Line cost</th><th /></tr></thead>
            <tbody>
              {w.parts.map((p) => (
                <tr key={p.id}>
                  <td><strong>{p.partName}</strong><span className="sub mono">{p.partNumber}</span></td>
                  <td className="right">{p.quantityUsed}</td>
                  <td className="right">{fmtMoney(p.unitCostAtUse)}</td>
                  <td className="right">{fmtMoney(p.lineCost)}</td>
                  <td className="right">
                    {w.status === "IN_PROGRESS" && canWork && (
                      <button className="btn btn-quiet btn-small" disabled={busy} onClick={() => run(() => api.removePart(w.id, p.id))}>Return to stock</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {w.parts.length === 0 && <Empty>No parts recorded.</Empty>}
        </div>
        {w.status === "IN_PROGRESS" && canWork && parts.data && (
          <AddPart parts={parts.data.content} onAdd={(partId, qty) => run(() => api.addPart(w.id, partId, qty))} busy={busy} />
        )}
      </section>

      {/* ---------------------------------------------------------------- history */}
      <section className="section">
        <header><h2>Status history</h2></header>
        <div className="panel">
          <ul className="timeline">
            {history.data?.map((h) => (
              <li key={h.id}>
                <strong>
                  {h.fromStatus && h.fromStatus !== h.toStatus ? `${label(h.fromStatus)} to ${label(h.toStatus)}` : label(h.toStatus)}
                </strong>
                <span className="when"> · {fmtDateTime(h.changedAt)}{h.changedBy && ` · ${h.changedBy}`}</span>
                {h.notes && <div>{h.notes}</div>}
              </li>
            ))}
          </ul>
          {history.data?.length === 0 && <Empty>No history yet.</Empty>}
        </div>
      </section>
    </>
  );
}

function AddPart({ parts, onAdd, busy }) {
  const [partId, setPartId] = useState("");
  const [qty, setQty] = useState("1");

  return (
    <form
      className="panel"
      style={{ marginTop: 12 }}
      onSubmit={(e) => {
        e.preventDefault();
        onAdd(Number(partId), Number(qty));
        setQty("1");
      }}
    >
      <div className="filters" style={{ marginBottom: 0 }}>
        <Field id="p-part" label="Add a spare part">
          <select id="p-part" required value={partId} onChange={(e) => setPartId(e.target.value)}>
            <option value="">Choose a part</option>
            {parts.map((p) => (
              <option key={p.id} value={p.id} disabled={p.quantityInStock === 0}>
                {p.name} ({p.partNumber}), {p.quantityInStock} {p.unit} in stock
              </option>
            ))}
          </select>
        </Field>
        <Field id="p-qty" label="Quantity"><input id="p-qty" type="number" min="1" required value={qty} onChange={(e) => setQty(e.target.value)} /></Field>
        <button className="btn btn-quiet" disabled={busy}>Add to work order</button>
      </div>
    </form>
  );
}
