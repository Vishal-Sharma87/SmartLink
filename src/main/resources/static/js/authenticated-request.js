(function () {
  const accessTokenKey = 'smartlink_access_token';
  const expiredMessageKey = 'smartlink_auth_message';
  let refreshPromise = null;
  let redirectStarted = false;

  const currentToken = () => sessionStorage.getItem(accessTokenKey);

  const headersWithToken = (headers, token) => {
    const result = new Headers(headers || {});
    if (token) result.set('Authorization', 'Bearer ' + token);
    else result.delete('Authorization');
    return result;
  };

  const isAuthenticationFailure = async (response) => {
    if (response.status !== 401) return false;
    try {
      const body = await response.clone().json();
      return body && body.code === 'AUTHENTICATION_FAILED';
    } catch (error) {
      return false;
    }
  };

  const expireSession = () => {
    sessionStorage.removeItem(accessTokenKey);
    if (redirectStarted) return;
    redirectStarted = true;
    sessionStorage.setItem(expiredMessageKey, 'Your session has expired. Please log in again.');
    if (window.location.pathname !== '/login') window.location.assign('/login');
  };

  const refreshAccessToken = () => {
    if (refreshPromise) return refreshPromise;

    refreshPromise = fetch('/auth/refresh-token', {
      method: 'POST',
      credentials: 'same-origin',
      headers: { Accept: 'application/json' }
    })
      .then(async (response) => {
        if (!response.ok) throw new Error('Refresh rejected');
        const body = await response.json();
        const token = body && body.data && body.data.jwtToken;
        if (!token) throw new Error('Refresh response was incomplete');
        sessionStorage.setItem(accessTokenKey, token);
        return token;
      })
      .finally(() => {
        refreshPromise = null;
      });

    return refreshPromise;
  };

  const authenticatedRequest = async (input, init) => {
    const options = { ...(init || {}) };
    const tokenUsed = currentToken();
    options.credentials = options.credentials || 'same-origin';
    options.headers = headersWithToken(options.headers, tokenUsed);

    const response = await fetch(input, options);
    if (!(await isAuthenticationFailure(response))) return response;

    // Another request may have refreshed the same token while this request was in flight.
    let token = currentToken();
    if (!token || token === tokenUsed) {
      try {
        token = await refreshAccessToken();
      } catch (error) {
        expireSession();
        return response;
      }
    }

    const retryOptions = { ...options, headers: headersWithToken(init && init.headers, token) };
    const retryResponse = await fetch(input, retryOptions);
    if (await isAuthenticationFailure(retryResponse)) expireSession();
    return retryResponse;
  };

  window.SmartLinkAuth = {
    request: authenticatedRequest,
    accessToken: currentToken
  };
})();
