(function () {
  const tokenKey = "smartlink_token";
  const token = () => sessionStorage.getItem(tokenKey);
  const userFromToken = () => {
    try {
      const part = (token() || "").split(".")[1];
      if (!part) return "";
      const payload = JSON.parse(
        atob(part.replace(/-/g, "+").replace(/_/g, "/")),
      );
      return payload.name || payload.sub || "";
    } catch (e) {
      return "";
    }
  };
  window.SmartLink = {
    token,
    authHeaders: () => (token() ? { Authorization: "Bearer " + token() } : {}),
    clearAuth: () => sessionStorage.removeItem(tokenKey),
    saveAuth: (value) => sessionStorage.setItem(tokenKey, value),
    showAlert: (element, message, type) => {
      if (!element) return;
      element.textContent = message;
      element.className = "alert " + (type || "error");
    },
    hideAlert: (element) => {
      if (element) element.className = "alert hidden";
    },
    errorMessage: async (response) => {
      try {
        const body = await response.json();
        return body.message || "Something went wrong. Please try again.";
      } catch (e) {
        return response.status === 401
          ? "Your session has expired. Please log in again."
          : "Something went wrong. Please try again.";
      }
    },
    formatDate: (value) =>
      value
        ? new Date(value).toLocaleDateString(undefined, {
            year: "numeric",
            month: "short",
            day: "numeric",
          })
        : "—",
  };
  document.addEventListener("DOMContentLoaded", () => {
    const user = userFromToken();
    document.querySelectorAll("[data-user-label]").forEach((el) => {
      el.textContent = user || "";
    });
    document
      .querySelectorAll("[data-logout]")
      .forEach((btn) => btn.classList.toggle("hidden", !user));
    document
      .querySelectorAll("[data-guest-link]")
      .forEach((el) => el.classList.toggle("hidden", !!user));
    document.querySelectorAll("[data-logout]").forEach((btn) =>
      btn.addEventListener("click", () => {
        SmartLink.clearAuth();
        window.location.href = "/";
      }),
    );
    const menu = document.querySelector("[data-menu-toggle]"),
      links = document.querySelector("#navLinks");
    if (menu && links)
      menu.addEventListener("click", () => links.classList.toggle("open"));
  });
})();
