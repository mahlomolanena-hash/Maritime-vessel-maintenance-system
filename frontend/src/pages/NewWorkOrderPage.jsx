import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { api } from "../api/client";
import { Alert, Field, PageHeader } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useFetch } from "../hooks/useFetch";
import { useForm } from "../hooks/useForm";
import { WO_PRIORITIES, WO_TYPES, label } from "../utils/format";
import { flattenTree } from "../utils/tree";

export default function NewWorkOrderPage() {
  const { isManager } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [values, bind, setValues] = useForm({
    vesselId: params.get("vesselId") || "",
    equipmentId: params.get("equipmentId") || "",
    taskId: "",
    title: "",
    description: "",
    type: "CORRECTIVE",
    priority: "MEDIUM",
    dueDate: "",
    assignedToId: "",
  });
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const vessels = useFetch(() => api.vessels({ size: 100 }));
  const equipment = useFetch(() => (values.vesselId ? api.vesselEquipment(values.vesselId) : Promise.resolve([])), values.vesselId);
  const tasks = useFetch(() => (values.equipmentId ? api.equipmentTasks(values.equipmentId) : Promise.resolve([])), values.equipmentId);
  const users = useFetch(() => (isManager ? api.users() : Promise.resolve([])));

  const equipmentRows = flattenTree(equipment.data || []);

  const pickTask = (e) => {
    const task = tasks.data?.find((t) => String(t.id) === e.target.value);
    setValues((v) => ({
      ...v,
      taskId: e.target.value,
      title: task && !v.title ? task.title : v.title,
      description: task && !v.description ? task.description || "" : v.description,
    }));
  };

  const submit = async (ev) => {
    ev.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const wo = await api.createWorkOrder({
        equipmentId: Number(values.equipmentId),
        taskId: values.taskId ? Number(values.taskId) : null,
        title: values.title,
        description: values.description || null,
        type: values.type,
        priority: values.priority,
        dueDate: values.dueDate || null,
        assignedToId: values.assignedToId ? Number(values.assignedToId) : null,
      });
      navigate(`/work-orders/${wo.id}`);
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  const fe = error?.fieldErrors || {};
  return (
    <>
      <div className="breadcrumb"><Link to="/work-orders">Work orders</Link></div>
      <PageHeader title="New work order" />
      <form className="panel" onSubmit={submit}>
        <Alert error={error} />
        <div className="form-grid">
          <Field id="n-vessel" label="Vessel">
            <select id="n-vessel" required value={values.vesselId} onChange={(e) => setValues((v) => ({ ...v, vesselId: e.target.value, equipmentId: "", taskId: "" }))}>
              <option value="">Choose a vessel</option>
              {vessels.data?.content.map((v) => <option key={v.id} value={v.id}>{v.name}</option>)}
            </select>
          </Field>
          <Field id="n-equipment" label="Equipment" error={fe.equipmentId}>
            <select id="n-equipment" required disabled={!values.vesselId} value={values.equipmentId} onChange={(e) => setValues((v) => ({ ...v, equipmentId: e.target.value, taskId: "" }))}>
              <option value="">Choose equipment</option>
              {equipmentRows.map((e) => <option key={e.id} value={e.id}>{"— ".repeat(e.depth)}{e.name} ({e.tagNumber})</option>)}
            </select>
          </Field>
          <Field id="n-task" label="Planned task (optional)" hint="Fills in the title and instructions">
            <select id="n-task" disabled={!values.equipmentId} value={values.taskId} onChange={pickTask}>
              <option value="">None, ad-hoc job</option>
              {tasks.data?.map((t) => <option key={t.id} value={t.id}>{t.title}</option>)}
            </select>
          </Field>
          <Field id="n-title" label="Title" error={fe.title}><input id="n-title" type="text" required {...bind("title")} /></Field>
          <Field id="n-type" label="Type"><select id="n-type" {...bind("type")}>{WO_TYPES.map((t) => <option key={t} value={t}>{label(t)}</option>)}</select></Field>
          <Field id="n-priority" label="Priority"><select id="n-priority" {...bind("priority")}>{WO_PRIORITIES.map((t) => <option key={t} value={t}>{label(t)}</option>)}</select></Field>
          <Field id="n-due" label="Due date"><input id="n-due" type="date" {...bind("dueDate")} /></Field>
          {isManager && (
            <Field id="n-assign" label="Assign to">
              <select id="n-assign" {...bind("assignedToId")}>
                <option value="">Leave unassigned</option>
                {users.data?.filter((u) => u.enabled).map((u) => <option key={u.id} value={u.id}>{u.firstName} {u.lastName}</option>)}
              </select>
            </Field>
          )}
          <div className="field wide"><label htmlFor="n-desc">Description</label><textarea id="n-desc" {...bind("description")} /></div>
        </div>
        <div className="form-actions">
          <button className="btn btn-primary" disabled={busy}>{busy ? "Creating…" : "Create work order"}</button>
          <Link className="btn btn-quiet" to="/work-orders">Cancel</Link>
        </div>
      </form>
    </>
  );
}
