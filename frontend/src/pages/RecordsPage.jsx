import { Link } from "react-router-dom";
import { api } from "../api/client";
import { Alert, Empty, Field, Loading, PageHeader, Pagination, Tag } from "../components/ui";
import { useFetch } from "../hooks/useFetch";
import { useFilters } from "../hooks/useFilters";
import { WO_TYPES, fmtDateTime, fmtHours, fmtMoney, label } from "../utils/format";

export default function RecordsPage() {
  const filters = useFilters();
  const vesselId = filters.get("vesselId");
  const equipmentId = filters.get("equipmentId");
  const type = filters.get("type");
  const from = filters.get("from");
  const to = filters.get("to");
  const q = filters.get("q");

  const vessels = useFetch(() => api.vessels({ size: 100 }));
  const list = useFetch(() => api.records({ vesselId, equipmentId, type, from, to, q, page: filters.page, size: 20 }), filters.key);

  return (
    <>
      <PageHeader title="Maintenance history" subtitle="Every completed job, with the hour-meter readings at the time of service." />

      <div className="filters">
        <Field id="q" label="Search"><input id="q" type="search" placeholder="Job or work performed" value={q} onChange={(e) => filters.set("q", e.target.value)} /></Field>
        <Field id="vessel" label="Vessel">
          <select id="vessel" value={vesselId} onChange={(e) => filters.set("vesselId", e.target.value)}>
            <option value="">All vessels</option>
            {vessels.data?.content.map((v) => <option key={v.id} value={v.id}>{v.name}</option>)}
          </select>
        </Field>
        <Field id="type" label="Type">
          <select id="type" value={type} onChange={(e) => filters.set("type", e.target.value)}>
            <option value="">All</option>
            {WO_TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}
          </select>
        </Field>
        <Field id="from" label="From"><input id="from" type="date" value={from} onChange={(e) => filters.set("from", e.target.value)} /></Field>
        <Field id="to" label="To"><input id="to" type="date" value={to} onChange={(e) => filters.set("to", e.target.value)} /></Field>
        {equipmentId && <button className="btn btn-quiet" onClick={() => filters.set("equipmentId", "")}>Clear equipment filter</button>}
      </div>

      <Alert error={list.error} />
      {list.loading && !list.data && <Loading />}
      {list.data && (
        <>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Completed</th><th>Job</th><th>Equipment</th><th>Done by</th><th className="right">Running h</th><th className="right">Parts</th><th>Work performed</th></tr></thead>
              <tbody>
                {list.data.content.map((r) => (
                  <tr key={r.id}>
                    <td className="nowrap">{fmtDateTime(r.completedAt)}</td>
                    <td>
                      <Link to={`/work-orders/${r.workOrderId}`}><strong>{r.title}</strong></Link>
                      <span className="sub"><span className="mono">{r.workOrderNumber}</span> · <Tag value={r.type} /></span>
                    </td>
                    <td><Link to={`/equipment/${r.equipmentId}`}>{r.equipmentName}</Link><span className="sub">{r.vesselName}</span></td>
                    <td>{r.performedBy}</td>
                    <td className="right mono">{fmtHours(r.runningHoursAtService)}</td>
                    <td className="right">{fmtMoney(r.partsCost)}</td>
                    <td style={{ maxWidth: 320 }}>{r.workPerformed}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {list.data.content.length === 0 && <Empty>No completed maintenance matches these filters.</Empty>}
          </div>
          <Pagination page={list.data} onPage={(p) => filters.set("page", String(p))} />
        </>
      )}
    </>
  );
}
