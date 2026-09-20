(function () {
  const page = document.querySelector('[data-report-page]');
  if (!page) return;

  const alert = page.querySelector('[data-alert]');
  const detailsForm = page.querySelector('[data-report-details-form]');
  const otpStep = page.querySelector('[data-report-otp-step]');
  const submitForm = page.querySelector('[data-report-submit-form]');
  const success = page.querySelector('[data-report-success]');
  const email = page.querySelector('[data-reporter-email]');
  const emailDisplay = page.querySelector('[data-report-email-display]');
  const otp = page.querySelector('[data-report-otp]');
  const generateButton = page.querySelector('[data-generate-otp]');
  const submitButton = page.querySelector('[data-submit-report]');
  const resendButton = page.querySelector('[data-resend-otp]');
  const editButton = page.querySelector('[data-edit-report]');

  const showAlert = (title, message, state) => {
    alert.querySelector('.notice-title').textContent = title;
    alert.querySelector('span').textContent = message;
    alert.dataset.state = state;
    alert.classList.remove('is-hidden');
  };

  const hideAlert = () => alert.classList.add('is-hidden');

  const responseMessage = async (response, fallback) => {
    try {
      const body = await response.clone().json();
      return body && body.message ? body.message : fallback;
    } catch (error) {
      return fallback;
    }
  };

  const setBusy = (button, busy, idleText, busyText) => {
    button.disabled = busy;
    button.textContent = busy ? busyText : idleText;
  };

  const selectedCauses = () => Array.from(page.querySelectorAll('[data-report-cause]:checked'))
    .map((input) => input.value);

  const reportDetails = () => ({
    reporterName: page.querySelector('[data-reporter-name]').value.trim(),
    reporterEmail: email.value.trim(),
    linkToReport: page.querySelector('[data-report-link]').value.trim(),
    cause: selectedCauses(),
    description: page.querySelector('[data-report-description]').value.trim()
  });

  const validateCauses = () => {
    if (selectedCauses().length > 0) return true;
    showAlert('Choose a report cause', 'Select at least one reason for reporting this SmartLink.', 'error');
    return false;
  };

  const requestOtp = async () => {
    const response = await fetch('/verify/generate-otp', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ email: email.value.trim() })
    });
    if (!response.ok) {
      throw new Error(await responseMessage(response, 'The verification code could not be sent. Please try again.'));
    }
  };

  detailsForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();
    if (!detailsForm.reportValidity() || !validateCauses()) return;
    setBusy(generateButton, true, 'Send verification code', 'Sending…');
    try {
      await requestOtp();
      emailDisplay.textContent = email.value.trim();
      detailsForm.classList.add('is-hidden');
      otpStep.classList.remove('is-hidden');
      otp.focus();
      showAlert('Verification code sent', 'Check your email, then enter the six-digit code to submit the report.', 'success');
    } catch (error) {
      showAlert('Could not send verification code', error.message || 'The verification code could not be sent. Please try again.', 'error');
    } finally {
      setBusy(generateButton, false, 'Send verification code', 'Sending…');
    }
  });

  resendButton.addEventListener('click', async () => {
    hideAlert();
    setBusy(resendButton, true, 'Send code again', 'Sending…');
    try {
      await requestOtp();
      otp.value = '';
      otp.focus();
      showAlert('Verification code sent', 'A new code was sent to your email.', 'success');
    } catch (error) {
      showAlert('Could not send verification code', error.message || 'The verification code could not be sent. Please try again.', 'error');
    } finally {
      setBusy(resendButton, false, 'Send code again', 'Sending…');
    }
  });

  submitForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();
    if (!submitForm.reportValidity()) return;
    const details = reportDetails();
    setBusy(submitButton, true, 'Submit report', 'Submitting…');
    resendButton.disabled = true;
    editButton.disabled = true;
    try {
      const response = await fetch('/report-abuse/', {
        method: 'POST',
        credentials: 'same-origin',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
        body: JSON.stringify({ ...details, otp: otp.value.trim() })
      });
      if (!response.ok) {
        throw new Error(await responseMessage(response, 'The report could not be submitted. Please try again.'));
      }
      const body = await response.json();
      if (!body || typeof body.data !== 'string') {
        throw new Error('The report response was incomplete. Please try again.');
      }
      otpStep.classList.add('is-hidden');
      success.querySelector('[data-report-success-message]').textContent = body.data;
      success.classList.remove('is-hidden');
    } catch (error) {
      resendButton.disabled = false;
      editButton.disabled = false;
      showAlert('Could not submit report', error.message || 'The report could not be submitted. Please try again.', 'error');
    } finally {
      setBusy(submitButton, false, 'Submit report', 'Submitting…');
    }
  });

  editButton.addEventListener('click', () => {
    hideAlert();
    otpStep.classList.add('is-hidden');
    detailsForm.classList.remove('is-hidden');
    otp.value = '';
    email.focus();
  });
})();
