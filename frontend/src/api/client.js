const API_BASE = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
const STORAGE_KEY = "maintenance_auth";

export class ApiError extends Error {
  constructor(message, status, fieldErrors) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

/* ------------------------------------------------------------------ auth storage
   Lives here (not in React state) so API calls made by a page's first effect
   already see the token, and so a background refresh can update it. */
function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

let current = load();
const listeners = new Set();

export const authStore = {
  get: () => current,
  set(data) {
    current = data;
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
    } catch {
      // storage unavailable: keep the session in memory only
    }
    listeners.forEach((fn) => fn());
  },
  clear() {
    current = null;
    try {
      localStorage.removeItem(STORAGE_KEY);
    } catch {
      // ignore
    }
    listeners.forEach((fn) => fn());
  },
  subscribe(fn) {
    listeners.add(fn);
    return () => listeners.delete(fn);
  },
};

/* ------------------------------------------------------------------ requests */
async function send(path, { method = "GET", body, token } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  return fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

// One refresh at a time: refresh tokens rotate, so parallel refreshes would revoke each other.
let refreshing = null;
function refreshSession() {
  if (!refreshing) {
    const stored = authStore.get();
    refreshing = (async () => {
      if (!stored?.refreshToken) throw new Error("no refresh token");
      const res = await send("/api/v1/auth/refresh", { method: "POST", body: { refreshToken: stored.refreshToken } });
      if (!res.ok) throw new Error("refresh failed");
      const data = await res.json();
      authStore.set(data);
      return data;
    })().finally(() => {
      refreshing = null;
    });
  }
  return refreshing;
}

async function request(path, options = {}) {
  const isAuthCall = path.startsWith("/api/v1/auth/");
  let res = await send(path, { ...options, token: authStore.get()?.accessToken });

  if (res.status === 401 && !isAuthCall && authStore.get()?.refreshToken) {
    try {
      const fresh = await refreshSession();
      res = await send(path, { ...options, token: fresh.accessToken });
    } catch {
      authStore.clear();
    }
  }

  if (res.status === 204) return null;

  let data = null;
  try {
    data = await res.json();
  } catch {
    // empty body
  }

  if (!res.ok) {
    if (res.status === 401 && !isAuthCall) authStore.clear();
    throw new ApiError(data?.message || `Request failed (${res.status})`, res.status, data?.fieldErrors);
  }
  return data;
}

const qs = (params = {}) => {
  const p = new URLSearchParams();
  Object.entries(params).forEach(([k, v]) => {
    if (v !== undefined && v !== null && v !== "" && v !== false) p.set(k, v);
  });
  const s = p.toString();
  return s ? `?${s}` : "";
};
const post = (path, body) => request(path, { method: "POST", body });
const patch = (path, body) => request(path, { method: "PATCH", body });
const del = (path) => request(path, { method: "DELETE" });

export const api = {
  // Auth
  login: (email, password) => post("/api/v1/auth/login", { email, password }),
  logout: () => post("/api/v1/auth/logout"),

  // Dashboard
  dashboard: () => request("/api/v1/dashboard"),

  // Users
  users: () => request("/api/v1/users"),
  createUser: (payload) => post("/api/v1/users", payload),
  updateUser: (id, payload) => patch(`/api/v1/users/${id}`, payload),

  // Vessels
  vessels: (params) => request(`/api/v1/vessels${qs(params)}`),
  vessel: (id) => request(`/api/v1/vessels/${id}`),
  vesselEquipment: (id) => request(`/api/v1/vessels/${id}/equipment`),
  createVessel: (payload) => post("/api/v1/vessels", payload),

  // Equipment
  equipment: (id) => request(`/api/v1/equipment/${id}`),
  createEquipment: (payload) => post("/api/v1/equipment", payload),
  equipmentTasks: (id) => request(`/api/v1/equipment/${id}/tasks`),
  equipmentSchedules: (id) => request(`/api/v1/equipment/${id}/schedules`),
  equipmentHours: (id, params) => request(`/api/v1/equipment/${id}/hours${qs(params)}`),
  recordHours: (id, payload) => post(`/api/v1/equipment/${id}/hours`, payload),

  // Tasks and schedules
  createTask: (payload) => post("/api/v1/tasks", payload),
  schedules: (params) => request(`/api/v1/schedules${qs(params)}`),
  dueSchedules: (params) => request(`/api/v1/schedules/due${qs(params)}`),
  createSchedule: (payload) => post("/api/v1/schedules", payload),
  generateWorkOrder: (scheduleId, payload) => post(`/api/v1/schedules/${scheduleId}/work-orders`, payload || {}),

  // Work orders
  workOrders: (params) => request(`/api/v1/work-orders${qs(params)}`),
  workOrder: (id) => request(`/api/v1/work-orders/${id}`),
  workOrderHistory: (id) => request(`/api/v1/work-orders/${id}/history`),
  createWorkOrder: (payload) => post("/api/v1/work-orders", payload),
  assignWorkOrder: (id, userId) => post(`/api/v1/work-orders/${id}/assign`, { userId }),
  unassignWorkOrder: (id) => post(`/api/v1/work-orders/${id}/unassign`),
  startWorkOrder: (id) => post(`/api/v1/work-orders/${id}/start`),
  holdWorkOrder: (id, notes) => post(`/api/v1/work-orders/${id}/hold`, { notes }),
  resumeWorkOrder: (id) => post(`/api/v1/work-orders/${id}/resume`),
  completeWorkOrder: (id, payload) => post(`/api/v1/work-orders/${id}/complete`, payload),
  cancelWorkOrder: (id, notes) => post(`/api/v1/work-orders/${id}/cancel`, { notes }),
  addPart: (id, sparePartId, quantity) => post(`/api/v1/work-orders/${id}/parts`, { sparePartId, quantity }),
  removePart: (id, lineId) => del(`/api/v1/work-orders/${id}/parts/${lineId}`),

  // Maintenance history
  records: (params) => request(`/api/v1/maintenance-records${qs(params)}`),

  // Spare parts
  spareParts: (params) => request(`/api/v1/spare-parts${qs(params)}`),
  createSparePart: (payload) => post("/api/v1/spare-parts", payload),
  adjustStock: (id, delta, reason) => patch(`/api/v1/spare-parts/${id}/stock`, { delta, reason }),
};
