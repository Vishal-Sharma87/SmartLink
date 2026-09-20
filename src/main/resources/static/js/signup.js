(function () {
  const tokenKey = 'smartlink_access_token';
  const alert = document.querySelector('[data-alert]');
  const showAlert = (message, state) => {
    if (!alert) return;
    alert.querySelector('.notice-title').textContent = state === 'error' ? 'Could not continue' : 'Signup update';
    alert.querySelector('span').textContent = message;
    alert.dataset.state = state;
    alert.classList.remove('is-hidden');
  };
  const hideAlert = () => alert && alert.classList.add('is-hidden');
  const responseMessage = async (response) => {
    try {
      const body = await response.json();
      return body.message || 'The request could not be completed. Please try again.';
    } catch (error) {
      return 'The request could not be completed. Please try again.';
    }
  };
  const setBusy = (form, busy, label) => {
    const button = form.querySelector('button[type="submit"]');
    if (!button) return;
    button.disabled = busy;
    button.textContent = busy ? label : 'Continue to verification';
  };
  const form = document.querySelector('[data-signup-form]');
  if (form) {
    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      hideAlert();
      if (!form.reportValidity()) return;
      setBusy(form, true, 'Sending code…');
      const payload = Object.fromEntries(new FormData(form).entries());
      try {
        const response = await fetch('/auth/signup/initiate', {
          method: 'POST',
          credentials: 'same-origin',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
        if (!response.ok) throw new Error(await responseMessage(response));
        window.location.assign('/signup/verify');
      } catch (error) {
        showAlert(error.message || 'The request could not be completed. Please try again.', 'error');
        setBusy(form, false);
      }
    });
  }

  const verificationForm = document.querySelector('[data-signup-verification-form]');
  const otp = document.querySelector('[data-otp-input]');
  if (otp) {
    otp.addEventListener('input', () => {
      otp.value = otp.value.replace(/\D/g, '').slice(0, 6);
    });
    otp.focus();
  }
  if (verificationForm) {
    verificationForm.addEventListener('submit', async (event) => {
      event.preventDefault();
      hideAlert();
      if (!verificationForm.reportValidity()) return;
      const button = verificationForm.querySelector('button[type="submit"]');
      button.disabled = true;
      button.textContent = 'Verifying…';
      try {
        const response = await fetch('/auth/signup/verify', {
          method: 'POST',
          credentials: 'same-origin',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ otp: otp.value })
        });
        if (!response.ok) throw new Error(await responseMessage(response));
        const body = await response.json();
        const jwtToken = body.data && body.data.jwtToken;
        if (!jwtToken) throw new Error('Signup completed, but authentication could not be established.');
        sessionStorage.setItem(tokenKey, jwtToken);
        window.location.assign('/dashboard');
      } catch (error) {
        showAlert(error.message || 'The request could not be completed. Please try again.', 'error');
        button.disabled = false;
        button.textContent = 'Verify account';
      }
    });
  }
})();
