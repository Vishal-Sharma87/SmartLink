(function () {
  const form = document.querySelector('[data-shorten-form]');
  if (!form) return;

  const alert = document.querySelector('[data-alert]');
  const input = form.querySelector('[data-original-url]');
  const submit = form.querySelector('[data-shorten-submit]');
  const result = document.querySelector('[data-creation-result]');
  const shortUrl = document.querySelector('[data-short-url]');
  const creationMessage = document.querySelector('[data-creation-message]');
  const creationStatus = document.querySelector('[data-creation-status]');
  const copyButton = document.querySelector('[data-copy-short-url]');
  const closeResultButton = document.querySelector('[data-close-creation-result]');
  let requestInFlight = false;
  let resultVisible = false;

  const showAlert = (message, state) => {
    if (!alert) return;
    alert.querySelector('.notice-title').textContent = state === 'error' ? 'Could not create link' : 'Link update';
    alert.querySelector('span').textContent = message;
    alert.dataset.state = state;
    alert.classList.remove('is-hidden');
  };

  const hideAlert = () => alert && alert.classList.add('is-hidden');

  const apiErrorMessage = async (response) => {
    try {
      const body = await response.json();
      return body.message || 'The link could not be created. Please try again.';
    } catch (error) {
      return 'The link could not be created. Please try again.';
    }
  };

  const setBusy = (busy) => {
    submit.disabled = busy;
    submit.textContent = busy ? 'Creating…' : 'Create short link';
  };

  const showResult = (data) => {
    if (!data || !data.shortUrl || !result) {
      throw new Error('The creation response did not include a short URL.');
    }
    const shortCode = SmartLinkDisplay.shortCodeFromUrl(data.shortUrl);
    if (!shortCode) throw new Error('The creation response did not include a valid short code.');
    shortUrl.href = SmartLinkDisplay.buildShortUrl(shortCode);
    shortUrl.textContent = SmartLinkDisplay.displayShortUrl(shortCode);
    const backendMessage = data.message || 'Your SmartLink has been queued for safety processing.';
    creationMessage.textContent = data.status === 'PENDING' && !/activat|available after|processing completes/i.test(backendMessage)
      ? backendMessage + ' It will be activated after safety processing completes.'
      : backendMessage;
    creationStatus.textContent = data.status || '';
    creationStatus.dataset.status = data.status || '';
    form.classList.add('is-hidden');
    result.classList.remove('is-hidden');
    resultVisible = true;
    closeResultButton.focus();
  };

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    if (requestInFlight || resultVisible) return;
    hideAlert();
    if (!form.reportValidity()) return;
    requestInFlight = true;
    setBusy(true);
    try {
      const response = await SmartLinkAuth.request('/link/create', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ originalUrl: input.value.trim() })
      });
      if (!response.ok) throw new Error(await apiErrorMessage(response));
      const body = await response.json();
      showResult(body && body.data);
      showAlert('Your short link was queued successfully.', 'success');
    } catch (error) {
      showAlert(error.message || 'The link could not be created. Please try again.', 'error');
    } finally {
      requestInFlight = false;
      setBusy(false);
    }
  });

  const closeResult = () => {
    result.classList.add('is-hidden');
    form.classList.remove('is-hidden');
    form.reset();
    hideAlert();
    resultVisible = false;
    if (copyButton) copyButton.textContent = 'Copy link';
    input.focus();
  };

  closeResultButton.addEventListener('click', closeResult);

  result.addEventListener('click', (event) => {
    if (event.target === result) closeResult();
  });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && resultVisible) closeResult();
  });

  copyButton.addEventListener('click', async () => {
    const value = shortUrl && shortUrl.href;
    if (!value) return;
    if (!navigator.clipboard) {
      showAlert('Copy is unavailable here. Select the short URL and copy it manually.', 'error');
      return;
    }
    try {
      await navigator.clipboard.writeText(value);
      copyButton.textContent = 'Copied';
      window.setTimeout(() => { copyButton.textContent = 'Copy link'; }, 1400);
    } catch (error) {
      showAlert('Copy is unavailable here. Select the short URL and copy it manually.', 'error');
    }
  });
})();
