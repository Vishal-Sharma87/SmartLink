(function () {
  const form = document.querySelector("[data-signup-verify-form]");
  if (!form) return;
  const alert = document.querySelector("[data-signup-verify-alert]"),
    resend = document.querySelector("[data-resend-signup]"),
    email = document.querySelector("[data-signup-email]");
  const setBusy = (busy) => {
    const button = form.querySelector("[data-submit]");
    button.disabled = busy;
    button.textContent = busy ? "Verifying…" : "Verify email →";
  };
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    setBusy(true);
    try {
      const response = await fetch("/auth/signup/verify", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(Object.fromEntries(new FormData(form).entries())),
      });
      if (!response.ok) throw new Error(await SmartLink.errorMessage(response));
      window.location.href = "/auth?mode=login&verified=true";
    } catch (error) {
      SmartLink.showAlert(alert, error.message, "error");
      setBusy(false);
    }
  });
  resend.addEventListener("click", async () => {
    resend.disabled = true;
    try {
      const response = await fetch("/verify/generate-otp", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: email.textContent.trim() }),
      });
      if (!response.ok) throw new Error(await SmartLink.errorMessage(response));
      SmartLink.showAlert(
        alert,
        "A new verification code was sent.",
        "success",
      );
    } catch (error) {
      SmartLink.showAlert(alert, error.message, "error");
    }
    resend.disabled = false;
  });
})();
