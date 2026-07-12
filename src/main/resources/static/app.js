const i18n = window.ArchiveLogisticsI18n;

const state = {
  locale: i18n.readLocale(),
  operationsPayload: null,
  routeSummaryPayload: null,
  outboxPayload: null,
  nexusSettlementPayload: null,
  lastPublishPayload: null,
  lastNexusSettlementPayload: null,
};

const $ = (id) => document.getElementById(id);
const localeMap = {
  ko: "ko-KR",
  en: "en-US",
  ja: "ja-JP",
  "zh-CN": "zh-CN",
};

function t(key, params = {}) {
  return i18n.t(state.locale, key, params);
}

function setText(id, value) {
  const element = $(id);
  if (element) {
    element.textContent = value;
  }
}

function formatNumber(value) {
  return Number(value || 0).toLocaleString(localeMap[state.locale] || "ko-KR");
}

function formatCurrency(value) {
  return t("common.currencyKrw", { value: formatNumber(value) });
}

function formatOptionalCurrency(value) {
  return value == null ? t("common.noData") : formatCurrency(value);
}

function formatOptionalNumber(value) {
  return value == null ? t("common.noData") : formatNumber(value);
}

function formatPercent(value) {
  return value == null ? t("common.noData") : `${(Number(value) * 100).toFixed(1)}%`;
}

function formatTimestamp(value) {
  if (!value) return t("common.noData");
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? String(value) : date.toLocaleString(localeMap[state.locale] || "ko-KR", { hour12: false });
}

function nowText() {
  return new Date().toLocaleString(localeMap[state.locale] || "ko-KR", { hour12: false });
}

function statusLabel(code) {
  if (!code) return "-";
  return t(`status.${code}`, {});
}

function booleanLabel(value) {
  return value ? t("common.true") : t("common.false");
}

function applyTranslations() {
  document.documentElement.lang = state.locale;
  document.documentElement.dataset.language = state.locale;
  document.title = t("page.title");

  document.querySelectorAll("[data-i18n]").forEach((element) => {
    element.textContent = t(element.dataset.i18n);
  });
  document.querySelectorAll("[data-i18n-title]").forEach((element) => {
    element.setAttribute("title", t(element.dataset.i18nTitle));
  });
  document.querySelectorAll("[data-i18n-aria-label]").forEach((element) => {
    element.setAttribute("aria-label", t(element.dataset.i18nAriaLabel));
  });
  document.querySelectorAll("[data-i18n-placeholder]").forEach((element) => {
    element.setAttribute("placeholder", t(element.dataset.i18nPlaceholder));
  });
}

function renderStoredData() {
  if (state.operationsPayload) updateOperations(state.operationsPayload);
  if (state.routeSummaryPayload) updateRouteSummary(state.routeSummaryPayload);
  if (state.outboxPayload) updateOutbox(state.outboxPayload);
  if (state.nexusSettlementPayload) updateNexusSettlement(state.nexusSettlementPayload);
  if (state.lastPublishPayload) renderLastPublish(state.lastPublishPayload);
  if (state.lastNexusSettlementPayload) renderLastNexusSettlement(state.lastNexusSettlementPayload);
}

function setLocale(locale) {
  state.locale = i18n.normalizeLocale(locale);
  localStorage.setItem(i18n.storageKey, state.locale);
  localStorage.removeItem("archive-logistics-language");
  const selector = $("languageSelector");
  if (selector) {
    selector.value = state.locale;
  }
  applyTranslations();
  renderStoredData();
}

function initLanguageSelector() {
  const selector = $("languageSelector");
  if (!selector) return;

  for (const option of selector.options) {
    option.textContent = i18n.localeNames[option.value] || option.textContent;
  }
  selector.value = state.locale;
  selector.addEventListener("change", () => setLocale(selector.value));
  setLocale(state.locale);
}

function logActivity(titleKey, detail = "") {
  const list = $("activityLog");
  if (!list) return;

  const item = document.createElement("li");
  const title = t(titleKey);
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
  state.operationsPayload = payload;
  const data = payload.data || {};

  setText("traceId", t("common.trace", { traceId: payload.traceId || "-" }));
  setText("serviceStatus", statusLabel(data.status));
  setText("profile", data.profile || "-");
  setText("ledgerStatus", statusLabel(data.ledger?.status));
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
  setText("ledgerMode", data.ledger?.enabled ? statusLabel("ENABLED") : statusLabel("DRY_RUN"));
  setText("ledgerEnabled", booleanLabel(Boolean(data.ledger?.enabled)));
  const ledgerBase = data.ledger?.baseUrl || "";
  const ledgerPath = data.ledger?.bulkEndpoint || "";
  setText("ledgerEndpoint", ledgerBase ? `${ledgerBase.replace(/\/+$/, "")}${ledgerPath.startsWith("/") ? ledgerPath : `/${ledgerPath}`}` : "-");
  setText("contractMode", data.ledger?.contractMode || "-");
  setText("usedHeap", t("common.usedHeapValue", { value: formatNumber(data.memory?.usedHeapMb) }));
  updateRuntimeDashboard(data);
  setText("lastRefresh", nowText());
}

function updateRuntimeDashboard(data) {
  const runtime = data.runtime || {};
  const workforce = data.workforce || {};
  const balance = data.balance || {};
  const noData = t("common.noData");
  const runtimeStatus = runtime.pipelineStatus || runtime.schedulerStatus || (data.liveFlowAvailable ? "LIVE" : "NO_DATA");
  const produced = runtime.eventsProducedLastTick;
  const consumed = runtime.eventsConsumedLastTick;

  setText("runtimePipelineStatus", runtimeStatus);
  setText("runtimeLastEvent", `${t("panel.latestEvent")}: ${formatTimestamp(runtime.lastEventAt || data.latestEventAt)}`);
  setText("runtimeTickFlow", produced == null && consumed == null ? noData : `${formatOptionalNumber(produced)} / ${formatOptionalNumber(consumed)}`);
  setText("runtimeBacklog", formatOptionalNumber(balance.backlogCount ?? workforce.backlogCount ?? runtime.backlogCount));
  setText("runtimeCapacityUtilization", formatPercent(balance.capacityUtilization));

  setText("lifecycleRequested", formatOptionalNumber(balance.shipmentsRequested));
  setText("lifecycleDispatched", formatOptionalNumber(balance.shipmentsDispatched));
  setText("lifecycleCompleted", formatOptionalNumber(balance.shipmentsCompleted));
  setText("lifecycleDelayed", formatOptionalNumber(balance.shipmentsDelayed));
  setText("lifecycleDelayRate", formatPercent(balance.delayRate));
  setText("lifecycleAverageEta", balance.averageEta == null ? noData : t("common.minutes", { value: formatNumber(balance.averageEta) }));
  setText("lifecycleRemainingCapacity", formatOptionalNumber(workforce.remainingCapacity));
  setText("lifecycleBottleneck", balance.bottleneckRole || workforce.bottleneckRole || noData);

  setText("balanceScope", balance.calculationScope || balance.status || noData);
  setText("balanceRevenue", formatOptionalCurrency(balance.logisticsRevenue));
  setText("balanceCost", formatOptionalCurrency(balance.totalCost));
  setText("balanceProfit", formatOptionalCurrency(balance.operatingProfit));
  setText("balanceMargin", formatPercent(balance.operatingMargin));
  setText("balanceCash", formatOptionalCurrency(balance.cashBalance));
  setText("balanceNegativeProfitStreak", formatOptionalNumber(balance.negativeProfitStreak));
  setText("balanceFuelCost", formatOptionalCurrency(balance.fuelCost));
  setText("balanceTollCost", formatOptionalCurrency(balance.tollCost));
  setText("balanceWorkforceCost", formatOptionalCurrency(balance.workforceCost));
  const additionalCost = [balance.delayPenaltyCost, balance.coldChainCost, balance.ledgerFee]
    .filter((value) => value != null)
    .reduce((total, value) => total + Number(value), 0);
  const hasAdditionalCost = balance.delayPenaltyCost != null || balance.coldChainCost != null || balance.ledgerFee != null;
  setText("balanceAdditionalCost", hasAdditionalCost ? formatCurrency(additionalCost) : noData);
}

function updateRouteSummary(payload) {
  state.routeSummaryPayload = payload;
  const data = payload.data || {};

  setText("summaryRoutes", formatNumber(data.routePlans));
  setText("totalCost", formatCurrency(data.totalCost));
  setText("delayedRoutes", formatNumber(data.delayedRoutes));
  setText("deviatedRoutes", formatNumber(data.deviatedRoutes));
}

function updateOutbox(payload) {
  state.outboxPayload = payload;
  const data = payload.data || {};

  const pending = Number(data.pending || 0);
  const published = Number(data.published || 0);
  const retry = Number(data.retry || 0);
  const failed = Number(data.failed || 0);
  const skipped = Number(data.skipped || 0);
  const total = pending + published + retry + failed + skipped;
  const percent = (value) => `${total ? Math.max((value / total) * 100, value ? 3 : 0) : 0}%`;

  setText("outboxPending", formatNumber(pending));
  setText("outboxTotal", t("common.events", { count: formatNumber(total) }));
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
  state.nexusSettlementPayload = payload;
  if (payload.error) {
    setText("nexusSettlementStatus", statusLabel("ERROR"));
    setText("nexusSettlementMode", payload.error.message);
    return;
  }

  const data = payload.data || {};
  const totalRows = Number(data.totalSettlements || 0);
  const sent = Number(data.sent || 0);
  const dryRun = Number(data.dryRun || 0);
  const retry = Number(data.retry || 0);
  const failed = Number(data.failed || 0);
  const status = failed ? "FAILED" : retry ? "RETRY" : sent ? "SENT" : dryRun ? "DRY_RUN" : "READY";

  setText("nexusSettlementRows", formatNumber(totalRows));
  setText("nexusSettlementSent", `${formatNumber(sent)} / ${formatNumber(dryRun)}`);
  setText("nexusManufacturingCost", formatCurrency(data.totalManufacturingImpactCost));
  setText("nexusSettlementStatus", statusLabel(status));
  setText("nexusSettlementMode", t("settlement.outboxBasis"));
}

function renderLastPublish(data) {
  setText("lastPublish", t("activity.publishSummary", {
    status: statusLabel(data.status || "-"),
    requested: formatNumber(data.requestedCount),
    published: formatNumber(data.publishedCount),
    retry: formatNumber(data.retriedCount),
    skipped: formatNumber(data.skippedCount),
  }));
}

function renderLastNexusSettlement(data) {
  setText("lastNexusSettlement", t("activity.nexusSettlementSummary", {
    sent: formatNumber(data.sentCount || 0),
    dryRun: formatNumber(data.dryRunCount || 0),
    skipped: formatNumber(data.skippedCount || 0),
  }));
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
    logActivity("activity.refresh", t("activity.refreshLoaded"));
  } catch (error) {
    logActivity("activity.refreshFailed", error.message);
  } finally {
    $("refreshButton").disabled = false;
  }
}

async function publishOutbox() {
  $("publishButton").disabled = true;
  try {
    const result = await request("/api/outbox/publish", { method: "POST" });
    const data = result.data || {};
    state.lastPublishPayload = data;
    renderLastPublish(data);
    logActivity("activity.publish", statusLabel(data.status) || t("activity.completed"));
    await refresh();
  } catch (error) {
    logActivity("activity.publishFailed", error.message);
  } finally {
    $("publishButton").disabled = false;
  }
}

async function runNexusSettlement() {
  $("settlementButton").disabled = true;
  try {
    const result = await request("/api/settlements/nexus-daily/run", { method: "POST" });
    const data = result.data || {};
    state.lastNexusSettlementPayload = data;
    renderLastNexusSettlement(data);
    logActivity("activity.nexusSettlement", t("activity.nexusSettlementDetail", {
      date: data.settlementDate || "-",
      factories: formatNumber(data.requestedFactoryCount),
    }));
    await refresh();
  } catch (error) {
    logActivity("activity.nexusSettlementFailed", error.message);
  } finally {
    $("settlementButton").disabled = false;
  }
}

document.addEventListener("DOMContentLoaded", () => {
  initLanguageSelector();
  $("refreshButton").addEventListener("click", refresh);
  $("publishButton").addEventListener("click", publishOutbox);
  $("settlementButton").addEventListener("click", runNexusSettlement);
  $("routeFilter").addEventListener("submit", (event) => {
    event.preventDefault();
    refresh();
  });

  refresh();
  window.setInterval(refresh, 30000);
});
