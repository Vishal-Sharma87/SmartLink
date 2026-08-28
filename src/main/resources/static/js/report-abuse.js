(function () {
  const app = document.querySelector("[data-report-app]");
  if (!app) return;
  const form = app.querySelector("[data-report-form]"),
    alert = app.querySelector("[data-report-alert]"),
    stepOne = app.querySelector('[data-report-step="1"]'),
    stepTwo = app.querySelector('[data-report-step="2"]');
  const values = () => {
    const data = Object.fromEntries(new FormData(form).entries());
    data.cause = Array.from(
      form.querySelectorAll('input[name="cause"]:checked'),
    ).map((input) => input.value);
    return data;
  };
  const setStep = (step) => {
    stepOne.classList.toggle("hidden", step !== 1);
    stepTwo.classList.toggle("hidden", step !== 2);
    app
      .querySelectorAll("[data-step-indicator]")
      .forEach((el) =>
        el.classList.toggle(
          "active",
          Number(el.dataset.stepIndicator) === step,
        ),
      );
    SmartLink.hideAlert(alert);
  };
  app
    .querySelector("[data-continue-report]")
    .addEventListener("click", async () => {
      if (!values().cause.length) {
        SmartLink.showAlert(
          alert,
          "Select at least one report cause.",
          "error",
        );
        return;
      }
      if (
        !stepOne.querySelector("input:invalid,select:invalid,textarea:invalid")
      ) {
        const data = values(),
          button = app.querySelector("[data-continue-report]");
        button.disabled = true;
        button.textContent = "Sending code…";
        try {
          const response = await fetch("/verify/generate-otp", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: data.reporterEmail }),
          });
          if (!response.ok)
            throw new Error(await SmartLink.errorMessage(response));
          setStep(2);
        } catch (error) {
          SmartLink.showAlert(alert, error.message, "error");
        }
        button.disabled = false;
        button.innerHTML = "Continue to verification <span>→</span>";
      } else form.reportValidity();
    });
  app
    .querySelector("[data-back-report]")
    .addEventListener("click", () => setStep(1));
  form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const button = app.querySelector('[data-report-step="2"] [data-submit]');
    button.disabled = true;
    button.textContent = "Submitting…";
    try {
      const response = await fetch("/report-abuse", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(values()),
      });
      if (!response.ok) throw new Error(await SmartLink.errorMessage(response));
      form.classList.add("hidden");
      app.querySelector("[data-report-success]").classList.remove("hidden");
      app.querySelector(".stepper").classList.add("hidden");
    } catch (error) {
      SmartLink.showAlert(alert, error.message, "error");
      button.disabled = false;
      button.innerHTML = "Verify & submit report <span>→</span>";
    }
  });
})();
