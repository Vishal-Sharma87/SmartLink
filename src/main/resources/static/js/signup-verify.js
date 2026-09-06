
(function () {
  const form = document.querySelector("[data-signup-verify-form]");
  if (!form) return;

  const alert = document.querySelector("[data-signup-verify-alert]");
  const resend = document.querySelector("[data-resend-signup]");
  const email = document.querySelector("[data-signup-email]");

  const setBusy = (busy) => {
    const button = form.querySelector("[data-submit]");

    if (!button) return;

    button.disabled = busy;
    button.textContent = busy ? "Verifying…" : "Verify email →";
  };

  form.addEventListener("submit", async (event) => {
    event.preventDefault();

    SmartLink.hideAlert(alert);
    setBusy(true);

    try {
      const response = await fetch("/auth/signup/verify", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(
          Object.fromEntries(new FormData(form).entries()),
        ),
      });

      if (!response.ok) {
        throw new Error(await SmartLink.errorMessage(response));
      }

      const body = await response.json();

      SmartLink.saveAuth(body.data.token);

      window.location.href = "/dashboard";
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
        body: JSON.stringify({
          email: email.textContent.trim(),
        }),
      });

      if (!response.ok) {
        throw new Error(await SmartLink.errorMessage(response));
      }

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
})()