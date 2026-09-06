(function () {
  const root = document.querySelector("[data-dashboard]");
  if (!root) return;
  const body = root.querySelector("[data-links-body]"),
    empty = root.querySelector("[data-links-empty]"),
    alert = root.querySelector("[data-dashboard-alert]");
  if (!SmartLink.token()) {
    window.location.href = "/auth";
    return;
  }
  const escape = (value) =>
    String(value == null ? "" : value).replace(
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
  const shortUrl = (hash) =>
    new URL("/" + encodeURIComponent(hash), window.location.origin).toString();
  const statusClass = (status) =>
    "status-" + String(status || "unverified").toLowerCase();
  const statusLabel = (status) =>
    String(status || "UNVERIFIED").replaceAll("_", " ");
  const updateMetrics = (links) => {
    const safe = links.filter((x) => x.status === "SAFE").length;
    const review = links.filter((x) =>
      ["SUSPICIOUS", "UNVERIFIED", "PENDING_REVERIFICATION"].includes(x.status),
    ).length;
    const clicks = links.reduce((sum, x) => sum + (Number(x.clickCnt) || 0), 0);
    root.querySelector("[data-total-links]").textContent = links.length;
    root.querySelector("[data-safe-links]").textContent = safe;
    root.querySelector("[data-review-links]").textContent = review;
    root.querySelector("[data-total-clicks]").textContent =
      clicks.toLocaleString();
  };
  const render = (links) => {
    updateMetrics(links);
    empty.classList.toggle("hidden", links.length > 0);
    root
      .querySelector(".table-wrap")
      .classList.toggle("hidden", links.length === 0);
    body.innerHTML = links
      .map((link) => {
        const original = escape(link.actualUrl),
          short = escape(shortUrl(link.hashedKey)),
          status = escape(statusLabel(link.status));
        return (
          '<tr><td title="' +
          original +
          '">' +
          original +
          '</td><td><a class="short-link" href="' +
          short +
          '" target="_blank" rel="noopener">' +
          short +
          '</a></td><td><span class="status-badge ' +
          statusClass(link.status) +
          '">' +
          status +
          "</span></td><td>" +
          (Number(link.clickCnt) || 0) +
          "</td><td>" +
          SmartLink.formatDate(link.creationTime) +
          '</td><td><div class="row-actions"><a class="icon-button action-link" title="View analytics" href="/analytics?shortHash=' +
          encodeURIComponent(link.hashedKey) +
          '">◌</a><button class="icon-button" title="View details" data-detail="' +
          escape(link.hashedKey) +
          '">↗</button><button class="icon-button" title="Copy link" data-copy="' +
          short +
          '">▣</button><button class="icon-button danger-button" title="Delete link" data-delete="' +
          escape(link.hashedKey) +
          '">×</button></div></td></tr>'
        );
      })
      .join("");
    body
      .querySelectorAll("[data-detail]")
      .forEach((button) =>
        button.addEventListener("click", () =>
          showDetails(button.dataset.detail),
        ),
      );
    body
      .querySelectorAll("[data-copy]")
      .forEach((button) =>
        button.addEventListener("click", () =>
          copy(button.dataset.copy, button),
        ),
      );
    body
      .querySelectorAll("[data-delete]")
      .forEach((button) =>
        button.addEventListener("click", () =>
          deleteLink(button.dataset.delete),
        ),
      );
  };
  const deleteLink = async (hash) => {
    if (!window.confirm("Delete this short link? This cannot be undone."))
      return;
    try {
      const response = await fetch("/link/" + encodeURIComponent(hash), {
        method: "DELETE",
        headers: SmartLink.authHeaders(),
      });
      if (!response.ok) throw new Error(await SmartLink.errorMessage(response));
      SmartLink.showAlert(alert, "Link deleted.", "success");
      loadLinks();
    } catch (error) {
      SmartLink.showAlert(alert, error.message, "error");
    }
  };
  const loadLinks = async () => {
    SmartLink.hideAlert(alert);
    try {
      const response = await fetch("/link/", {
        headers: SmartLink.authHeaders(),
      });
      if (response.status === 401) {
        SmartLink.clearAuth();
        window.location.href = "/auth";
        return;
      }
      if (!response.ok) throw new Error(await SmartLink.errorMessage(response));
      const data = await response.json();
      render(data.data?.links || []);
    } catch (error) {
      SmartLink.showAlert(alert, error.message, "error");
    }
  };
  const copy = async (value, button) => {
    try {
      await navigator.clipboard.writeText(value);
      const old = button.textContent;
      button.textContent = "✓";
      setTimeout(() => (button.textContent = old), 1300);
    } catch (e) {
      SmartLink.showAlert(
        alert,
        "Copy was not available. Please copy the link manually.",
        "error",
      );
    }
  };
  const showDetails = async (hash) => {
    const panel = root.querySelector("[data-detail-panel]"),
      content = root.querySelector("[data-detail-content]");
    panel.classList.remove("hidden");
    content.innerHTML = '<p class="section-subtitle">Loading details…</p>';
    try {
      const [linkResponse, scanResponse] = await Promise.all([
        fetch("/link/" + encodeURIComponent(hash), {
          headers: SmartLink.authHeaders(),
        }),
        fetch("/link/debug/verdict/" + encodeURIComponent(hash), {
          headers: SmartLink.authHeaders(),
        }),
      ]);
      if (!linkResponse.ok)
        throw new Error(await SmartLink.errorMessage(linkResponse));
      const link = (await linkResponse.json()).data,
        scan = scanResponse.ok ? (await scanResponse.json()).data : null,
        original = escape(link.actualUrl),
        short = escape(shortUrl(link.hashedKey));
      content.innerHTML =
        '<div class="detail-item"><small>Original URL</small><strong>' +
        original +
        '</strong></div><div class="detail-item"><small>Short URL</small><a class="short-link" href="' +
        short +
        '" target="_blank">' +
        short +
        '</a></div><div class="detail-item"><small>Status</small><span class="status-badge ' +
        statusClass(link.status) +
        '">' +
        escape(statusLabel(link.status)) +
        '</span></div><div class="detail-item"><small>Clicks</small><strong>' +
        (Number(link.clickCnt) || 0) +
        '</strong></div><div class="detail-item"><small>Created</small><strong>' +
        SmartLink.formatDate(link.creationTime) +
        "</strong></div>" +
        (scan
          ? '<div class="detail-item"><small>Scan engines</small><strong>' +
            (scan.totalEngines || 0) +
            "</strong></div>"
          : "");
      panel.scrollIntoView({ behavior: "smooth", block: "center" });
    } catch (error) {
      content.innerHTML =
        '<p class="section-subtitle">' + escape(error.message) + "</p>";
    }
  };
  root
    .querySelector("[data-create-form]")
    .addEventListener("submit", async (event) => {
      event.preventDefault();
      const form = event.currentTarget,
        button = form.querySelector("[data-submit]"),
        input = form.elements.actualUrl,
        result = root.querySelector("[data-create-result]"),
        link = root.querySelector("[data-created-url]");
      button.disabled = true;
      button.textContent = "Shortening…";
      SmartLink.hideAlert(alert);
      result.classList.add("hidden");
      try {
        const response = await fetch("/link/create", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            ...SmartLink.authHeaders(),
          },
          body: JSON.stringify({ actualUrl: input.value.trim() }),
        });
        if (response.status === 401) {
          SmartLink.clearAuth();
          window.location.href = "/auth";
          return;
        }
        if (!response.ok)
          throw new Error(await SmartLink.errorMessage(response));
        const data = (await response.json()).data;
        link.href = data.shortUrl;
        link.textContent = data.shortUrl;
        result.classList.remove("hidden");
        input.value = "";
        SmartLink.showAlert(
          alert,
          "Short link created. Safety processing may take a moment before it appears in your list.",
          "success",
        );
        setTimeout(loadLinks, 1200);
        setTimeout(loadLinks, 4000);
      } catch (error) {
        SmartLink.showAlert(alert, error.message, "error");
      }
      button.disabled = false;
      button.innerHTML = "Shorten URL <span>→</span>";
    });
  root
    .querySelector("[data-copy-created]")
    .addEventListener("click", (event) =>
      copy(root.querySelector("[data-created-url]").href, event.currentTarget),
    );
  root
    .querySelector("[data-refresh-links]")
    .addEventListener("click", loadLinks);
  root
    .querySelector("[data-close-detail]")
    .addEventListener("click", () =>
      root.querySelector("[data-detail-panel]").classList.add("hidden"),
    );
  try {
    const part = SmartLink.token().split(".")[1],
      payload = JSON.parse(atob(part.replace(/-/g, "+").replace(/_/g, "/")));
    root.querySelector("[data-dashboard-user]").textContent =
      payload.name || "there";
  } catch (e) {}
  loadLinks();
})();
