import { useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { Alert, Empty, Field, Loading, PageHeader, Pagination, Tag } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useFetch } from "../hooks/useFetch";
import { useFilters } from "../hooks/useFilters";
import { useForm } from "../hooks/useForm";
import { VESSEL_STATUSES, VESSEL_TYPES, label } from "../utils/format";

const EMPTY = { name: "", imoNumber: "", vesselType: "CARGO", flag: "", yearBuilt: "", grossTonnage: "", status: "ACTIVE" };

export default function VesselsPage() {
  const { isManager } = useAuth();
  const filters = useFilters();
  const q = filters.get("q");
  const status = filters.get("status");

  const list = useFetch(() => api.vessels({ q, status, page: filters.page, size: 20 }), filters.key);
  const [showForm, setShowForm] = useState(false);

  return (
    <>
      <PageHeader title="Vessels" subtitle="The fleet, with equipment counts.">
        {isManager && (
          <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>
            {showForm ? "Close form" : "Add vessel"}
          </button>
        )}
      </PageHeader>

      {showForm && <VesselForm onSaved={() => { setShowForm(false); list.reload(); }} />}

      <div className="filters">
        <Field id="q" label="Search name or IMO">
          <input id="q" type="search" value={q} onChange={(e) => filters.set("q", e.target.value)} />
        </Field>
        <Field id="status" label="Status">
          <select id="status" value={status} onChange={(e) => filters.set("status", e.target.value)}>
            <option value="">All</option>
            {VESSEL_STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
          </select>
        </Field>
      </div>

      <Alert error={list.error} />
      {list.loading && !list.data && <Loading />}
      {list.data && (
        <>
          <div className="table-wrap">
            <table>
              <thead>
                <tr><th>Vessel</th><th>IMO</th><th>Type</th><th>Flag</th><th>Built</th><th>Equipment</th><th>Status</th></tr>
              </thead>
              <tbody>
                {list.data.content.map((v) => (
                  <tr key={v.id}>
                    <td><Link to={`/vessels/${v.id}`}><strong>{v.name}</strong></Link></td>
                    <td className="mono">{v.imoNumber}</td>
                    <td>{label(v.vesselType)}</td>
                    <td>{v.flag}</td>
                    <td>{v.yearBuilt}</td>
                    <td>{v.equipmentCount}</td>
                    <td><Tag value={v.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
            {list.data.content.length === 0 && <Empty>No vessels match.</Empty>}
          </div>
          <Pagination page={list.data} onPage={(p) => filters.set("page", String(p))} />
        </>
      )}
    </>
  );
}

function VesselForm({ onSaved }) {
  const [values, bind] = useForm(EMPTY);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.createVessel({
        ...values,
        yearBuilt: values.yearBuilt ? Number(values.yearBuilt) : null,
        grossTonnage: values.grossTonnage ? Number(values.grossTonnage) : null,
      });
      onSaved();
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  const fe = error?.fieldErrors || {};
  return (
    <form className="panel" onSubmit={submit} style={{ marginBottom: 20 }}>
      <h3 style={{ marginBottom: 12 }}>New vessel</h3>
      <Alert error={error} />
      <div className="form-grid">
        <Field id="v-name" label="Name" error={fe.name}><input id="v-name" type="text" required {...bind("name")} /></Field>
        <Field id="v-imo" label="IMO number" hint="7 digits" error={fe.imoNumber}><input id="v-imo" type="text" required inputMode="numeric" maxLength={7} {...bind("imoNumber")} /></Field>
        <Field id="v-type" label="Type"><select id="v-type" {...bind("vesselType")}>{VESSEL_TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}</select></Field>
        <Field id="v-flag" label="Flag"><input id="v-flag" type="text" {...bind("flag")} /></Field>
        <Field id="v-year" label="Year built" error={fe.yearBuilt}><input id="v-year" type="number" {...bind("yearBuilt")} /></Field>
        <Field id="v-gt" label="Gross tonnage"><input id="v-gt" type="number" step="0.01" min="0" {...bind("grossTonnage")} /></Field>
        <Field id="v-status" label="Status"><select id="v-status" {...bind("status")}>{VESSEL_STATUSES.map((t) => <option key={t} value={t}>{label(t)}</option>)}</select></Field>
      </div>
      <div className="form-actions"><button className="btn btn-primary" disabled={busy}>{busy ? "Saving…" : "Save vessel"}</button></div>
    </form>
  );
}
