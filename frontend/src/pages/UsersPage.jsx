import { useState } from "react";
import { api } from "../api/client";
import { Alert, Field, Loading, PageHeader, Tag } from "../components/ui";
import { useAuth } from "../context/AuthContext";
import { useFetch } from "../hooks/useFetch";
import { useForm } from "../hooks/useForm";
import { ROLES, label } from "../utils/format";

export default function UsersPage() {
  const { userId } = useAuth();
  const list = useFetch(() => api.users());
  const [showForm, setShowForm] = useState(false);
  const [error, setError] = useState(null);

  const toggle = async (user) => {
    setError(null);
    try {
      await api.updateUser(user.id, { enabled: !user.enabled });
      list.reload();
    } catch (err) {
      setError(err);
    }
  };

  return (
    <>
      <PageHeader title="Users" subtitle="Accounts are created by an administrator; there is no public sign-up.">
        <button className="btn btn-primary" onClick={() => setShowForm((v) => !v)}>{showForm ? "Close form" : "Add user"}</button>
      </PageHeader>

      {showForm && <UserForm onSaved={() => { setShowForm(false); list.reload(); }} />}
      <Alert error={list.error || error} />
      {list.loading && !list.data && <Loading />}
      {list.data && (
        <div className="table-wrap">
          <table>
            <thead><tr><th>Name</th><th>Email</th><th>Roles</th><th>Status</th><th /></tr></thead>
            <tbody>
              {list.data.map((u) => (
                <tr key={u.id}>
                  <td><strong>{u.firstName} {u.lastName}</strong></td>
                  <td>{u.email}</td>
                  <td>{u.roles.map(label).join(", ")}</td>
                  <td><Tag value={u.enabled ? "ACTIVE" : "CANCELLED"} text={u.enabled ? "Active" : "Disabled"} /></td>
                  <td className="right">
                    {u.id !== userId && (
                      <button className="btn btn-quiet btn-small" onClick={() => toggle(u)}>{u.enabled ? "Disable" : "Enable"}</button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function UserForm({ onSaved }) {
  const [values, bind] = useForm({ email: "", password: "", firstName: "", lastName: "", phone: "" });
  const [roles, setRoles] = useState(["ENGINEER"]);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  const toggleRole = (role) => setRoles((r) => (r.includes(role) ? r.filter((x) => x !== role) : [...r, role]));

  const submit = async (e) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await api.createUser({ ...values, phone: values.phone || null, roles });
      onSaved();
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  const fe = error?.fieldErrors || {};
  return (
    <form className="panel" onSubmit={submit} style={{ marginBottom: 20 }}>
      <h3 style={{ marginBottom: 12 }}>New user</h3>
      <Alert error={error} />
      <div className="form-grid">
        <Field id="u-first" label="First name" error={fe.firstName}><input id="u-first" type="text" required {...bind("firstName")} /></Field>
        <Field id="u-last" label="Last name" error={fe.lastName}><input id="u-last" type="text" required {...bind("lastName")} /></Field>
        <Field id="u-email" label="Email" error={fe.email}><input id="u-email" type="email" required {...bind("email")} /></Field>
        <Field id="u-pass" label="Temporary password" hint="At least 8 characters" error={fe.password}><input id="u-pass" type="password" required minLength={8} {...bind("password")} /></Field>
        <Field id="u-phone" label="Phone"><input id="u-phone" type="text" {...bind("phone")} /></Field>
        <div className="field">
          <label>Roles</label>
          {ROLES.map((r) => (
            <label key={r} className="inline-check"><input type="checkbox" checked={roles.includes(r)} onChange={() => toggleRole(r)} /> {label(r)}</label>
          ))}
          {fe.roles && <span className="field-error">{fe.roles}</span>}
        </div>
      </div>
      <div className="form-actions"><button className="btn btn-primary" disabled={busy || roles.length === 0}>{busy ? "Saving…" : "Create user"}</button></div>
    </form>
  );
}
