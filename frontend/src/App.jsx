import { Route, Routes } from "react-router-dom";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";

import LoginPage from "./pages/LoginPage";
import DashboardPage from "./pages/DashboardPage";
import VesselsPage from "./pages/VesselsPage";
import VesselDetailPage from "./pages/VesselDetailPage";
import EquipmentDetailPage from "./pages/EquipmentDetailPage";
import SchedulesPage from "./pages/SchedulesPage";
import WorkOrdersPage from "./pages/WorkOrdersPage";
import NewWorkOrderPage from "./pages/NewWorkOrderPage";
import WorkOrderDetailPage from "./pages/WorkOrderDetailPage";
import RecordsPage from "./pages/RecordsPage";
import SparePartsPage from "./pages/SparePartsPage";
import UsersPage from "./pages/UsersPage";

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<DashboardPage />} />
        <Route path="/vessels" element={<VesselsPage />} />
        <Route path="/vessels/:id" element={<VesselDetailPage />} />
        <Route path="/equipment/:id" element={<EquipmentDetailPage />} />
        <Route path="/schedules" element={<SchedulesPage />} />
        <Route path="/work-orders" element={<WorkOrdersPage />} />
        <Route path="/work-orders/new" element={<NewWorkOrderPage />} />
        <Route path="/work-orders/:id" element={<WorkOrderDetailPage />} />
        <Route path="/records" element={<RecordsPage />} />
        <Route path="/spare-parts" element={<SparePartsPage />} />
        <Route
          path="/users"
          element={
            <ProtectedRoute adminOnly>
              <UsersPage />
            </ProtectedRoute>
          }
        />
      </Route>
    </Routes>
  );
}
