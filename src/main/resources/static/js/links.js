(function () {
  const page = document.querySelector('[data-links-page]');
  if (!page) return;

  const alert = page.querySelector('[data-alert]');
  const loading = page.querySelector('[data-links-loading]');
  const empty = page.querySelector('[data-links-empty]');
  const list = page.querySelector('[data-links-list]');

  const escapeHtml = (value) => String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');

  const showAlert = (message, state) => {
    if (!alert) return;
    alert.querySelector('.notice-title').textContent = state === 'error' ? 'Could not load links' : 'Links updated';
    alert.querySelector('span').textContent = message;
    alert.dataset.state = state;
    alert.classList.remove('is-hidden');
  };

  const hideAlert = () => alert && alert.classList.add('is-hidden');

  const responseMessage = async (response, fallback) => {
    try {
      const body = await response.clone().json();
      return body && body.message ? body.message : fallback;
    } catch (error) {
      return fallback;
    }
  };

  const formatDate = (value) => {
    if (!value) return 'Date unavailable';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? 'Date unavailable' : date.toLocaleString();
  };

  const shortUrlFor = (shortCode) => SmartLinkDisplay.buildShortUrl(shortCode);

  const statusClass = (status) => String(status || '').toLowerCase().replace(/[^a-z0-9]+/g, '-');

  const linkCard = (link) => {
    const shortCode = String(link.shortCode || '');
    const shortUrl = shortUrlFor(shortCode);
    const displayedShortUrl = SmartLinkDisplay.displayShortUrl(shortCode);
    const status = String(link.status || '');
    return `<article class="link-card" data-short-code="${escapeHtml(shortCode)}">
      <div class="link-card-header">
        <div>
          <p class="kicker">ORIGINAL URL</p>
          <p class="link-original" title="${escapeHtml(link.originalUrl)}">${escapeHtml(link.originalUrl)}</p>
        </div>
        <span class="status-label status-${statusClass(status)}" data-status="${escapeHtml(status)}">${escapeHtml(status.replaceAll('_', ' '))}</span>
      </div>
      <div class="link-card-short-url">
        <p class="kicker">SMARTLINK</p>
        <a class="result-url" href="${escapeHtml(shortUrl)}" target="_blank" rel="noopener noreferrer">${escapeHtml(displayedShortUrl)}</a>
      </div>
      <dl class="link-card-meta">
        <div><dt>Clicks</dt><dd>${escapeHtml(link.clickCnt)}</dd></div>
        <div><dt>Reports</dt><dd>${escapeHtml(link.reportCnt)}</dd></div>
        <div><dt>Created</dt><dd>${escapeHtml(formatDate(link.createdAt))}</dd></div>
      </dl>
      <div class="link-card-actions">
        <button class="button button-secondary button-small" type="button" data-copy-link="${escapeHtml(shortUrl)}">Copy link</button>
        <a class="button button-ghost button-small" href="/analytics?shortCode=${encodeURIComponent(shortCode)}">View analytics</a>
        <button class="button button-ghost button-small danger-action" type="button" data-delete-link="${escapeHtml(shortCode)}">Delete</button>
      </div>
    </article>`;
  };

  const renderLinks = (links) => {
    loading.classList.add('is-hidden');
    if (!Array.isArray(links) || links.length === 0) {
      list.classList.add('is-hidden');
      empty.classList.remove('is-hidden');
      return;
    }
    empty.classList.add('is-hidden');
    list.innerHTML = links.map(linkCard).join('');
    list.classList.remove('is-hidden');
  };

  const loadLinks = async () => {
    hideAlert();
    loading.classList.remove('is-hidden');
    empty.classList.add('is-hidden');
    list.classList.add('is-hidden');
    try {
      const response = await SmartLinkAuth.request('/link/', { headers: { Accept: 'application/json' } });
      if (!response.ok) throw new Error(await responseMessage(response, 'Your links could not be loaded. Please try again.'));
      const body = await response.json();
      if (!body || !body.data || !Array.isArray(body.data.links)) {
        throw new Error('The links response was incomplete. Please try again.');
      }
      renderLinks(body.data.links);
    } catch (error) {
      loading.classList.add('is-hidden');
      showAlert(error.message || 'Your links could not be loaded. Please try again.', 'error');
    }
  };

  page.addEventListener('click', async (event) => {
    const copyButton = event.target.closest('[data-copy-link]');
    if (copyButton) {
      const value = copyButton.dataset.copyLink;
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
      return;
    }

    const deleteButton = event.target.closest('[data-delete-link]');
    if (!deleteButton) return;
    const shortCode = deleteButton.dataset.deleteLink;
    if (!shortCode || !window.confirm('Delete this SmartLink? This cannot be undone.')) return;
    deleteButton.disabled = true;
    deleteButton.textContent = 'Deleting…';
    try {
      const response = await SmartLinkAuth.request('/link/' + encodeURIComponent(shortCode), {
        method: 'DELETE',
        headers: { Accept: 'application/json' }
      });
      if (!response.ok) throw new Error(await responseMessage(response, 'The link could not be deleted. Please try again.'));
      await loadLinks();
      showAlert('The SmartLink was deleted.', 'success');
    } catch (error) {
      deleteButton.disabled = false;
      deleteButton.textContent = 'Delete';
      showAlert(error.message || 'The link could not be deleted. Please try again.', 'error');
    }
  });

  loadLinks();
})();
