import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { api } from "../api/client";
import GenerateWorkOrderButton from "../components/GenerateWorkOrderButton";
import { Alert, DueGauge, Empty, Field, Loading, PageHeader, Tag } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useFetch } from "../hooks/useFetch";
import { useForm } from "../hooks/useForm";
import { CRITICALITIES, fmtDate, fmtHours, label } from "../utils/format";
import { flattenTree } from "../utils/tree";

export default function VesselDetailPage() {
  const { id } = useParams();
  const { isManager } = useAuth();
  const vessel = useFetch(() => api.vessel(id), id);
  const equipment = useFetch(() => api.vesselEquipment(id), id);
  const due = useFetch(() => api.dueSchedules({ vesselId: id }), id);
  const records = useFetch(() => api.records({ vesselId: id, size: 5 }), id);
  const [showForm, setShowForm] = useState(false);
  const [actionError, setActionError] = useState(null);

  const rows = useMemo(() => flattenTree(equipment.data || []), [equipment.data]);
  const v = vessel.data;

  if (vessel.loading && !v) return <Loading />;
  if (vessel.error) return <Alert error={vessel.error} />;

  return (
    <>
      <div className="breadcrumb"><Link to="/vessels">Vessels</Link></div>
      <PageHeader title={v.name} subtitle={`IMO ${v.imoNumber}`}>
        {isManager && (
          <button className="btn btn-primary" onClick={() => setShowForm((x) => !x)}>
            {showForm ? "Close form" : "Add equipment"}
          </button>
        )}
      </PageHeader>

      <dl className="panel facts">
        <div><dt>Status</dt><dd><Tag value={v.status} /></dd></div>
        <div><dt>Type</dt><dd>{label(v.vesselType)}</dd></div>
        <div><dt>Flag</dt><dd>{v.flag || "Not set"}</dd></div>
        <div><dt>Year built</dt><dd>{v.yearBuilt || "Not set"}</dd></div>
        <div><dt>Gross tonnage</dt><dd>{v.grossTonnage ? Number(v.grossTonnage).toLocaleString() : "Not set"}</dd></div>
        <div><dt>Equipment registered</dt><dd>{v.equipmentCount}</dd></div>
      </dl>

      {showForm && (
        <EquipmentForm vesselId={Number(id)} options={rows} onSaved={() => { setShowForm(false); equipment.reload(); vessel.reload(); }} />
      )}

      <section className="section">
        <header><h2>Equipment</h2></header>
        <Alert error={equipment.error} />
        <div className="table-wrap">
          <table>
            <thead><tr><th>Equipment</th><th>Tag</th><th>Criticality</th><th>Status</th><th className="right">Running hours</th></tr></thead>
            <tbody>
              {rows.map((e) => (
                <tr key={e.id}>
                  <td>
                    <span className="tree-name" style={{ paddingLeft: e.depth * 22 }}>
                      {e.depth > 0 && <span className="branch" aria-hidden="true">└</span>}
                      <Link to={`/equipment/${e.id}`}>{e.name}</Link>
                    </span>
                    {e.model && <span className="sub" style={{ paddingLeft: e.depth * 22 + (e.depth > 0 ? 16 : 0) }}>{[e.manufacturer, e.model].filter(Boolean).join(" ")}</span>}
                  </td>
                  <td className="mono">{e.tagNumber}</td>
                  <td><Tag value={e.criticality} /></td>
                  <td><Tag value={e.status} /></td>
                  <td className="right mono">{fmtHours(e.runningHours)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {!equipment.loading && rows.length === 0 && <Empty>No equipment registered on this vessel yet.</Empty>}
        </div>
      </section>

      <div className="cols cols-even section">
        <section>
          <header><h2>Maintenance due</h2></header>
          <Alert error={actionError} />
          {due.data && due.data.length === 0 && <div className="panel"><Empty>Nothing due on this vessel.</Empty></div>}
          {due.data && due.data.length > 0 && (
            <div className="watch">
              {due.data.map((sc) => (
                <div className="watch-row" key={sc.id}>
                  <div className="what">
                    <strong>{sc.taskTitle}</strong>
                    <span><Link to={`/equipment/${sc.equipmentId}`}>{sc.equipmentName}</Link></span>
                  </div>
                  <DueGauge schedule={sc} />
                  <GenerateWorkOrderButton schedule={sc} onError={setActionError} />
                </div>
              ))}
            </div>
          )}
        </section>

        <section>
          <header><h2>Recent maintenance</h2><Link to={`/records?vesselId=${id}`}>Full history</Link></header>
          {records.data && records.data.content.length === 0 && <div className="panel"><Empty>No completed work yet.</Empty></div>}
          {records.data && records.data.content.length > 0 && (
            <div className="watch">
              {records.data.content.map((r) => (
                <div className="watch-row" style={{ gridTemplateColumns: "1fr auto" }} key={r.id}>
                  <div className="what">
                    <strong><Link to={`/work-orders/${r.workOrderId}`}>{r.title}</Link></strong>
                    <span>{r.equipmentName} · {r.performedBy || "Unknown"}</span>
                  </div>
                  <span className="muted nowrap">{fmtDate(r.completedAt)}</span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </>
  );
}

function EquipmentForm({ vesselId, options, onSaved }) {
  const [values, bind] = useForm({ name: "", tagNumber: "", parentId: "", category: "", manufacturer: "", model: "", serialNumber: "", location: "", criticality: "MEDIUM" });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.createEquipment({ ...values, vesselId, parentId: values.parentId ? Number(values.parentId) : null });
      onSaved();
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  const fe = error?.fieldErrors || {};
  return (
    <form className="panel" onSubmit={submit} style={{ marginTop: 20 }}>
      <h3 style={{ marginBottom: 12 }}>New equipment</h3>
      <Alert error={error} />
      <div className="form-grid">
        <Field id="e-name" label="Name" error={fe.name}><input id="e-name" type="text" required {...bind("name")} /></Field>
        <Field id="e-tag" label="Tag number" hint="Unique on this vessel" error={fe.tagNumber}><input id="e-tag" type="text" required {...bind("tagNumber")} /></Field>
        <Field id="e-parent" label="Part of">
          <select id="e-parent" {...bind("parentId")}>
            <option value="">Top level</option>
            {options.map((o) => <option key={o.id} value={o.id}>{"— ".repeat(o.depth)}{o.name}</option>)}
          </select>
        </Field>
        <Field id="e-cat" label="Category"><input id="e-cat" type="text" {...bind("category")} /></Field>
        <Field id="e-make" label="Manufacturer"><input id="e-make" type="text" {...bind("manufacturer")} /></Field>
        <Field id="e-model" label="Model"><input id="e-model" type="text" {...bind("model")} /></Field>
        <Field id="e-serial" label="Serial number"><input id="e-serial" type="text" {...bind("serialNumber")} /></Field>
        <Field id="e-loc" label="Location"><input id="e-loc" type="text" {...bind("location")} /></Field>
        <Field id="e-crit" label="Criticality"><select id="e-crit" {...bind("criticality")}>{CRITICALITIES.map((c) => <option key={c} value={c}>{label(c)}</option>)}</select></Field>
      </div>
      <div className="form-actions"><button className="btn btn-primary" disabled={busy}>{busy ? "Saving…" : "Save equipment"}</button></div>
    </form>
  );
}
