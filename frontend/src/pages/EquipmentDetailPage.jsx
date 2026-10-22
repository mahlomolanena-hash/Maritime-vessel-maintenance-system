import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import GenerateWorkOrderButton from "../components/GenerateWorkOrderButton";
import { Alert, DueGauge, Empty, Field, Loading, PageHeader, Tag } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useFetch } from "../hooks/useFetch";
import { useForm } from "../hooks/useForm";
import { INTERVAL_TYPES, fmtDate, fmtDateTime, fmtHours, fmtInterval, label } from "../utils/format";

export default function EquipmentDetailPage() {
  const { id } = useParams();
  const { isManager } = useAuth();
  const equipment = useFetch(() => api.equipment(id), id);
  const tasks = useFetch(() => api.equipmentTasks(id), id);
  const schedules = useFetch(() => api.equipmentSchedules(id), id);
  const readings = useFetch(() => api.equipmentHours(id, { size: 5 }), id);
  const work = useFetch(() => api.workOrders({ equipmentId: id, size: 10 }), id);
  const history = useFetch(() => api.records({ equipmentId: id, size: 10 }), id);
  const [actionError, setActionError] = useState(null);
  const [panel, setPanel] = useState(null); // "task" | "schedule" | null

  const e = equipment.data;
  if (equipment.loading && !e) return <Loading />;
  if (equipment.error) return <Alert error={equipment.error} />;

  const refreshAll = () => {
    equipment.reload();
    tasks.reload();
    schedules.reload();
  };

  return (
    <>
      <div className="breadcrumb">
        <Link to="/vessels">Vessels</Link> / <Link to={`/vessels/${e.vesselId}`}>{e.vesselName}</Link>
      </div>
      <PageHeader title={e.name} subtitle={[e.manufacturer, e.model].filter(Boolean).join(" ") || undefined}>
        <Link className="btn btn-quiet" to={`/work-orders/new?equipmentId=${e.id}&vesselId=${e.vesselId}`}>Raise work order</Link>
      </PageHeader>

      <div className="cols">
        <dl className="panel facts">
          <div><dt>Tag number</dt><dd className="mono">{e.tagNumber}</dd></div>
          <div><dt>Status</dt><dd><Tag value={e.status} /></dd></div>
          <div><dt>Criticality</dt><dd><Tag value={e.criticality} /></dd></div>
          <div><dt>Part of</dt><dd>{e.parentId ? <Link to={`/equipment/${e.parentId}`}>{e.parentName}</Link> : "Top level"}</dd></div>
          <div><dt>Category</dt><dd>{e.category || "Not set"}</dd></div>
          <div><dt>Serial number</dt><dd className="mono">{e.serialNumber || "Not set"}</dd></div>
          <div><dt>Location</dt><dd>{e.location || "Not set"}</dd></div>
          <div><dt>Installed</dt><dd>{e.installedDate ? fmtDate(e.installedDate) : "Not set"}</dd></div>
        </dl>

        <HourMeters equipment={e} readings={readings} onRecorded={() => { equipment.reload(); readings.reload(); schedules.reload(); }} />
      </div>

      <section className="section">
        <header>
          <h2>Planned maintenance</h2>
          {isManager && (
            <div className="page-actions">
              <button className="btn btn-quiet btn-small" onClick={() => setPanel(panel === "task" ? null : "task")}>New task</button>
              <button className="btn btn-quiet btn-small" onClick={() => setPanel(panel === "schedule" ? null : "schedule")} disabled={!tasks.data?.length}>New schedule</button>
            </div>
          )}
        </header>
        <Alert error={actionError} />
        {panel === "task" && <TaskForm equipmentId={e.id} onSaved={() => { setPanel(null); refreshAll(); }} />}
        {panel === "schedule" && <ScheduleForm tasks={tasks.data || []} onSaved={() => { setPanel(null); refreshAll(); }} />}

        {schedules.data && schedules.data.length === 0 && <div className="panel"><Empty>No preventive schedules yet.</Empty></div>}
        {schedules.data && schedules.data.length > 0 && (
          <div className="watch">
            {schedules.data.map((sc) => (
              <div className="watch-row" key={sc.id}>
                <div className="what">
                  <strong>{sc.taskTitle}</strong>
                  <span>{fmtInterval(sc)}{!sc.active && " · inactive"}</span>
                  <span>
                    {sc.nextDueDate ? `Next due ${fmtDate(sc.nextDueDate)}` : `Next due at ${fmtHours(sc.nextDueHours)} h`}
                  </span>
                </div>
                <DueGauge schedule={sc} />
                <GenerateWorkOrderButton schedule={sc} onError={setActionError} />
              </div>
            ))}
          </div>
        )}

        {tasks.data && tasks.data.length > 0 && (
          <p className="muted" style={{ marginTop: 10 }}>
            Tasks defined: {tasks.data.map((t) => t.title).join(", ")}
          </p>
        )}
      </section>

      <div className="cols cols-even section">
        <section>
          <header><h2>Work orders</h2><Link to={`/work-orders?equipmentId=${e.id}`}>All</Link></header>
          {work.data && work.data.content.length === 0 && <div className="panel"><Empty>No work orders for this equipment.</Empty></div>}
          {work.data && work.data.content.length > 0 && (
            <div className="watch">
              {work.data.content.map((wo) => (
                <div className="watch-row" style={{ gridTemplateColumns: "1fr auto" }} key={wo.id}>
                  <div className="what">
                    <strong><Link to={`/work-orders/${wo.id}`}>{wo.title}</Link></strong>
                    <span><span className="mono">{wo.workOrderNumber}</span>{wo.dueDate && ` · due ${fmtDate(wo.dueDate)}`}</span>
                  </div>
                  <Tag value={wo.status} />
                </div>
              ))}
            </div>
          )}
        </section>

        <section>
          <header><h2>Maintenance history</h2><Link to={`/records?equipmentId=${e.id}`}>All</Link></header>
          {history.data && history.data.content.length === 0 && <div className="panel"><Empty>No completed maintenance yet.</Empty></div>}
          {history.data && history.data.content.length > 0 && (
            <div className="watch">
              {history.data.content.map((r) => (
                <div className="watch-row" style={{ gridTemplateColumns: "1fr auto" }} key={r.id}>
                  <div className="what">
                    <strong><Link to={`/work-orders/${r.workOrderId}`}>{r.title}</Link></strong>
                    <span>{r.performedBy || "Unknown"} · at {fmtHours(r.runningHoursAtService)} running h</span>
                  </div>
                  <span className="muted nowrap">{fmtDateTime(r.completedAt)}</span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </>
  );
}

function HourMeters({ equipment, readings, onRecorded }) {
  const [values, bind, setValues] = useForm({ meterType: "RUNNING", hours: "", notes: "" });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (ev) => {
    ev.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.recordHours(equipment.id, { meterType: values.meterType, hours: Number(values.hours), notes: values.notes || null });
      setValues({ meterType: values.meterType, hours: "", notes: "" });
      onRecorded();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="panel">
      <h3 style={{ marginBottom: 12 }}>Hour meters</h3>
      <div className="meters">
        <div className="meter"><div className="value">{fmtHours(equipment.runningHours)}</div><div className="what">Running hours</div></div>
        <div className="meter"><div className="value">{fmtHours(equipment.operatingHours)}</div><div className="what">Operating hours</div></div>
      </div>
      <form onSubmit={submit} className="action-panel">
        <Alert error={error} />
        <div className="form-grid" style={{ gridTemplateColumns: "1fr 1fr" }}>
          <Field id="m-type" label="Meter"><select id="m-type" {...bind("meterType")}><option value="RUNNING">Running</option><option value="OPERATING">Operating</option></select></Field>
          <Field id="m-hours" label="New reading (hours)"><input id="m-hours" type="number" step="0.1" min="0" required {...bind("hours")} /></Field>
          <div className="field wide" style={{ gridColumn: "1 / -1" }}><label htmlFor="m-notes">Notes</label><input id="m-notes" type="text" {...bind("notes")} /></div>
        </div>
        <div className="form-actions"><button className="btn btn-quiet" disabled={busy}>{busy ? "Saving…" : "Record reading"}</button></div>
      </form>
      {readings.data && readings.data.content.length > 0 && (
        <ul className="muted" style={{ margin: "14px 0 0", paddingLeft: 18, fontSize: 14 }}>
          {readings.data.content.map((r) => (
            <li key={r.id}>{fmtDate(r.readingDate)}: <span className="mono">{fmtHours(r.hours)}</span> {label(r.meterType).toLowerCase()} h{r.recordedBy && ` by ${r.recordedBy}`}</li>
          ))}
        </ul>
      )}
    </div>
  );
}

function TaskForm({ equipmentId, onSaved }) {
  const [values, bind] = useForm({ title: "", description: "", estimatedHours: "" });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (ev) => {
    ev.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.createTask({ equipmentId, title: values.title, description: values.description || null, estimatedHours: values.estimatedHours ? Number(values.estimatedHours) : null });
      onSaved();
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  return (
    <form className="panel" onSubmit={submit} style={{ marginBottom: 14 }}>
      <Alert error={error} />
      <div className="form-grid">
        <Field id="t-title" label="Task title"><input id="t-title" type="text" required {...bind("title")} /></Field>
        <Field id="t-hours" label="Estimated hours"><input id="t-hours" type="number" step="0.1" min="0" {...bind("estimatedHours")} /></Field>
        <div className="field wide"><label htmlFor="t-desc">Instructions</label><textarea id="t-desc" {...bind("description")} /></div>
      </div>
      <div className="form-actions"><button className="btn btn-primary" disabled={busy}>{busy ? "Saving…" : "Save task"}</button></div>
    </form>
  );
}

function ScheduleForm({ tasks, onSaved }) {
  const [values, bind] = useForm({ taskId: tasks[0]?.id ?? "", intervalType: "CALENDAR_DAYS", intervalValue: "", lastDoneDate: "", lastDoneHours: "" });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  const hourBased = values.intervalType !== "CALENDAR_DAYS";

  const submit = async (ev) => {
    ev.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.createSchedule({
        taskId: Number(values.taskId),
        intervalType: values.intervalType,
        intervalValue: Number(values.intervalValue),
        lastDoneDate: !hourBased && values.lastDoneDate ? values.lastDoneDate : null,
        lastDoneHours: hourBased && values.lastDoneHours !== "" ? Number(values.lastDoneHours) : null,
      });
      onSaved();
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  return (
    <form className="panel" onSubmit={submit} style={{ marginBottom: 14 }}>
      <Alert error={error} />
      <div className="form-grid">
        <Field id="s-task" label="Task"><select id="s-task" required {...bind("taskId")}>{tasks.map((t) => <option key={t.id} value={t.id}>{t.title}</option>)}</select></Field>
        <Field id="s-type" label="Repeat by"><select id="s-type" {...bind("intervalType")}>{INTERVAL_TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}</select></Field>
        <Field id="s-int" label={hourBased ? "Every (hours)" : "Every (days)"}><input id="s-int" type="number" min="1" required {...bind("intervalValue")} /></Field>
        {hourBased ? (
          <Field id="s-lh" label="Last done at (hours)" hint="Leave blank to start from the current meter"><input id="s-lh" type="number" step="0.1" min="0" {...bind("lastDoneHours")} /></Field>
        ) : (
          <Field id="s-ld" label="Last done on" hint="Leave blank to start from today"><input id="s-ld" type="date" {...bind("lastDoneDate")} /></Field>
        )}
      </div>
      <div className="form-actions"><button className="btn btn-primary" disabled={busy}>{busy ? "Saving…" : "Save schedule"}</button></div>
    </form>
  );
}
