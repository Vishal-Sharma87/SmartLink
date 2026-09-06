(function () {
  const root = document.querySelector("[data-analytics]");
  if (!root) return;
  const select = root.querySelector("[data-analytics-select]"),
    alert = root.querySelector("[data-analytics-alert]"),
    content = root.querySelector("[data-analytics-content]"),
    empty = root.querySelector("[data-analytics-empty]");
  if (!SmartLink.token()) {
    window.location.href = "/auth";
    return;
  }
  const escape = (value) =>
    String(value == null ? "—" : value).replace(
      /[&<>"']/g,
      (char) =>
        ({
          "&": "&amp;",
          "<": "&lt;",
          ">": "&gt;",
          '"': "&quot;",
          "'": "&#039;",
        })[char],
    );
  const shortUrl = (hash) => window.location.origin + "/" + hash;
  const loadLinks = async () => {
    try {
      const response = await fetch("/link/", {
        headers: SmartLink.authHeaders(),
      });
      if (!response.ok) throw new Error(await SmartLink.errorMessage(response));
      const links = (await response.json()).data?.links || [];
      select.innerHTML = links.length
        ? links
            .map(
              (link) =>
                `<option value="${escape(link.hashedKey)}">${escape(link.hashedKey)} · ${escape(link.actualUrl)}</option>`,
            )
            .join("")
        : `<option value="">No links found</option>`;
      const requested = new URLSearchParams(window.location.search).get(
        "shortHash",
      );
      const selected =
        links.find((link) => link.hashedKey === requested) || links[0];
      if (selected) {
        select.value = selected.hashedKey;
        loadAnalytics(selected.hashedKey);
      } else empty.classList.remove("hidden");
    } catch (error) {
      SmartLink.showAlert(alert, error.message, "error");
    }
  };
  const renderDistribution = (key, values, total) => {
    const node = root.querySelector(`[data-distribution="${key}"]`);
    node.innerHTML =
      values && values.length
        ? values
            .map(
              (item) =>
                `<div class="distribution-row"><div><span>${escape(item.label)}</span><b>${item.count.toLocaleString()} <small>${Math.round(item.percentage)}%</small></b></div><div class="bar"><i style="width:${Math.max(3, item.percentage)}%"></i></div></div>`,
            )
            .join("")
        : `<p class="section-subtitle analytics-placeholder">No data available yet.</p>`;
  };
  const loadAnalytics = async (hash) => {
    if (!hash) return;
    content.classList.add("hidden");
    empty.classList.add("hidden");
    SmartLink.showAlert(alert, "Loading analytics…", "info");
    try {
      const response = await fetch(
        "/analytics/link?shortHash=" + encodeURIComponent(hash),
        { headers: SmartLink.authHeaders() },
      );
      if (!response.ok) throw new Error(await SmartLink.errorMessage(response));
      const data = (await response.json()).data;
      SmartLink.hideAlert(alert);
      content.classList.remove("hidden");
      root.querySelector("[data-analytics-hash]").textContent = data.shortHash;
      root.querySelector("[data-analytics-url]").textContent = data.actualUrl;
      const open = root.querySelector("[data-analytics-open]");
      open.href = shortUrl(data.shortHash);
      root.querySelector("[data-total-clicks]").textContent = (
        data.totalClicks || 0
      ).toLocaleString();
      root.querySelector("[data-unique-countries]").textContent =
        data.uniqueCountries || 0;
      root.querySelector("[data-top-country]").textContent = data.topCountry
        ? data.topCountry.label
        : "—";
      root.querySelector("[data-top-country-count]").textContent =
        data.topCountry
          ? `${data.topCountry.count} recorded clicks`
          : "No country data yet";
      ["devices", "browsers", "operatingSystems", "countries"].forEach((key) =>
        renderDistribution(key, data[key], data.totalClicks),
      );
      const rows = root.querySelector("[data-recent-clicks]");
      rows.innerHTML = (data.recentClicks || [])
        .map(
          (click) =>
            `<tr><td>${escape(click.time ? new Date(click.time).toLocaleString() : "—")}</td><td>${escape(click.country || click.continent)}</td><td>${escape(click.device)}</td><td>${escape(click.browser)}</td><td>${escape(click.operatingSystem)}</td><td>${escape(click.timeZone)}</td></tr>`,
        )
        .join("");
      root
        .querySelector("[data-recent-empty]")
        .classList.toggle("hidden", !!(data.recentClicks || []).length);
    } catch (error) {
      SmartLink.showAlert(alert, error.message, "error");
      content.classList.add("hidden");
    }
  };
  select.addEventListener("change", () => loadAnalytics(select.value));
  loadLinks();
})();
