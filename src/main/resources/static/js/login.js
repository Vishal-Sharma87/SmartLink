(function () {
  const form = document.querySelector('[data-login-form]');
  const alert = document.querySelector('[data-alert]');
  const tokenKey = 'smartlink_access_token';
  const expiredMessageKey = 'smartlink_auth_message';
  if (!form) return;

  const showError = (message) => {
    if (!alert) return;
    alert.querySelector('.notice-title').textContent = 'Could not log in';
    alert.querySelector('span').textContent = message;
    alert.dataset.state = 'error';
    alert.classList.remove('is-hidden');
  };

  const hideError = () => {
    if (alert) alert.classList.add('is-hidden');
  };

  const expiredMessage = sessionStorage.getItem(expiredMessageKey);
  if (expiredMessage) {
    sessionStorage.removeItem(expiredMessageKey);
    showError(expiredMessage);
  }

  const apiErrorMessage = async (response) => {
    try {
      const body = await response.json();
      return body.message || 'The request could not be completed. Please try again.';
    } catch (error) {
      return 'The request could not be completed. Please try again.';
    }
  };

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideError();
    if (!form.reportValidity()) return;

    const button = form.querySelector('button[type="submit"]');
    button.disabled = true;
    button.textContent = 'Logging in…';

    try {
      const payload = Object.fromEntries(new FormData(form).entries());
      const response = await fetch('/auth/login', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (!response.ok) throw new Error(await apiErrorMessage(response));

      const body = await response.json();
      const jwtToken = body.data && body.data.jwtToken;
      if (!jwtToken) throw new Error('Login completed, but authentication could not be established.');

      // The backend sets the refresh_token as an HttpOnly cookie. JavaScript never reads or stores it.
      sessionStorage.setItem(tokenKey, jwtToken);
      window.location.assign('/dashboard');
    } catch (error) {
      const message = error instanceof TypeError
        ? 'The login request could not reach SmartLink. Please try again.'
        : (error.message || 'The request could not be completed. Please try again.');
      showError(message);
      button.disabled = false;
      button.textContent = 'Log in';
    }
  });
})();
