const state = {
  operations: null,
  routeSummary: null,
  outbox: null,
  nexusSettlement: null,
};

const $ = (id) => document.getElementById(id);
const languages = ["ko", "en", "ja", "zh"];

function initLanguageSelector() {
  const selector = $("languageSelector");
  if (!selector) return;
  const saved = localStorage.getItem("archive-logistics-language");
  const language = languages.includes(saved) ? saved : "ko";
  selector.value = language;
  document.documentElement.lang = language;
  document.documentElement.dataset.language = language;
  selector.addEventListener("change", () => {
    localStorage.setItem("archive-logistics-language", selector.value);
    document.documentElement.lang = selector.value;
    document.documentElement.dataset.language = selector.value;
  });
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString("ko-KR");
}

function formatCurrency(value) {
  return `${formatNumber(value)} KRW`;
}

function nowText() {
  return new Date().toLocaleString("ko-KR", { hour12: false });
}

function setText(id, value) {
  const element = $(id);
  if (element) {
    element.textContent = value;
  }
}

function logActivity(title, detail = "") {
  const list = $("activityLog");
  const item = document.createElement("li");
  item.innerHTML = `<strong>${title}</strong>${detail ? ` - ${detail}` : ""}`;
  list.prepend(item);
  while (list.children.length > 9) {
    list.removeChild(list.lastElementChild);
  }
}

async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { "Accept": "application/json" },
    ...options,
  });

  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`);
  }

  return response.json();
}

async function optionalRequest(path) {
  try {
    return await request(path);
  } catch (error) {
    return { error };
  }
}

function routeSummaryUrl() {
  const params = new URLSearchParams();
  const factoryId = $("factoryId").value;
  const date = $("summaryDate").value;

  if (factoryId) {
    params.set("factoryId", factoryId);
  }

  if (date) {
    params.set("date", date);
  }

  return `/api/routes/summary${params.toString() ? `?${params}` : ""}`;
}

function updateOperations(payload) {
  const data = payload.data || {};
  state.operations = data;

  setText("traceId", `trace ${payload.traceId || "-"}`);
  setText("serviceStatus", data.status || "-");
  setText("profile", data.profile || "-");
  setText("ledgerStatus", data.ledger?.status || "-");
  setText("receivedEvents", formatNumber(data.receivedEvents));
  setText("processedEvents", formatNumber(data.processedEvents));
  setText("duplicateEvents", formatNumber(data.duplicateEvents));
  setText("failedEvents", formatNumber(data.failedEvents));
  setText("routePlans", formatNumber(data.routePlans));
  setText("approvalRequired", formatNumber(data.risk?.approvalRequired));
  setText("riskApproval", formatNumber(data.risk?.approvalRequired));
  setText("riskDelayed", formatNumber(data.risk?.delayedRoutes));
  setText("riskDeviation", formatNumber(data.risk?.deviatedRoutes));
  setText("riskColdChain", formatNumber(data.risk?.coldChainRisk));
  setText("ledgerMode", data.ledger?.enabled ? "ENABLED" : "DRY_RUN");
  setText("ledgerEnabled", String(Boolean(data.ledger?.enabled)));
  const ledgerBase = data.ledger?.baseUrl || "";
  const ledgerPath = data.ledger?.bulkEndpoint || "";
  setText("ledgerEndpoint", ledgerBase ? `${ledgerBase.replace(/\/+$/, "")}${ledgerPath.startsWith("/") ? ledgerPath : `/${ledgerPath}`}` : "-");
  setText("contractMode", data.ledger?.contractMode || "-");
  setText("usedHeap", `${formatNumber(data.memory?.usedHeapMb)} MB`);
  setText("lastRefresh", nowText());
}

function updateRouteSummary(payload) {
  const data = payload.data || {};
  state.routeSummary = data;

  setText("summaryRoutes", formatNumber(data.routePlans));
  setText("totalCost", formatCurrency(data.totalCost));
  setText("delayedRoutes", formatNumber(data.delayedRoutes));
  setText("deviatedRoutes", formatNumber(data.deviatedRoutes));
}

function updateOutbox(payload) {
  const data = payload.data || {};
  state.outbox = data;

  const pending = Number(data.pending || 0);
  const published = Number(data.published || 0);
  const retry = Number(data.retry || 0);
  const failed = Number(data.failed || 0);
  const skipped = Number(data.skipped || 0);
  const total = pending + published + retry + failed + skipped;
  const percent = (value) => `${total ? Math.max((value / total) * 100, value ? 3 : 0) : 0}%`;

  setText("outboxPending", formatNumber(pending));
  setText("outboxTotal", `${formatNumber(total)} events`);
  setText("pendingCount", formatNumber(pending));
  setText("publishedCount", formatNumber(published));
  setText("retryCount", formatNumber(retry));
  setText("failedCount", formatNumber(failed));
  setText("skippedCount", formatNumber(skipped));

  $("barPending").style.width = percent(pending);
  $("barPublished").style.width = percent(published);
  $("barRetry").style.width = percent(retry);
  $("barFailed").style.width = percent(failed);
  $("barSkipped").style.width = percent(skipped);
}

function updateNexusSettlement(payload) {
  if (payload.error) {
    setText("nexusSettlementStatus", "ERROR");
    setText("nexusSettlementMode", payload.error.message);
    return;
  }

  const data = payload.data || {};
  state.nexusSettlement = data;

  const totalRows = Number(data.totalSettlements || 0);
  const sent = Number(data.sent || 0);
  const dryRun = Number(data.dryRun || 0);
  const retry = Number(data.retry || 0);
  const failed = Number(data.failed || 0);

  setText("nexusSettlementRows", formatNumber(totalRows));
  setText("nexusSettlementSent", `${formatNumber(sent)} / ${formatNumber(dryRun)}`);
  setText("nexusManufacturingCost", formatCurrency(data.totalManufacturingImpactCost));
  setText("nexusSettlementStatus", failed ? "FAILED" : retry ? "RETRY" : sent ? "SENT" : dryRun ? "DRY_RUN" : "READY");
  setText("nexusSettlementMode", "PUBLISHED outbox basis");
}

async function refresh() {
  $("refreshButton").disabled = true;
  try {
    const [operations, routeSummary, outbox, nexusSettlement] = await Promise.all([
      request("/api/operations/summary"),
      request(routeSummaryUrl()),
      request("/api/outbox/summary"),
      optionalRequest("/api/settlements/nexus-daily/summary"),
    ]);

    updateOperations(operations);
    updateRouteSummary(routeSummary);
    updateOutbox(outbox);
    updateNexusSettlement(nexusSettlement);
    logActivity("Refresh", "operations, route, outbox, settlement summary loaded");
  } catch (error) {
    logActivity("Refresh failed", error.message);
  } finally {
    $("refreshButton").disabled = false;
  }
}

async function simulate() {
  $("simulateButton").disabled = true;
  try {
    const result = await request("/api/simulations/shipments?count=100", { method: "POST" });
    const data = result.data || {};
    logActivity("Simulation", `processed ${formatNumber(data.processedCount)} / outbox ${formatNumber(data.outboxCreatedCount)}`);
    await refresh();
  } catch (error) {
    logActivity("Simulation failed", error.message);
  } finally {
    $("simulateButton").disabled = false;
  }
}

async function publishOutbox() {
  $("publishButton").disabled = true;
  try {
    const result = await request("/api/outbox/publish", { method: "POST" });
    const data = result.data || {};
    setText("lastPublish", `${data.status || "-"} | requested ${formatNumber(data.requestedCount)} | published ${formatNumber(data.publishedCount)} | retry ${formatNumber(data.retriedCount)} | skipped ${formatNumber(data.skippedCount)}`);
    logActivity("Publish", data.message || data.status || "completed");
    await refresh();
  } catch (error) {
    logActivity("Publish failed", error.message);
  } finally {
    $("publishButton").disabled = false;
  }
}

async function runNexusSettlement() {
  $("settlementButton").disabled = true;
  try {
    const result = await request("/api/settlements/nexus-daily/run", { method: "POST" });
    const data = result.data || {};
    setText("lastNexusSettlement", `${data.sentCount || 0} sent / ${data.dryRunCount || 0} dry-run / ${data.skippedCount || 0} skipped`);
    logActivity("Nexus Settlement", `date ${data.settlementDate || "-"} | factories ${formatNumber(data.requestedFactoryCount)}`);
    await refresh();
  } catch (error) {
    logActivity("Nexus settlement failed", error.message);
  } finally {
    $("settlementButton").disabled = false;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  initLanguageSelector();
  $("refreshButton").addEventListener("click", refresh);
  $("simulateButton").addEventListener("click", simulate);
  $("publishButton").addEventListener("click", publishOutbox);
  $("settlementButton").addEventListener("click", runNexusSettlement);
  $("routeFilter").addEventListener("submit", (event) => {
    event.preventDefault();
    refresh();
  });

  refresh();
  window.setInterval(refresh, 30000);
});
