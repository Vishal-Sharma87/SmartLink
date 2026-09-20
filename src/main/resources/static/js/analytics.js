(function () {
  const page = document.querySelector('[data-analytics-page]');
  if (!page) return;

  const alert = page.querySelector('[data-alert]');
  const loading = page.querySelector('[data-analytics-loading]');
  const content = page.querySelector('[data-analytics-content]');
  const summaries = page.querySelector('[data-analytics-summaries]');
  const clicks = page.querySelector('[data-analytics-clicks]');
  const tableWrap = page.querySelector('[data-analytics-table-wrap]');
  const empty = page.querySelector('[data-analytics-empty]');

  const escapeHtml = (value) => String(value ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');

  const showAlert = (title, message, state) => {
    alert.querySelector('.notice-title').textContent = title;
    alert.querySelector('span').textContent = message;
    alert.dataset.state = state;
    alert.classList.remove('is-hidden');
  };

  const formatDate = (value) => {
    if (!value) return '—';
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? '—' : date.toLocaleString();
  };

  const valueOrDash = (value) => value === null || value === undefined || value === '' ? '—' : value;

  const chartColors = ['#8b6cff', '#a994ff', '#6f53d9', '#c1b6ff', '#5540aa', '#d6d0ff', '#9478ff', '#43338b'];
  const donutCircumference = 2 * Math.PI * 42;

  const summaryCard = (title, items) => {
    if (!Array.isArray(items) || items.length === 0) return '';
    let offset = 0;
    const segments = items.map((item, index) => {
      const percentage = Number(item && item.percentage);
      const safePercentage = Number.isFinite(percentage) ? Math.min(Math.max(percentage, 0), 100) : 0;
      const length = donutCircumference * safePercentage / 100;
      const segment = `<circle cx="60" cy="60" r="42" fill="none" stroke="${chartColors[index % chartColors.length]}" stroke-width="14" stroke-dasharray="${length} ${donutCircumference - length}" stroke-dashoffset="-${offset}" transform="rotate(-90 60 60)"></circle>`;
      offset += length;
      return segment;
    }).join('');
    return `<section class="analytics-summary-card" aria-label="${escapeHtml(title)}">
      <div class="section-heading compact-heading"><p class="kicker">${escapeHtml(title)}</p><h2>${escapeHtml(title)} distribution</h2></div>
      <div class="analytics-summary-content">
        <svg class="analytics-donut" viewBox="0 0 120 120" role="img" aria-label="${escapeHtml(title)} distribution chart">
          <circle cx="60" cy="60" r="42" fill="none" stroke="var(--surface-3)" stroke-width="14"></circle>
          ${segments}
        </svg>
        <ul class="summary-list">${items.map((item, index) => {
        const percentage = Number(item && item.percentage);
        return `<li class="summary-row">
          <span class="summary-swatch" style="background: ${chartColors[index % chartColors.length]}" aria-hidden="true"></span>
          <span class="summary-row-label"><span>${escapeHtml(valueOrDash(item && item.label))}</span><span>${Number.isFinite(percentage) ? percentage.toFixed(1) : '0.0'}%</span></span>
          <span class="summary-count">${escapeHtml(valueOrDash(item && item.count))} clicks</span>
        </li>`;
      }).join('')}</ul>
      </div>
    </section>`;
  };

  const renderSummaries = (data) => {
    const groups = [
      ['Countries', data.countries],
      ['Continents', data.continents],
      ['Devices', data.devices],
      ['Browsers', data.browsers],
      ['Operating systems', data.operatingSystems]
    ];
    summaries.innerHTML = groups.map(([title, items]) => summaryCard(title, items)).join('');
  };

  const renderRecentClicks = (items) => {
    if (!Array.isArray(items) || items.length === 0) {
      tableWrap.classList.add('is-hidden');
      empty.classList.remove('is-hidden');
      return;
    }
    empty.classList.add('is-hidden');
    clicks.innerHTML = items.map((item) => `<tr>
      <td>${escapeHtml(formatDate(item && item.time))}</td>
      <td>${escapeHtml(valueOrDash(item && item.country))}</td>
      <td>${escapeHtml(valueOrDash(item && item.continent))}</td>
      <td>${escapeHtml(valueOrDash(item && item.device))}</td>
      <td>${escapeHtml(valueOrDash(item && item.browser))}</td>
      <td>${escapeHtml(valueOrDash(item && item.operatingSystem))}</td>
      <td>${escapeHtml(valueOrDash(item && item.timeZone))}</td>
    </tr>`).join('');
  };

  const renderAnalytics = (data) => {
    const shortCode = valueOrDash(data.shortCode);
    const shortUrl = shortCode === '—' ? '#' : SmartLinkDisplay.buildShortUrl(data.shortCode);
    page.querySelector('[data-analytics-short-code]').textContent = shortCode;
    page.querySelector('[data-analytics-short-url]').textContent = shortUrl === '#' ? '' : SmartLinkDisplay.displayShortUrl(data.shortCode);
    page.querySelector('[data-analytics-short-url]').href = shortUrl;
    page.querySelector('[data-analytics-original-url]').textContent = valueOrDash(data.originalUrl);
    page.querySelector('[data-metric-total-clicks]').textContent = valueOrDash(data.totalClicks);
    page.querySelector('[data-metric-unique-countries]').textContent = valueOrDash(data.uniqueCountries);
    page.querySelector('[data-metric-top-country]').textContent = data.topCountry
      ? `${valueOrDash(data.topCountry.label)} (${valueOrDash(data.topCountry.count)})`
      : '—';
    renderSummaries(data);
    renderRecentClicks(data.recentClicks);
    loading.classList.add('is-hidden');
    content.classList.remove('is-hidden');
  };

  const responseMessage = async (response, fallback) => {
    try {
      const body = await response.clone().json();
      return body && body.message ? body.message : fallback;
    } catch (error) {
      return fallback;
    }
  };

  const shortCode = new URLSearchParams(window.location.search).get('shortCode');
  if (!shortCode || !shortCode.trim()) {
    loading.classList.add('is-hidden');
    showAlert('Analytics unavailable', 'Select a SmartLink from My Links to view its analytics.', 'error');
    return;
  }

  SmartLinkAuth.request('/analytics/link?shortCode=' + encodeURIComponent(shortCode.trim()), {
    headers: { Accept: 'application/json' }
  }).then(async (response) => {
    if (!response.ok) {
      throw new Error(await responseMessage(response, 'Analytics could not be loaded. Please try again.'));
    }
    const body = await response.json();
    if (!body || !body.data || typeof body.data !== 'object') {
      throw new Error('The analytics response was incomplete. Please try again.');
    }
    renderAnalytics(body.data);
  }).catch((error) => {
    loading.classList.add('is-hidden');
    showAlert('Could not load analytics', error.message || 'Analytics could not be loaded. Please try again.', 'error');
  });
})();
