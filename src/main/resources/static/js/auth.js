(function () {
  const $ = (selector) => document.querySelector(selector);
  const loginForm = $("[data-login-form]"),
    signupForm = $("[data-signup-form]"),
    alert = $("[data-auth-alert]");
  if (!loginForm || !signupForm) return;
  const setMode = (mode) => {
    document
      .querySelectorAll("[data-auth-tab]")
      .forEach((tab) =>
        tab.classList.toggle("active", tab.dataset.authTab === mode),
      );
    loginForm.classList.toggle("hidden", mode !== "login");
    signupForm.classList.toggle("hidden", mode !== "signup");
    SmartLink.hideAlert(alert);
  };
  const params = new URLSearchParams(window.location.search);
  setMode(params.get("mode") === "signup" ? "signup" : "login");
  document
    .querySelectorAll("[data-auth-tab]")
    .forEach((tab) =>
      tab.addEventListener("click", () => setMode(tab.dataset.authTab)),
    );
  const formData = (form) => Object.fromEntries(new FormData(form).entries());
  const setBusy = (form, busy) => {
    const btn = form.querySelector("[data-submit]");
    if (btn) {
      btn.disabled = busy;
      btn.dataset.originalText ||= btn.innerHTML;
      if (busy) btn.textContent = "Working…";
      else btn.innerHTML = btn.dataset.originalText;
    }
  };
  loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    SmartLink.hideAlert(alert);
    setBusy(loginForm, true);
    try {
      const response = await fetch("/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData(loginForm)),
      });
      if (!response.ok) throw new Error(await SmartLink.errorMessage(response));
      const body = await response.json();
      SmartLink.saveAuth(body.data.token);
      window.location.href = "/dashboard";
    } catch (error) {
      SmartLink.showAlert(alert, error.message, "error");
      setBusy(loginForm, false);
    }
  });
  signupForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    SmartLink.hideAlert(alert);
    setBusy(signupForm, true);
    try {
      const response = await fetch("/auth/signup/initiate", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(formData(signupForm)),
      });
      if (!response.ok) throw new Error(await SmartLink.errorMessage(response));
      window.location.href = "/signup/verify";
    } catch (error) {
      SmartLink.showAlert(alert, error.message, "error");
      setBusy(signupForm, false);
    }
  });
})();
