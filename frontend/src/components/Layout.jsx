import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { label } from "../utils/format";

const links = [
  { to: "/", label: "Dashboard", end: true },
  { to: "/vessels", label: "Vessels" },
  { to: "/schedules", label: "Planned maintenance" },
  { to: "/work-orders", label: "Work orders" },
  { to: "/records", label: "Maintenance history" },
  { to: "/spare-parts", label: "Spare parts" },
];

export default function Layout() {
  const { auth, logout, isAdmin } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">
          <svg width="26" height="26" viewBox="0 0 32 32" aria-hidden="true">
            <rect width="32" height="32" rx="7" fill="#f2f6f7" />
            <path
              d="M16 6v3m0 0a3 3 0 1 1 0 6 3 3 0 0 1 0-6zm0 6v11m-7-6a7 7 0 0 0 14 0m-14 0h3m11 0h-3"
              fill="none" stroke="#0f4c5c" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"
            />
          </svg>
          <span>Tidewatch</span>
        </div>

        <nav className="nav" aria-label="Main">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end}>
              {l.label}
            </NavLink>
          ))}
          {isAdmin && <NavLink to="/users">Users</NavLink>}
        </nav>

        <div className="sidebar-foot">
          <div className="who">
            <strong>
              {auth.firstName} {auth.lastName}
            </strong>
            <span>{auth.roles.map(label).join(", ")}</span>
          </div>
          <button className="btn btn-ghost" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </aside>

      <main className="content">
        <Outlet />
      </main>
    </div>
  );
}
