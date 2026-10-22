import { useState } from "react";
import { api } from "../api/client";
import { Alert, Empty, Field, Loading, PageHeader, Pagination, Tag } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useFetch } from "../hooks/useFetch";
import { useFilters } from "../hooks/useFilters";
import { useForm } from "../hooks/useForm";
import { fmtMoney } from "../utils/format";

export default function SparePartsPage() {
  const { isManager } = useAuth();
  const filters = useFilters();
  const q = filters.get("q");
  const lowStock = filters.get("lowStock") === "1";
  const list = useFetch(() => api.spareParts({ q, lowStock, page: filters.page, size: 20 }), filters.key);
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState(null);

  const adjust = async (part, delta) => {
    setError(null);
    try {
      await api.adjustStock(part.id, delta, delta > 0 ? "Stock received" : "Stock written off");
      list.reload();
    } catch (err) {
      setError(err);
    }
  };

  return (
    <>
      <PageHeader title="Spare parts" subtitle="Stock on board. Parts at or below their reorder level are flagged.">
        {isManager && <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>{showForm ? "Close form" : "Add part"}</button>}
      </PageHeader>

      {showForm && <PartForm onSaved={() => { setShowForm(false); list.reload(); }} />}

      <div className="filters">
        <Field id="q" label="Search"><input id="q" type="search" placeholder="Name or part number" value={q} onChange={(e) => filters.set("q", e.target.value)} /></Field>
        <label className="inline-check"><input type="checkbox" checked={lowStock} onChange={(e) => filters.set("lowStock", e.target.checked)} /> Low stock only</label>
      </div>

      <Alert error={list.error || error} />
      {list.loading && !list.data && <Loading />}
      {list.data && (
        <>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Part</th><th>Location</th><th className="right">In stock</th><th className="right">Reorder at</th><th className="right">Unit cost</th>{isManager && <th />}</tr></thead>
              <tbody>
                {list.data.content.map((p) => (
                  <tr key={p.id}>
                    <td><strong>{p.name}</strong><span className="sub mono">{p.partNumber}</span></td>
                    <td>{p.storageLocation}</td>
                    <td className="right">
                      {p.quantityInStock} {p.unit}
                      {p.lowStock && <span className="sub"><Tag value="DUE_SOON" text="Low stock" /></span>}
                    </td>
                    <td className="right">{p.reorderLevel}</td>
                    <td className="right">{fmtMoney(p.unitCost)}</td>
                    {isManager && (
                      <td className="right nowrap">
                        <button className="btn btn-quiet btn-small" onClick={() => adjust(p, 1)} aria-label={`Add one ${p.name}`}>+1</button>{" "}
                        <button className="btn btn-quiet btn-small" disabled={p.quantityInStock === 0} onClick={() => adjust(p, -1)} aria-label={`Remove one ${p.name}`}>−1</button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
            {list.data.content.length === 0 && <Empty>No parts match.</Empty>}
          </div>
          <Pagination page={list.data} onPage={(p) => filters.set("page", String(p))} />
        </>
      )}
    </>
  );
}

function PartForm({ onSaved }) {
  const [values, bind] = useForm({ partNumber: "", name: "", description: "", unit: "pcs", quantityInStock: "0", reorderLevel: "0", unitCost: "", storageLocation: "" });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.createSparePart({
        ...values,
        quantityInStock: Number(values.quantityInStock),
        reorderLevel: Number(values.reorderLevel),
        unitCost: Number(values.unitCost),
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
      <h3 style={{ marginBottom: 12 }}>New spare part</h3>
      <Alert error={error} />
      <div className="form-grid">
        <Field id="sp-num" label="Part number" error={fe.partNumber}><input id="sp-num" type="text" required {...bind("partNumber")} /></Field>
        <Field id="sp-name" label="Name" error={fe.name}><input id="sp-name" type="text" required {...bind("name")} /></Field>
        <Field id="sp-unit" label="Unit"><input id="sp-unit" type="text" {...bind("unit")} /></Field>
        <Field id="sp-qty" label="Quantity in stock"><input id="sp-qty" type="number" min="0" required {...bind("quantityInStock")} /></Field>
        <Field id="sp-re" label="Reorder level"><input id="sp-re" type="number" min="0" required {...bind("reorderLevel")} /></Field>
        <Field id="sp-cost" label="Unit cost" error={fe.unitCost}><input id="sp-cost" type="number" step="0.01" min="0" required {...bind("unitCost")} /></Field>
        <Field id="sp-loc" label="Storage location"><input id="sp-loc" type="text" {...bind("storageLocation")} /></Field>
        <div className="field wide"><label htmlFor="sp-desc">Description</label><input id="sp-desc" type="text" {...bind("description")} /></div>
      </div>
      <div className="form-actions"><button className="btn btn-primary" disabled={busy}>{busy ? "Saving…" : "Save part"}</button></div>
    </form>
  );
}
