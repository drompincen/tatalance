// ============================================================
// Chauffeur Platform Demo - Main Application
// ============================================================

let currentLang = "en";
let currentView = "dashboard";

function t(path) {
  const keys = path.split(".");
  let obj = i18n[currentLang];
  for (const k of keys) {
    obj = obj?.[k];
  }
  return obj || path;
}

// ============================================================
// Navigation & Routing
// ============================================================

function navigate(view) {
  currentView = view;
  document.querySelectorAll(".sidebar-nav a").forEach((a) => a.classList.remove("active"));
  const activeLink = document.querySelector(`[data-view="${view}"]`);
  if (activeLink) activeLink.classList.add("active");
  renderView(view);
  closeSidebar();
}

function renderView(view) {
  const area = document.getElementById("content-area");
  const breadcrumb = document.getElementById("breadcrumb");
  switch (view) {
    case "dashboard": renderDashboard(area); breadcrumb.innerHTML = `<strong>${t("nav.dashboard")}</strong>`; break;
    case "clients": renderClients(area); breadcrumb.innerHTML = `<strong>${t("nav.clients")}</strong>`; break;
    case "drivers": renderDrivers(area); breadcrumb.innerHTML = `<strong>${t("nav.drivers")}</strong>`; break;
    case "rides": renderRides(area); breadcrumb.innerHTML = `<strong>${t("nav.rides")}</strong>`; break;
    case "invoices": renderInvoices(area); breadcrumb.innerHTML = `<strong>${t("nav.invoices")}</strong>`; break;
    case "payouts": renderPayouts(area); breadcrumb.innerHTML = `<strong>${t("nav.payouts")}</strong>`; break;
    case "reports": renderReports(area); breadcrumb.innerHTML = `<strong>${t("nav.reports")}</strong>`; break;
    default: renderDashboard(area);
  }
}

// ============================================================
// Language Toggle
// ============================================================

function toggleLang() {
  currentLang = currentLang === "en" ? "es" : "en";

  // Update toggle switch visual state
  const toggle = document.getElementById("langToggle");
  toggle.classList.toggle("es", currentLang === "es");
  toggle.querySelectorAll(".lang-option").forEach((opt) => {
    opt.classList.toggle("active", opt.dataset.lang === currentLang);
  });

  // Update html lang attribute
  document.getElementById("htmlRoot").lang = currentLang;

  updateSidebar();
  renderView(currentView);
}

function updateSidebar() {
  document.getElementById("appTitle").textContent = t("appTitle");
  document.getElementById("appSubtitle").textContent = t("appSubtitle");

  // Translate main nav labels
  document.querySelectorAll(".sidebar-nav a[data-view]").forEach((a) => {
    const view = a.dataset.view;
    const label = a.querySelector(".nav-label");
    if (label && t(`nav.${view}`)) label.textContent = t(`nav.${view}`);
  });

  // Translate section headers and journey links via data-i18n
  document.querySelectorAll("[data-i18n]").forEach((el) => {
    const key = el.dataset.i18n;
    const val = t(key);
    if (val && val !== key) el.textContent = val;
  });
}

// ============================================================
// Mobile sidebar
// ============================================================

function toggleSidebar() {
  document.querySelector(".sidebar").classList.toggle("open");
  document.querySelector(".sidebar-overlay").classList.toggle("show");
}
function closeSidebar() {
  document.querySelector(".sidebar").classList.remove("open");
  document.querySelector(".sidebar-overlay").classList.remove("show");
}

// ============================================================
// Toast notifications
// ============================================================

function showAlert(containerId, message, type = "success") {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.className = `alert alert-${type} show`;
  el.innerHTML = `<span>${type === "success" ? "&#10003;" : "&#9432;"}</span> ${message}`;
  setTimeout(() => el.classList.remove("show"), 4000);
}

// ============================================================
// Helpers
// ============================================================

function statusBadge(status) {
  const map = {
    scheduled: "info", pendingAssignment: "warning", pending: "warning",
    assigned: "info", accepted: "info",
    inProgress: "warning", in_progress: "warning",
    completed: "success", paid: "success",
    cancelled: "danger", outstanding: "danger", overdue: "danger",
    partiallyPaid: "warning", partially_paid: "warning",
    draft: "neutral", sent: "info",
    available: "success", on_trip: "warning", off_duty: "neutral",
  };
  const badge = map[status] || "neutral";
  return `<span class="badge badge-${badge}">${status.replace(/([A-Z])/g, " $1").trim()}</span>`;
}

function formatCurrency(val) {
  return "$" + Number(val).toFixed(2);
}

function formatDate(dateStr) {
  if (!dateStr) return "—";
  const d = new Date(dateStr);
  return d.toLocaleDateString(currentLang === "es" ? "es-ES" : "en-US", {
    year: "numeric", month: "short", day: "numeric",
  });
}

function formatDateTime(dateStr) {
  if (!dateStr) return "—";
  const d = new Date(dateStr);
  return d.toLocaleString(currentLang === "es" ? "es-ES" : "en-US", {
    month: "short", day: "numeric", hour: "2-digit", minute: "2-digit",
  });
}

// ============================================================
// DASHBOARD (Journey 11 lite)
// ============================================================

function renderDashboard(area) {
  const totalRev = sampleData.monthlyRevenue.reduce((s, m) => s + m.revenue, 0);
  const totalPay = sampleData.monthlyRevenue.reduce((s, m) => s + m.payouts, 0);
  const totalRides = sampleData.monthlyRevenue.reduce((s, m) => s + m.rides, 0);
  const outstanding = sampleData.invoices.filter((i) => i.status !== "paid").reduce((s, i) => s + i.total, 0);

  area.innerHTML = `
    <div class="journey-indicator">
      <span class="ji-num">11</span>
      ${t("journeys.j11")}
    </div>
    <div id="dashAlert"></div>
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">${t("reports.totalRevenue")}</div>
        <div class="stat-value">${formatCurrency(totalRev)}</div>
        <div class="stat-detail">${totalRides} ${t("reports.ridesCompleted").toLowerCase()}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">${t("reports.totalPayouts")}</div>
        <div class="stat-value">${formatCurrency(totalPay)}</div>
        <div class="stat-detail">${sampleData.drivers.length} ${t("nav.drivers").toLowerCase()}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">${t("reports.netMargin")}</div>
        <div class="stat-value">${formatCurrency(totalRev - totalPay)}</div>
        <div class="stat-detail">${((totalRev - totalPay) / totalRev * 100).toFixed(1)}%</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">${t("reports.outstandingInvoices")}</div>
        <div class="stat-value">${formatCurrency(outstanding)}</div>
        <div class="stat-detail">${sampleData.invoices.filter((i) => i.status !== "paid").length} invoices</div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2>${t("reports.revenueByMonth")}</h2>
      </div>
      <div class="card-body">
        <div class="chart-container">
          <div class="bar-chart" id="revenueChart"></div>
          <div class="chart-legend">
            <span><span class="legend-dot" style="background:var(--primary)"></span> ${t("reports.totalRevenue")}</span>
            <span><span class="legend-dot" style="background:var(--accent)"></span> ${t("reports.totalPayouts")}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="card mt-2">
      <div class="card-header">
        <h2>${currentLang === "en" ? "Upcoming Rides" : "Proximos Viajes"}</h2>
      </div>
      <div class="card-body">
        <div class="table-wrapper">
          <table>
            <thead><tr>
              <th>${t("common.date")}</th>
              <th>${t("invoice.client")}</th>
              <th>${t("ride.pickupLocation")}</th>
              <th>${t("ride.dropoffLocation")}</th>
              <th>${t("assign.assignedTo")}</th>
              <th>${t("ride.status")}</th>
            </tr></thead>
            <tbody>
              ${sampleData.rides.filter((r) => r.status !== "completed" && r.status !== "cancelled")
                .map((r) => `<tr onclick="navigate('rides')">
                  <td>${formatDateTime(r.pickupDate)}</td>
                  <td>${getClientName(r.clientId)}</td>
                  <td>${r.pickupLocation}</td>
                  <td>${r.dropoffLocation}</td>
                  <td>${getDriverName(r.assignedDriverId)}</td>
                  <td>${statusBadge(r.status)}</td>
                </tr>`).join("")}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  `;

  // Render bar chart
  const maxRev = Math.max(...sampleData.monthlyRevenue.map((m) => m.revenue));
  const chart = document.getElementById("revenueChart");
  chart.innerHTML = sampleData.monthlyRevenue.map((m) => `
    <div class="bar-group">
      <div class="bar-value">${formatCurrency(m.revenue)}</div>
      <div style="display:flex;gap:3px;align-items:flex-end;height:100%;width:100%;justify-content:center;">
        <div class="bar bar-revenue" style="height:${(m.revenue / maxRev) * 100}%;width:45%"></div>
        <div class="bar bar-payout" style="height:${(m.payouts / maxRev) * 100}%;width:45%"></div>
      </div>
      <div class="bar-label">${m.month.split(" ")[0]}</div>
    </div>
  `).join("");
}

// ============================================================
// CLIENTS (Journeys 1, 10)
// ============================================================

function renderClients(area) {
  area.innerHTML = `
    <div class="journey-indicator">
      <span class="ji-num">1</span>
      ${t("journeys.j1")}
    </div>
    <div id="clientAlert"></div>

    <div class="card">
      <div class="card-header">
        <h2>${t("client.listTitle")}</h2>
        <button class="btn btn-primary" onclick="showClientForm()">${t("client.title")}</button>
      </div>
      <div class="card-body">
        <div class="search-bar">
          <input type="text" id="clientSearch" placeholder="${t("client.search")}" oninput="filterClients()">
        </div>
        <div class="table-wrapper">
          <table>
            <thead><tr>
              <th>${t("client.fullName")}</th>
              <th>${t("client.phone")}</th>
              <th>${t("client.email")}</th>
              <th>${t("client.vehicleType")}</th>
              <th>${t("client.preferredDriver")}</th>
              <th>${t("common.actions")}</th>
            </tr></thead>
            <tbody id="clientTableBody"></tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Client Form Modal -->
    <div class="modal-overlay" id="clientModal">
      <div class="modal">
        <div class="modal-header">
          <h3>${t("client.title")}</h3>
          <button class="modal-close" onclick="closeModal('clientModal')">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group">
              <label>${t("client.fullName")} *</label>
              <input type="text" id="clientName" required>
            </div>
            <div class="form-group">
              <label>${t("client.phone")} *</label>
              <input type="tel" id="clientPhone">
            </div>
            <div class="form-group">
              <label>${t("client.email")}</label>
              <input type="email" id="clientEmail">
            </div>
            <div class="form-group">
              <label>${t("client.billingAddress")}</label>
              <input type="text" id="clientAddress">
            </div>
            <div class="form-group full-width">
              <label>${t("client.notes")}</label>
              <textarea id="clientNotes" rows="2"></textarea>
            </div>
          </div>

          <div class="section-title">${t("client.preferences")}</div>
          <div class="form-grid">
            <div class="form-group">
              <label>${t("client.vehicleType")}</label>
              <select id="clientVehicle">
                <option value="">${t("client.none")}</option>
                <option value="sedan">${t("client.sedan")}</option>
                <option value="suv">${t("client.suv")}</option>
                <option value="van">${t("client.van")}</option>
                <option value="luxury">${t("client.luxury")}</option>
              </select>
            </div>
            <div class="form-group">
              <label>${t("client.language")}</label>
              <select id="clientLang">
                <option value="English">English</option>
                <option value="Spanish">Spanish</option>
              </select>
            </div>
            <div class="form-group full-width">
              <label>${t("client.frequentLocations")}</label>
              <input type="text" id="clientLocations">
            </div>
            <div class="form-group full-width">
              <label>${t("client.vipInstructions")}</label>
              <textarea id="clientVip" rows="2"></textarea>
            </div>
            <div class="form-group">
              <label>${t("client.preferredDriver")}</label>
              <select id="clientPrefDriver">
                <option value="">${t("client.none")}</option>
                ${sampleData.drivers.map((d) => `<option value="${d.id}">${d.name}</option>`).join("")}
              </select>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" onclick="closeModal('clientModal')">${t("common.cancel")}</button>
          <button class="btn btn-primary" onclick="saveClient()">${t("client.save")}</button>
        </div>
      </div>
    </div>

    <!-- Client Detail Modal (Journey 10) -->
    <div class="modal-overlay" id="clientDetailModal">
      <div class="modal" style="max-width:700px">
        <div class="modal-header">
          <h3 id="clientDetailTitle"></h3>
          <button class="modal-close" onclick="closeModal('clientDetailModal')">&times;</button>
        </div>
        <div class="modal-body" id="clientDetailBody"></div>
      </div>
    </div>
  `;
  renderClientTable();
}

function renderClientTable(filter = "") {
  const tbody = document.getElementById("clientTableBody");
  if (!tbody) return;
  const clients = sampleData.clients.filter((c) => {
    if (!filter) return true;
    const f = filter.toLowerCase();
    return c.fullName.toLowerCase().includes(f) || c.phone.includes(f) || c.email.toLowerCase().includes(f);
  });
  tbody.innerHTML = clients.map((c) => `
    <tr onclick="showClientDetail(${c.id})">
      <td><strong>${c.fullName}</strong></td>
      <td>${c.phone}</td>
      <td>${c.email}</td>
      <td>${c.vehicleType || "—"}</td>
      <td>${getDriverName(c.preferredDriverId)}</td>
      <td>
        <button class="btn btn-sm btn-outline" onclick="event.stopPropagation(); showClientDetail(${c.id})">${t("client.editBtn")}</button>
      </td>
    </tr>
  `).join("");
}

function filterClients() {
  const val = document.getElementById("clientSearch").value;
  renderClientTable(val);
}

function showClientForm() {
  document.getElementById("clientModal").classList.add("show");
}

function closeModal(id) {
  document.getElementById(id).classList.remove("show");
}

function saveClient() {
  const name = document.getElementById("clientName").value;
  if (!name) return;
  sampleData.clients.push({
    id: nextClientId(),
    fullName: name,
    phone: document.getElementById("clientPhone").value,
    email: document.getElementById("clientEmail").value,
    billingAddress: document.getElementById("clientAddress").value,
    notes: document.getElementById("clientNotes").value,
    vehicleType: document.getElementById("clientVehicle").value,
    preferredLanguage: document.getElementById("clientLang").value,
    frequentLocations: document.getElementById("clientLocations").value,
    vipInstructions: document.getElementById("clientVip").value,
    preferredDriverId: document.getElementById("clientPrefDriver").value ? Number(document.getElementById("clientPrefDriver").value) : null,
    createdAt: new Date().toISOString().split("T")[0],
  });
  closeModal("clientModal");
  renderClientTable();
  showAlert("clientAlert", t("client.successMsg"));
}

function showClientDetail(clientId) {
  const c = sampleData.clients.find((x) => x.id === clientId);
  if (!c) return;
  const rides = sampleData.rides.filter((r) => r.clientId === clientId);
  document.getElementById("clientDetailTitle").textContent = c.fullName;
  document.getElementById("clientDetailBody").innerHTML = `
    <div class="journey-indicator">
      <span class="ji-num">10</span>
      ${t("journeys.j10")}
    </div>

    <div class="detail-grid">
      <div class="detail-item"><div class="detail-label">${t("client.phone")}</div><div class="detail-value">${c.phone}</div></div>
      <div class="detail-item"><div class="detail-label">${t("client.email")}</div><div class="detail-value">${c.email}</div></div>
      <div class="detail-item full-width"><div class="detail-label">${t("client.billingAddress")}</div><div class="detail-value">${c.billingAddress || "—"}</div></div>
      <div class="detail-item full-width"><div class="detail-label">${t("client.notes")}</div><div class="detail-value">${c.notes || "—"}</div></div>
    </div>

    <div class="section-title">${t("vip.preferences")}</div>
    <div class="detail-grid">
      <div class="detail-item"><div class="detail-label">${t("client.vehicleType")}</div><div class="detail-value">${c.vehicleType || "—"}</div></div>
      <div class="detail-item"><div class="detail-label">${t("client.language")}</div><div class="detail-value">${c.preferredLanguage || "—"}</div></div>
      <div class="detail-item"><div class="detail-label">${t("client.preferredDriver")}</div><div class="detail-value">${getDriverName(c.preferredDriverId)}</div></div>
      <div class="detail-item full-width"><div class="detail-label">${t("client.frequentLocations")}</div><div class="detail-value">${c.frequentLocations || "—"}</div></div>
      <div class="detail-item full-width"><div class="detail-label">${t("client.vipInstructions")}</div><div class="detail-value">${c.vipInstructions || "—"}</div></div>
    </div>

    <div class="section-title">${t("vip.previousRides")} (${rides.length})</div>
    ${rides.length ? `<div class="table-wrapper"><table>
      <thead><tr><th>${t("common.date")}</th><th>${t("ride.dropoffLocation")}</th><th>${t("assign.assignedTo")}</th><th>${t("ride.status")}</th></tr></thead>
      <tbody>${rides.map((r) => `<tr>
        <td>${formatDateTime(r.pickupDate)}</td>
        <td>${r.dropoffLocation}</td>
        <td>${getDriverName(r.assignedDriverId)}</td>
        <td>${statusBadge(r.status)}</td>
      </tr>`).join("")}</tbody>
    </table></div>` : `<p class="empty-state"><span class="empty-icon">&#128663;</span><br>No rides yet</p>`}

    <div class="btn-group">
      <button class="btn btn-primary" onclick="closeModal('clientDetailModal'); navigate('rides')">${t("vip.createNewRide")}</button>
    </div>
  `;
  document.getElementById("clientDetailModal").classList.add("show");
}

// ============================================================
// DRIVERS (Journey 2)
// ============================================================

function renderDrivers(area) {
  area.innerHTML = `
    <div class="journey-indicator">
      <span class="ji-num">2</span>
      ${t("journeys.j2")}
    </div>
    <div id="driverAlert"></div>

    <div class="card">
      <div class="card-header">
        <h2>${t("driver.listTitle")}</h2>
        <button class="btn btn-primary" onclick="showDriverForm()">${t("driver.createTitle")}</button>
      </div>
      <div class="card-body">
        <div class="table-wrapper">
          <table>
            <thead><tr>
              <th>${t("driver.name")}</th>
              <th>${t("driver.phone")}</th>
              <th>${t("driver.vehicle")}</th>
              <th>${t("driver.workZones")}</th>
              <th>${t("driver.languages")}</th>
              <th>${t("driver.status")}</th>
            </tr></thead>
            <tbody id="driverTableBody"></tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Driver Form Modal -->
    <div class="modal-overlay" id="driverModal">
      <div class="modal">
        <div class="modal-header">
          <h3>${t("driver.createTitle")}</h3>
          <button class="modal-close" onclick="closeModal('driverModal')">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group">
              <label>${t("driver.name")} *</label>
              <input type="text" id="driverName">
            </div>
            <div class="form-group">
              <label>${t("driver.phone")}</label>
              <input type="tel" id="driverPhone">
            </div>
            <div class="form-group">
              <label>${t("driver.email")}</label>
              <input type="email" id="driverEmail">
            </div>
            <div class="form-group">
              <label>${t("driver.availability")}</label>
              <select id="driverAvail">
                <option value="available">${t("driver.available")}</option>
                <option value="on_trip">${t("driver.onTrip")}</option>
                <option value="off_duty">${t("driver.offDuty")}</option>
              </select>
            </div>
            <div class="form-group full-width">
              <label>${t("driver.licenseNotes")}</label>
              <input type="text" id="driverLicense">
            </div>
            <div class="form-group">
              <label>${t("driver.vehicle")}</label>
              <input type="text" id="driverVehicle">
            </div>
            <div class="form-group">
              <label>${t("driver.workZones")}</label>
              <input type="text" id="driverZones">
            </div>
            <div class="form-group">
              <label>${t("driver.languages")}</label>
              <input type="text" id="driverLangs">
            </div>
            <div class="form-group">
              <label>${t("driver.payoutTerms")}</label>
              <input type="text" id="driverPayout">
            </div>
            <div class="form-group full-width">
              <label>${t("driver.internalNotes")}</label>
              <textarea id="driverNotes" rows="2"></textarea>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" onclick="closeModal('driverModal')">${t("common.cancel")}</button>
          <button class="btn btn-primary" onclick="saveDriver()">${t("driver.save")}</button>
        </div>
      </div>
    </div>
  `;
  renderDriverTable();
}

function renderDriverTable() {
  const tbody = document.getElementById("driverTableBody");
  if (!tbody) return;
  tbody.innerHTML = sampleData.drivers.map((d) => `
    <tr>
      <td><strong>${d.name}</strong></td>
      <td>${d.phone}</td>
      <td>${d.vehicle || "—"}</td>
      <td>${d.workZones || "—"}</td>
      <td>${d.languages || "—"}</td>
      <td><span class="availability-dot ${d.availability}"></span>${t(`driver.${d.availability === "on_trip" ? "onTrip" : d.availability === "off_duty" ? "offDuty" : "available"}`)}</td>
    </tr>
  `).join("");
}

function showDriverForm() {
  document.getElementById("driverModal").classList.add("show");
}

function saveDriver() {
  const name = document.getElementById("driverName").value;
  if (!name) return;
  sampleData.drivers.push({
    id: nextDriverId(),
    name,
    phone: document.getElementById("driverPhone").value,
    email: document.getElementById("driverEmail").value,
    licenseNotes: document.getElementById("driverLicense").value,
    availability: document.getElementById("driverAvail").value,
    internalNotes: document.getElementById("driverNotes").value,
    vehicle: document.getElementById("driverVehicle").value,
    workZones: document.getElementById("driverZones").value,
    languages: document.getElementById("driverLangs").value,
    payoutTerms: document.getElementById("driverPayout").value,
    payoutType: "percentage",
    payoutRate: 70,
    active: true,
  });
  closeModal("driverModal");
  renderDriverTable();
  showAlert("driverAlert", t("driver.successMsg"));
}

// ============================================================
// RIDES (Journeys 3, 4, 5, 6)
// ============================================================

function renderRides(area) {
  area.innerHTML = `
    <div class="journey-indicator">
      <span class="ji-num">3</span>
      ${t("journeys.j3")}
    </div>
    <div id="rideAlert"></div>

    <div class="workflow-steps">
      <div class="workflow-step done">${currentLang === "en" ? "Book" : "Reservar"}</div>
      <div class="workflow-step">${currentLang === "en" ? "Assign" : "Asignar"}</div>
      <div class="workflow-step">${currentLang === "en" ? "Accept" : "Aceptar"}</div>
      <div class="workflow-step">${currentLang === "en" ? "Complete" : "Completar"}</div>
      <div class="workflow-step">${currentLang === "en" ? "Invoice" : "Facturar"}</div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2>${t("ride.listTitle")}</h2>
        <button class="btn btn-primary" onclick="showRideForm()">${t("ride.title")}</button>
      </div>
      <div class="card-body">
        <div class="table-wrapper">
          <table>
            <thead><tr>
              <th>ID</th>
              <th>${t("common.date")}</th>
              <th>${t("invoice.client")}</th>
              <th>${t("ride.pickupLocation")}</th>
              <th>${t("ride.dropoffLocation")}</th>
              <th>${t("assign.assignedTo")}</th>
              <th>${t("ride.status")}</th>
              <th>${t("common.actions")}</th>
            </tr></thead>
            <tbody id="rideTableBody"></tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Ride Form Modal -->
    <div class="modal-overlay" id="rideModal">
      <div class="modal" style="max-width:650px">
        <div class="modal-header">
          <h3>${t("ride.title")}</h3>
          <button class="modal-close" onclick="closeModal('rideModal')">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group">
              <label>${t("ride.selectClient")} *</label>
              <select id="rideClient">
                <option value="">—</option>
                ${sampleData.clients.map((c) => `<option value="${c.id}">${c.fullName}</option>`).join("")}
              </select>
            </div>
            <div class="form-group">
              <label>${t("ride.pickupDate")} *</label>
              <input type="datetime-local" id="ridePickupDate">
            </div>
            <div class="form-group">
              <label>${t("ride.pickupLocation")} *</label>
              <input type="text" id="ridePickup">
            </div>
            <div class="form-group">
              <label>${t("ride.dropoffLocation")} *</label>
              <input type="text" id="rideDropoff">
            </div>
            <div class="form-group">
              <label>${t("ride.estimatedDuration")}</label>
              <input type="text" id="rideDuration" placeholder="e.g. 45 min">
            </div>
            <div class="form-group">
              <label>${t("ride.serviceType")}</label>
              <select id="rideService">
                <option value="pointToPoint">${t("ride.pointToPoint")}</option>
                <option value="hourly">${t("ride.hourly")}</option>
                <option value="airport">${t("ride.airport")}</option>
              </select>
            </div>
            <div class="form-group">
              <label>${t("ride.pricingModel")}</label>
              <select id="ridePricing">
                <option value="flatRate">${t("ride.flatRate")}</option>
                <option value="perHour">${t("ride.perHour")}</option>
                <option value="perMile">${t("ride.perMile")}</option>
              </select>
            </div>
            <div class="form-group">
              <label>${currentLang === "en" ? "Base Price ($)" : "Precio Base ($)"}</label>
              <input type="number" id="ridePrice" value="0" min="0" step="0.01">
            </div>
          </div>

          <div class="section-title">${t("ride.rideNotes")}</div>
          <div class="form-grid">
            <div class="form-group">
              <label class="checkbox-label"><input type="checkbox" id="rideAirport"> ${t("ride.airportPickup")}</label>
            </div>
            <div class="form-group">
              <label>${t("ride.flightNumber")}</label>
              <input type="text" id="rideFlight">
            </div>
            <div class="form-group full-width">
              <label>${t("ride.waitingInstructions")}</label>
              <textarea id="rideWaiting" rows="2"></textarea>
            </div>
            <div class="form-group full-width">
              <label>${t("ride.vipNotes")}</label>
              <textarea id="rideVip" rows="2"></textarea>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" onclick="closeModal('rideModal')">${t("common.cancel")}</button>
          <button class="btn btn-primary" onclick="saveRide()">${t("ride.save")}</button>
        </div>
      </div>
    </div>

    <!-- Ride Detail Modal (Journeys 4, 5, 6) -->
    <div class="modal-overlay" id="rideDetailModal">
      <div class="modal" style="max-width:700px">
        <div class="modal-header">
          <h3 id="rideDetailTitle"></h3>
          <button class="modal-close" onclick="closeModal('rideDetailModal')">&times;</button>
        </div>
        <div class="modal-body" id="rideDetailBody"></div>
      </div>
    </div>
  `;
  renderRideTable();
}

function renderRideTable() {
  const tbody = document.getElementById("rideTableBody");
  if (!tbody) return;
  tbody.innerHTML = sampleData.rides.map((r) => `
    <tr onclick="showRideDetail(${r.id})">
      <td>#${r.id}</td>
      <td>${formatDateTime(r.pickupDate)}</td>
      <td>${getClientName(r.clientId)}</td>
      <td>${r.pickupLocation}</td>
      <td>${r.dropoffLocation}</td>
      <td>${getDriverName(r.assignedDriverId)}</td>
      <td>${statusBadge(r.status)}</td>
      <td>
        ${r.status === "scheduled" || r.status === "pendingAssignment" ? `<button class="btn btn-sm btn-warning" onclick="event.stopPropagation(); showRideDetail(${r.id})">${t("assign.assignBtn")}</button>` : ""}
        ${r.status === "assigned" ? `<button class="btn btn-sm btn-success" onclick="event.stopPropagation(); acceptRide(${r.id})">${t("accept.acceptBtn")}</button>` : ""}
        ${r.status === "accepted" ? `<button class="btn btn-sm btn-primary" onclick="event.stopPropagation(); showCompleteRide(${r.id})">${t("complete.completeBtn")}</button>` : ""}
        ${r.status === "completed" ? `<button class="btn btn-sm btn-outline" onclick="event.stopPropagation(); generateInvoiceFromRide(${r.id})">${t("invoice.generateBtn")}</button>` : ""}
      </td>
    </tr>
  `).join("");
}

function showRideForm() {
  document.getElementById("rideModal").classList.add("show");
}

function saveRide() {
  const clientId = document.getElementById("rideClient").value;
  if (!clientId) return;
  sampleData.rides.push({
    id: nextRideId(),
    clientId: Number(clientId),
    pickupDate: document.getElementById("ridePickupDate").value,
    pickupLocation: document.getElementById("ridePickup").value,
    dropoffLocation: document.getElementById("rideDropoff").value,
    estimatedDuration: document.getElementById("rideDuration").value,
    serviceType: document.getElementById("rideService").value,
    pricingModel: document.getElementById("ridePricing").value,
    basePrice: Number(document.getElementById("ridePrice").value),
    notes: "",
    flightNumber: document.getElementById("rideFlight").value,
    isAirportPickup: document.getElementById("rideAirport").checked,
    waitingInstructions: document.getElementById("rideWaiting").value,
    vipNotes: document.getElementById("rideVip").value,
    status: "scheduled",
    assignedDriverId: null,
    assignedAt: null,
    acceptedAt: null,
    completedByDriverId: null,
    assignmentStatus: "pending",
    actualStart: null, actualEnd: null, actualDuration: null,
    waitingTime: 0, tolls: 0, parking: 0,
    additionalCharges: 0, chargeDescription: "",
    billableAmount: null,
  });
  closeModal("rideModal");
  renderRideTable();
  showAlert("rideAlert", t("ride.successMsg"));
}

function showRideDetail(rideId) {
  const r = sampleData.rides.find((x) => x.id === rideId);
  if (!r) return;
  const client = sampleData.clients.find((c) => c.id === r.clientId);

  let actionButtons = "";
  if (r.status === "scheduled" || r.status === "pendingAssignment") {
    actionButtons = `
      <div class="section-title">${t("journeys.j4")}</div>
      <div class="journey-indicator"><span class="ji-num">4</span>${t("assign.title")}</div>
      <div class="form-grid">
        <div class="form-group">
          <label>${t("assign.selectDriver")}</label>
          <select id="assignDriverSelect">
            <option value="">—</option>
            ${sampleData.drivers.filter((d) => d.availability === "available").map((d) =>
              `<option value="${d.id}">${d.name} (${d.workZones})</option>`
            ).join("")}
          </select>
        </div>
        <div class="form-group">
          <label class="checkbox-label" style="margin-top:1.4rem"><input type="checkbox" id="notifyDriverCheck" checked> ${t("assign.notifyDriver")}</label>
        </div>
      </div>
      <div class="btn-group">
        <button class="btn btn-warning" onclick="assignDriver(${r.id})">${t("assign.assignBtn")}</button>
      </div>`;
  } else if (r.status === "assigned") {
    actionButtons = `
      <div class="section-title">${t("journeys.j5")}</div>
      <div class="journey-indicator"><span class="ji-num">5</span>${t("accept.title")}</div>
      <div class="btn-group">
        <button class="btn btn-success" onclick="acceptRide(${r.id})">${t("accept.acceptBtn")}</button>
        <button class="btn btn-outline" onclick="reassignRide(${r.id})">${t("assign.reassignBtn")}</button>
      </div>`;
  } else if (r.status === "accepted") {
    actionButtons = `
      <div class="section-title">${t("journeys.j6")}</div>
      <div class="journey-indicator"><span class="ji-num">6</span>${t("complete.title")}</div>
      <div class="form-grid">
        <div class="form-group"><label>${t("complete.actualStart")}</label><input type="datetime-local" id="actualStart"></div>
        <div class="form-group"><label>${t("complete.actualEnd")}</label><input type="datetime-local" id="actualEnd"></div>
        <div class="form-group"><label>${t("complete.actualDuration")}</label><input type="number" id="actualDuration" value="0"></div>
        <div class="form-group"><label>${t("complete.waitingTime")}</label><input type="number" id="waitingTime" value="0"></div>
        <div class="form-group"><label>${t("complete.tolls")}</label><input type="number" id="tollsAmount" value="0" step="0.01"></div>
        <div class="form-group"><label>${t("complete.parking")}</label><input type="number" id="parkingAmount" value="0" step="0.01"></div>
        <div class="form-group"><label>${t("complete.additionalCharges")}</label><input type="number" id="addCharges" value="0" step="0.01"></div>
        <div class="form-group"><label>${t("complete.chargeDescription")}</label><input type="text" id="chargeDesc"></div>
      </div>
      <div class="btn-group">
        <button class="btn btn-primary" onclick="completeRide(${r.id})">${t("complete.completeBtn")}</button>
      </div>`;
  } else if (r.status === "completed") {
    actionButtons = `
      <div class="financial-summary">
        <div class="financial-row"><span>${t("invoice.baseCharge")}</span><span>${formatCurrency(r.basePrice)}</span></div>
        <div class="financial-row"><span>${t("complete.waitingTime")}</span><span>${r.waitingTime} min</span></div>
        <div class="financial-row"><span>${t("complete.tolls")}</span><span>${formatCurrency(r.tolls)}</span></div>
        <div class="financial-row"><span>${t("complete.parking")}</span><span>${formatCurrency(r.parking)}</span></div>
        <div class="financial-row"><span>${t("complete.additionalCharges")}</span><span>${formatCurrency(r.additionalCharges)}</span></div>
        <div class="financial-row total"><span>${t("complete.billableAmount")}</span><span>${formatCurrency(r.billableAmount)}</span></div>
      </div>
      <div class="btn-group">
        <button class="btn btn-primary" onclick="closeModal('rideDetailModal'); generateInvoiceFromRide(${r.id})">${t("invoice.generateBtn")}</button>
      </div>`;
  }

  document.getElementById("rideDetailTitle").textContent = `${currentLang === "en" ? "Ride" : "Viaje"} #${r.id}`;
  document.getElementById("rideDetailBody").innerHTML = `
    <div class="workflow-steps">
      <div class="workflow-step ${["scheduled","pendingAssignment","assigned","accepted","completed"].indexOf(r.status) >= 0 ? "done" : ""}">${currentLang === "en" ? "Book" : "Reservar"}</div>
      <div class="workflow-step ${["assigned","accepted","completed"].indexOf(r.status) >= 0 ? "done" : (r.status === "scheduled" ? "active" : "")}">${currentLang === "en" ? "Assign" : "Asignar"}</div>
      <div class="workflow-step ${["accepted","completed"].indexOf(r.status) >= 0 ? "done" : (r.status === "assigned" ? "active" : "")}">${currentLang === "en" ? "Accept" : "Aceptar"}</div>
      <div class="workflow-step ${r.status === "completed" ? "done" : (r.status === "accepted" ? "active" : "")}">${currentLang === "en" ? "Complete" : "Completar"}</div>
      <div class="workflow-step ${sampleData.invoices.some((i) => i.rideId === r.id) ? "done" : (r.status === "completed" ? "active" : "")}">${currentLang === "en" ? "Invoice" : "Facturar"}</div>
    </div>

    <div class="detail-grid">
      <div class="detail-item"><div class="detail-label">${t("invoice.client")}</div><div class="detail-value">${getClientName(r.clientId)}</div></div>
      <div class="detail-item"><div class="detail-label">${t("ride.status")}</div><div class="detail-value">${statusBadge(r.status)}</div></div>
      <div class="detail-item"><div class="detail-label">${t("ride.pickupDate")}</div><div class="detail-value">${formatDateTime(r.pickupDate)}</div></div>
      <div class="detail-item"><div class="detail-label">${t("ride.estimatedDuration")}</div><div class="detail-value">${r.estimatedDuration || "—"}</div></div>
      <div class="detail-item"><div class="detail-label">${t("ride.pickupLocation")}</div><div class="detail-value">${r.pickupLocation}</div></div>
      <div class="detail-item"><div class="detail-label">${t("ride.dropoffLocation")}</div><div class="detail-value">${r.dropoffLocation}</div></div>
      <div class="detail-item"><div class="detail-label">${t("ride.serviceType")}</div><div class="detail-value">${r.serviceType}</div></div>
      <div class="detail-item"><div class="detail-label">${t("assign.assignedTo")}</div><div class="detail-value">${getDriverName(r.assignedDriverId)}</div></div>
      ${r.assignedAt ? `<div class="detail-item"><div class="detail-label">${t("assign.assignedAt")}</div><div class="detail-value">${formatDateTime(r.assignedAt)}</div></div>` : ""}
      ${r.completedByDriverId ? `<div class="detail-item"><div class="detail-label">${t("complete.completedBy")}</div><div class="detail-value">${getDriverName(r.completedByDriverId)}</div></div>` : ""}
      ${r.flightNumber ? `<div class="detail-item"><div class="detail-label">${t("ride.flightNumber")}</div><div class="detail-value">${r.flightNumber}</div></div>` : ""}
      ${r.vipNotes ? `<div class="detail-item full-width"><div class="detail-label">${t("ride.vipNotes")}</div><div class="detail-value">${r.vipNotes}</div></div>` : ""}
    </div>

    ${actionButtons}
  `;
  document.getElementById("rideDetailModal").classList.add("show");
}

function assignDriver(rideId) {
  const driverId = document.getElementById("assignDriverSelect")?.value;
  if (!driverId) return;
  const ride = sampleData.rides.find((r) => r.id === rideId);
  ride.assignedDriverId = Number(driverId);
  ride.assignedAt = new Date().toISOString();
  ride.assignmentStatus = "assigned";
  ride.status = "assigned";
  closeModal("rideDetailModal");
  renderRideTable();
  showAlert("rideAlert", t("assign.successMsg"));
}

function acceptRide(rideId) {
  const ride = sampleData.rides.find((r) => r.id === rideId);
  ride.acceptedAt = new Date().toISOString();
  ride.assignmentStatus = "accepted";
  ride.status = "accepted";
  closeModal("rideDetailModal");
  if (document.getElementById("rideTableBody")) renderRideTable();
  showAlert("rideAlert", t("accept.successMsg"));
}

function reassignRide(rideId) {
  const ride = sampleData.rides.find((r) => r.id === rideId);
  ride.assignedDriverId = null;
  ride.assignedAt = null;
  ride.acceptedAt = null;
  ride.assignmentStatus = "pending";
  ride.status = "scheduled";
  closeModal("rideDetailModal");
  renderRideTable();
  showRideDetail(rideId);
}

function showCompleteRide(rideId) {
  showRideDetail(rideId);
}

function completeRide(rideId) {
  const ride = sampleData.rides.find((r) => r.id === rideId);
  ride.actualStart = document.getElementById("actualStart").value;
  ride.actualEnd = document.getElementById("actualEnd").value;
  ride.actualDuration = Number(document.getElementById("actualDuration").value);
  ride.waitingTime = Number(document.getElementById("waitingTime").value);
  ride.tolls = Number(document.getElementById("tollsAmount").value);
  ride.parking = Number(document.getElementById("parkingAmount").value);
  ride.additionalCharges = Number(document.getElementById("addCharges").value);
  ride.chargeDescription = document.getElementById("chargeDesc").value;
  ride.status = "completed";
  ride.completedByDriverId = ride.assignedDriverId;
  ride.billableAmount = ride.basePrice + ride.tolls + ride.parking + ride.additionalCharges;
  closeModal("rideDetailModal");
  renderRideTable();
  showAlert("rideAlert", t("complete.successMsg"));
}

// ============================================================
// INVOICES (Journeys 7, 9)
// ============================================================

function renderInvoices(area) {
  area.innerHTML = `
    <div class="journey-indicator">
      <span class="ji-num">7</span>
      ${t("journeys.j7")}
    </div>
    <div id="invoiceAlert"></div>

    <div class="card">
      <div class="card-header">
        <h2>${t("invoice.listTitle")}</h2>
      </div>
      <div class="card-body">
        <div class="table-wrapper">
          <table>
            <thead><tr>
              <th>${t("invoice.invoiceNumber")}</th>
              <th>${t("common.date")}</th>
              <th>${t("invoice.client")}</th>
              <th>${t("invoice.baseCharge")}</th>
              <th>${t("invoice.additionalCharges")}</th>
              <th>${t("invoice.total")}</th>
              <th>${t("invoice.status")}</th>
              <th>${t("common.actions")}</th>
            </tr></thead>
            <tbody id="invoiceTableBody"></tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Invoice Detail Modal -->
    <div class="modal-overlay" id="invoiceDetailModal">
      <div class="modal" style="max-width:650px">
        <div class="modal-header">
          <h3 id="invoiceDetailTitle"></h3>
          <button class="modal-close" onclick="closeModal('invoiceDetailModal')">&times;</button>
        </div>
        <div class="modal-body" id="invoiceDetailBody"></div>
      </div>
    </div>
  `;
  renderInvoiceTable();
}

function renderInvoiceTable() {
  const tbody = document.getElementById("invoiceTableBody");
  if (!tbody) return;
  tbody.innerHTML = sampleData.invoices.map((inv) => `
    <tr onclick="showInvoiceDetail('${inv.id}')">
      <td><strong>${inv.id}</strong></td>
      <td>${formatDate(inv.createdAt)}</td>
      <td>${getClientName(inv.clientId)}</td>
      <td>${formatCurrency(inv.baseCharge)}</td>
      <td>${formatCurrency(inv.additionalCharges)}</td>
      <td><strong>${formatCurrency(inv.total)}</strong></td>
      <td>${statusBadge(inv.status)}</td>
      <td>
        ${inv.status !== "paid" ? `<button class="btn btn-sm btn-success" onclick="event.stopPropagation(); showPaymentForm('${inv.id}')">${t("payment.saveBtn")}</button>` : ""}
      </td>
    </tr>
  `).join("");
}

function generateInvoiceFromRide(rideId) {
  const ride = sampleData.rides.find((r) => r.id === rideId);
  if (!ride || ride.status !== "completed") return;
  // Check if invoice already exists
  if (sampleData.invoices.some((i) => i.rideId === rideId)) {
    navigate("invoices");
    return;
  }
  const tax = ride.billableAmount * 0.08;
  const total = ride.billableAmount + tax;
  sampleData.invoices.push({
    id: nextInvoiceNumber(),
    clientId: ride.clientId,
    rideId: ride.id,
    baseCharge: ride.basePrice,
    additionalCharges: ride.tolls + ride.parking + ride.additionalCharges,
    tax: Math.round(tax * 100) / 100,
    total: Math.round(total * 100) / 100,
    status: "outstanding",
    createdAt: new Date().toISOString().split("T")[0],
    payments: [],
  });
  navigate("invoices");
  showAlert("invoiceAlert", t("invoice.successMsg"));
}

function showInvoiceDetail(invoiceId) {
  const inv = sampleData.invoices.find((i) => i.id === invoiceId);
  if (!inv) return;
  const ride = sampleData.rides.find((r) => r.id === inv.rideId);
  const totalPaid = inv.payments.reduce((s, p) => s + p.amount, 0);
  const remaining = inv.total - totalPaid;

  document.getElementById("invoiceDetailTitle").textContent = inv.id;
  document.getElementById("invoiceDetailBody").innerHTML = `
    <div class="detail-grid">
      <div class="detail-item"><div class="detail-label">${t("invoice.client")}</div><div class="detail-value">${getClientName(inv.clientId)}</div></div>
      <div class="detail-item"><div class="detail-label">${t("invoice.status")}</div><div class="detail-value">${statusBadge(inv.status)}</div></div>
      <div class="detail-item"><div class="detail-label">${t("common.date")}</div><div class="detail-value">${formatDate(inv.createdAt)}</div></div>
      ${ride ? `<div class="detail-item"><div class="detail-label">${currentLang === "en" ? "Ride" : "Viaje"}</div><div class="detail-value">#${ride.id} — ${ride.pickupLocation} → ${ride.dropoffLocation}</div></div>` : ""}
    </div>

    <div class="financial-summary">
      <div class="financial-row"><span>${t("invoice.baseCharge")}</span><span>${formatCurrency(inv.baseCharge)}</span></div>
      <div class="financial-row"><span>${t("invoice.additionalCharges")}</span><span>${formatCurrency(inv.additionalCharges)}</span></div>
      <div class="financial-row"><span>${t("invoice.tax")} (8%)</span><span>${formatCurrency(inv.tax)}</span></div>
      <div class="financial-row total"><span>${t("invoice.total")}</span><span>${formatCurrency(inv.total)}</span></div>
    </div>

    <div class="section-title">${t("payment.history")}</div>
    ${inv.payments.length ? `
      <div class="table-wrapper"><table>
        <thead><tr><th>${t("common.date")}</th><th>${t("common.amount")}</th><th>${t("payment.paymentMethod")}</th><th>${t("payment.reference")}</th></tr></thead>
        <tbody>${inv.payments.map((p) => `<tr>
          <td>${formatDate(p.date)}</td><td>${formatCurrency(p.amount)}</td><td>${p.method}</td><td>${p.reference}</td>
        </tr>`).join("")}</tbody>
      </table></div>
    ` : `<p style="color:var(--gray-400);font-size:0.85rem">${currentLang === "en" ? "No payments recorded" : "Sin pagos registrados"}</p>`}

    <div class="financial-summary mt-2">
      <div class="financial-row"><span>${t("payment.totalPaid")}</span><span class="positive">${formatCurrency(totalPaid)}</span></div>
      <div class="financial-row total"><span>${t("payment.remaining")}</span><span class="${remaining > 0 ? "negative" : ""}">${formatCurrency(remaining)}</span></div>
    </div>

    ${inv.status !== "paid" ? `
      <div class="section-title">${t("journeys.j9")}</div>
      <div class="journey-indicator"><span class="ji-num">9</span>${t("payment.title")}</div>
      <div class="form-grid">
        <div class="form-group">
          <label>${t("payment.paymentDate")}</label>
          <input type="date" id="payDate" value="${new Date().toISOString().split("T")[0]}">
        </div>
        <div class="form-group">
          <label>${t("payment.amountPaid")}</label>
          <input type="number" id="payAmount" value="${remaining.toFixed(2)}" step="0.01">
        </div>
        <div class="form-group">
          <label>${t("payment.paymentMethod")}</label>
          <select id="payMethod">
            <option value="cash">${t("payment.cash")}</option>
            <option value="bank_transfer">${t("payment.bankTransfer")}</option>
            <option value="card">${t("payment.card")}</option>
            <option value="check">${t("payment.check")}</option>
            <option value="zelle">${t("payment.zelle")}</option>
            <option value="venmo">${t("payment.venmo")}</option>
          </select>
        </div>
        <div class="form-group">
          <label>${t("payment.reference")}</label>
          <input type="text" id="payRef">
        </div>
      </div>
      <div class="btn-group">
        <button class="btn btn-success" onclick="recordPayment('${inv.id}')">${t("payment.saveBtn")}</button>
      </div>
    ` : ""}
  `;
  document.getElementById("invoiceDetailModal").classList.add("show");
}

function showPaymentForm(invoiceId) {
  showInvoiceDetail(invoiceId);
}

function recordPayment(invoiceId) {
  const inv = sampleData.invoices.find((i) => i.id === invoiceId);
  if (!inv) return;
  const amount = Number(document.getElementById("payAmount").value);
  inv.payments.push({
    date: document.getElementById("payDate").value,
    amount,
    method: document.getElementById("payMethod").value,
    reference: document.getElementById("payRef").value,
  });
  const totalPaid = inv.payments.reduce((s, p) => s + p.amount, 0);
  if (totalPaid >= inv.total) inv.status = "paid";
  else if (totalPaid > 0) inv.status = "partiallyPaid";
  closeModal("invoiceDetailModal");
  renderInvoiceTable();
  showAlert("invoiceAlert", t("payment.successMsg"));
}

// ============================================================
// PAYOUTS (Journey 8)
// ============================================================

function renderPayouts(area) {
  area.innerHTML = `
    <div class="journey-indicator">
      <span class="ji-num">8</span>
      ${t("journeys.j8")}
    </div>
    <div id="payoutAlert"></div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">${t("payout.title")}</div>
        <div class="stat-value">${formatCurrency(sampleData.payouts.reduce((s, p) => s + p.amount, 0))}</div>
        <div class="stat-detail">${sampleData.payouts.length} payouts</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">${t("payout.pending")}</div>
        <div class="stat-value">${formatCurrency(sampleData.payouts.filter((p) => p.status === "pending").reduce((s, p) => s + p.amount, 0))}</div>
        <div class="stat-detail">${sampleData.payouts.filter((p) => p.status === "pending").length} pending</div>
      </div>
    </div>

    <div class="card">
      <div class="card-header">
        <h2>${t("payout.listTitle")}</h2>
        <button class="btn btn-primary" onclick="showPayoutForm()">${t("payout.title")}</button>
      </div>
      <div class="card-body">
        <div class="table-wrapper">
          <table>
            <thead><tr>
              <th>${currentLang === "en" ? "Ride" : "Viaje"}</th>
              <th>${t("payout.driverName")}</th>
              <th>${t("payout.amount")}</th>
              <th>${t("payout.type")}</th>
              <th>${t("payout.paymentStatus")}</th>
              <th>${t("payout.notes")}</th>
            </tr></thead>
            <tbody id="payoutTableBody"></tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- Margin Analysis -->
    <div class="card mt-2">
      <div class="card-header"><h2>${t("payout.margin")}</h2></div>
      <div class="card-body" id="marginAnalysis"></div>
    </div>

    <!-- Payout Form Modal -->
    <div class="modal-overlay" id="payoutModal">
      <div class="modal">
        <div class="modal-header">
          <h3>${t("payout.title")}</h3>
          <button class="modal-close" onclick="closeModal('payoutModal')">&times;</button>
        </div>
        <div class="modal-body">
          <div class="form-grid">
            <div class="form-group">
              <label>${currentLang === "en" ? "Ride" : "Viaje"}</label>
              <select id="payoutRide">
                <option value="">—</option>
                ${sampleData.rides.filter((r) => r.status === "completed").map((r) =>
                  `<option value="${r.id}">#${r.id} — ${getClientName(r.clientId)} (${formatCurrency(r.billableAmount)})</option>`
                ).join("")}
              </select>
            </div>
            <div class="form-group">
              <label>${t("payout.driverName")}</label>
              <select id="payoutDriver">
                ${sampleData.drivers.map((d) => `<option value="${d.id}">${d.name}</option>`).join("")}
              </select>
            </div>
            <div class="form-group">
              <label>${t("payout.amount")}</label>
              <input type="number" id="payoutAmount" value="0" step="0.01">
            </div>
            <div class="form-group">
              <label>${t("payout.type")}</label>
              <select id="payoutType">
                <option value="flat">${t("payout.flat")}</option>
                <option value="percentage">${t("payout.percentage")}</option>
              </select>
            </div>
            <div class="form-group">
              <label>${t("payout.paymentStatus")}</label>
              <select id="payoutStatus">
                <option value="pending">${t("payout.pending")}</option>
                <option value="paid">${t("payout.paid")}</option>
              </select>
            </div>
            <div class="form-group">
              <label>${t("payout.notes")}</label>
              <input type="text" id="payoutNotes">
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn btn-outline" onclick="closeModal('payoutModal')">${t("common.cancel")}</button>
          <button class="btn btn-primary" onclick="savePayout()">${t("payout.saveBtn")}</button>
        </div>
      </div>
    </div>
  `;
  renderPayoutTable();
  renderMarginAnalysis();
}

function renderPayoutTable() {
  const tbody = document.getElementById("payoutTableBody");
  if (!tbody) return;
  tbody.innerHTML = sampleData.payouts.map((p) => `
    <tr>
      <td>#${p.rideId}</td>
      <td>${getDriverName(p.driverId)}</td>
      <td>${formatCurrency(p.amount)}</td>
      <td>${p.type}</td>
      <td>${statusBadge(p.status)}</td>
      <td>${p.notes || "—"}</td>
    </tr>
  `).join("");
}

function renderMarginAnalysis() {
  const el = document.getElementById("marginAnalysis");
  if (!el) return;
  // Show margin per completed ride that has both invoice and payout
  const rows = sampleData.rides.filter((r) => r.status === "completed").map((r) => {
    const inv = sampleData.invoices.find((i) => i.rideId === r.id);
    const pay = sampleData.payouts.find((p) => p.rideId === r.id);
    const clientCharge = inv ? inv.total : r.billableAmount || 0;
    const driverPay = pay ? pay.amount : 0;
    const margin = clientCharge - driverPay;
    return { ride: r, clientCharge, driverPay, margin };
  });

  el.innerHTML = `<div class="table-wrapper"><table>
    <thead><tr>
      <th>${currentLang === "en" ? "Ride" : "Viaje"}</th>
      <th>${t("invoice.client")}</th>
      <th>${t("assign.assignedTo")}</th>
      <th>${t("payout.clientCharge")}</th>
      <th>${t("payout.driverPayout")}</th>
      <th>${t("payout.margin")}</th>
    </tr></thead>
    <tbody>${rows.map((row) => `<tr>
      <td>#${row.ride.id}</td>
      <td>${getClientName(row.ride.clientId)}</td>
      <td>${getDriverName(row.ride.assignedDriverId)}</td>
      <td>${formatCurrency(row.clientCharge)}</td>
      <td>${formatCurrency(row.driverPay)}</td>
      <td><strong class="${row.margin >= 0 ? "positive" : "negative"}" style="color:${row.margin >= 0 ? "var(--success)" : "var(--danger)"}">${formatCurrency(row.margin)}</strong></td>
    </tr>`).join("")}</tbody>
  </table></div>`;
}

function showPayoutForm() {
  document.getElementById("payoutModal").classList.add("show");
}

function savePayout() {
  const rideId = document.getElementById("payoutRide").value;
  sampleData.payouts.push({
    id: nextPayoutId(),
    rideId: rideId ? Number(rideId) : null,
    driverId: Number(document.getElementById("payoutDriver").value),
    amount: Number(document.getElementById("payoutAmount").value),
    type: document.getElementById("payoutType").value,
    status: document.getElementById("payoutStatus").value,
    payoutDate: new Date().toISOString().split("T")[0],
    notes: document.getElementById("payoutNotes").value,
  });
  closeModal("payoutModal");
  renderPayoutTable();
  renderMarginAnalysis();
  showAlert("payoutAlert", t("payout.successMsg"));
}

// ============================================================
// REPORTS (Journey 11)
// ============================================================

function renderReports(area) {
  const totalRev = sampleData.invoices.reduce((s, i) => s + i.total, 0);
  const totalPay = sampleData.payouts.reduce((s, p) => s + p.amount, 0);
  const completedRides = sampleData.rides.filter((r) => r.status === "completed").length;

  area.innerHTML = `
    <div class="journey-indicator">
      <span class="ji-num">11</span>
      ${t("journeys.j11")}
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">${t("reports.totalRevenue")}</div>
        <div class="stat-value">${formatCurrency(totalRev)}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">${t("reports.totalPayouts")}</div>
        <div class="stat-value">${formatCurrency(totalPay)}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">${t("reports.netMargin")}</div>
        <div class="stat-value">${formatCurrency(totalRev - totalPay)}</div>
        <div class="stat-detail">${totalRev > 0 ? ((totalRev - totalPay) / totalRev * 100).toFixed(1) : 0}%</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">${t("reports.ridesCompleted")}</div>
        <div class="stat-value">${completedRides}</div>
        <div class="stat-detail">${t("reports.avgPerRide")}: ${completedRides ? formatCurrency(totalRev / completedRides) : "$0.00"}</div>
      </div>
    </div>

    <div class="filters-bar">
      <div class="form-group">
        <label>${t("reports.filterDriver")}</label>
        <select id="reportDriverFilter" onchange="updateReportTables()">
          <option value="">${t("reports.allDrivers")}</option>
          ${sampleData.drivers.map((d) => `<option value="${d.id}">${d.name}</option>`).join("")}
        </select>
      </div>
      <div class="form-group">
        <label>${t("reports.filterClient")}</label>
        <select id="reportClientFilter" onchange="updateReportTables()">
          <option value="">${t("reports.allClients")}</option>
          ${sampleData.clients.map((c) => `<option value="${c.id}">${c.fullName}</option>`).join("")}
        </select>
      </div>
      <button class="btn btn-outline btn-sm" onclick="exportReport()" style="align-self:flex-end">${t("reports.exportBtn")}</button>
    </div>

    <!-- Revenue by Client -->
    <div class="card">
      <div class="card-header"><h2>${t("reports.revenueByClient")}</h2></div>
      <div class="card-body"><div class="table-wrapper" id="revenueByClientTable"></div></div>
    </div>

    <!-- Revenue by Driver -->
    <div class="card mt-2">
      <div class="card-header"><h2>${t("reports.revenueByDriver")}</h2></div>
      <div class="card-body"><div class="table-wrapper" id="revenueByDriverTable"></div></div>
    </div>

    <!-- Outstanding Invoices -->
    <div class="card mt-2">
      <div class="card-header"><h2>${t("reports.outstandingInvoices")}</h2></div>
      <div class="card-body"><div class="table-wrapper" id="outstandingTable"></div></div>
    </div>
  `;
  updateReportTables();
}

function updateReportTables() {
  // Revenue by client
  const byClient = {};
  sampleData.invoices.forEach((inv) => {
    const name = getClientName(inv.clientId);
    if (!byClient[name]) byClient[name] = { revenue: 0, rides: 0 };
    byClient[name].revenue += inv.total;
    byClient[name].rides += 1;
  });
  document.getElementById("revenueByClientTable").innerHTML = `<table>
    <thead><tr><th>${t("invoice.client")}</th><th>${t("reports.ridesCompleted")}</th><th>${t("reports.totalRevenue")}</th></tr></thead>
    <tbody>${Object.entries(byClient).map(([name, d]) => `<tr><td>${name}</td><td>${d.rides}</td><td><strong>${formatCurrency(d.revenue)}</strong></td></tr>`).join("")}</tbody>
  </table>`;

  // Revenue by driver
  const byDriver = {};
  sampleData.rides.filter((r) => r.status === "completed").forEach((r) => {
    const name = getDriverName(r.assignedDriverId);
    if (!byDriver[name]) byDriver[name] = { revenue: 0, rides: 0, payouts: 0 };
    byDriver[name].revenue += r.billableAmount || 0;
    byDriver[name].rides += 1;
    const pay = sampleData.payouts.find((p) => p.rideId === r.id);
    if (pay) byDriver[name].payouts += pay.amount;
  });
  document.getElementById("revenueByDriverTable").innerHTML = `<table>
    <thead><tr><th>${t("payout.driverName")}</th><th>${t("reports.ridesCompleted")}</th><th>${t("reports.totalRevenue")}</th><th>${t("reports.driverPayouts")}</th><th>${t("reports.businessMargin")}</th></tr></thead>
    <tbody>${Object.entries(byDriver).map(([name, d]) => `<tr>
      <td>${name}</td><td>${d.rides}</td><td>${formatCurrency(d.revenue)}</td><td>${formatCurrency(d.payouts)}</td>
      <td><strong style="color:var(--success)">${formatCurrency(d.revenue - d.payouts)}</strong></td>
    </tr>`).join("")}</tbody>
  </table>`;

  // Outstanding
  const outstanding = sampleData.invoices.filter((i) => i.status !== "paid");
  document.getElementById("outstandingTable").innerHTML = outstanding.length ? `<table>
    <thead><tr><th>${t("invoice.invoiceNumber")}</th><th>${t("invoice.client")}</th><th>${t("invoice.total")}</th><th>${t("invoice.status")}</th></tr></thead>
    <tbody>${outstanding.map((i) => `<tr onclick="navigate('invoices')">
      <td>${i.id}</td><td>${getClientName(i.clientId)}</td><td><strong>${formatCurrency(i.total)}</strong></td><td>${statusBadge(i.status)}</td>
    </tr>`).join("")}</tbody>
  </table>` : `<p style="text-align:center;color:var(--gray-400);padding:1rem">${currentLang === "en" ? "No outstanding invoices" : "Sin facturas pendientes"}</p>`;
}

function exportReport() {
  alert(currentLang === "en" ? "Export feature: In production, this would generate a CSV/PDF report for accounting." : "Funcion de exportacion: En produccion, esto generaria un reporte CSV/PDF para contabilidad.");
}

// ============================================================
// Initialize
// ============================================================

document.addEventListener("DOMContentLoaded", () => {
  navigate("dashboard");
});
