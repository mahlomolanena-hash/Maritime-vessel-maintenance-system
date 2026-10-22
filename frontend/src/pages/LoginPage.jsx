import { useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { Alert, Field } from "../components/ui";
import { useAuth } from "../context/AuthContext";

export default function LoginPage() {
  const { auth, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const from = location.state?.from?.pathname || "/";

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  if (auth) return <Navigate to={from} replace />;

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      await login(email, password);
      navigate(from, { replace: true });
    } catch (err) {
      setError(err);
      setBusy(false);
    }
  };

  return (
    <div className="login-wrap">
      <section className="login-art">
        <h1>Tidewatch</h1>
        <p>Planned maintenance, work orders and service history for every vessel in the fleet.</p>
      </section>
      <section className="login-form">
        <div className="panel">
          <h2 style={{ marginBottom: 16 }}>Sign in</h2>
          <Alert error={error} />
          <form onSubmit={handleSubmit}>
            <div className="form-grid" style={{ gridTemplateColumns: "1fr" }}>
              <Field id="email" label="Email">
                <input id="email" type="email" required autoComplete="username" value={email} onChange={(e) => setEmail(e.target.value)} />
              </Field>
              <Field id="password" label="Password">
                <input id="password" type="password" required autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} />
              </Field>
            </div>
            <div className="form-actions">
              <button className="btn btn-primary" style={{ width: "100%" }} disabled={busy}>
                {busy ? "Signing in…" : "Sign in"}
              </button>
            </div>
          </form>
          <div className="demo">
            <span>Demo accounts (dev data)</span>
            <span className="mono">admin@example.com / Admin123!</span>
            <span className="mono">superintendent@example.com / Super123!</span>
            <span className="mono">engineer@example.com / Engineer123!</span>
          </div>
        </div>
      </section>
    </div>
  );
}
