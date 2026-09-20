(function () {
  const logout = function () {
    sessionStorage.removeItem('smartlink_access_token');
    sessionStorage.removeItem('smartlink_auth_message');
    window.location.assign('/');
  };

  window.SmartLinkNav = {
    logout: logout
  };

  const trimTrailingSlashes = function (value) {
    return String(value ?? '').replace(/\/+$/, '');
  };

  const encodeShortCode = function (shortCode) {
    return encodeURIComponent(String(shortCode ?? '').replace(/^\/+|\/+$/g, ''));
  };

  const configuredDomain = function () {
    return trimTrailingSlashes(window.SmartLinkConfig && window.SmartLinkConfig.domain);
  };

  const configuredEndpoint = function () {
    return trimTrailingSlashes(window.SmartLinkConfig && window.SmartLinkConfig.endpoint);
  };

  window.SmartLinkDisplay = {
    displayShortUrl: function (shortCode) {
      return configuredDomain() + '/' + encodeShortCode(shortCode);
    },
    buildShortUrl: function (shortCode) {
      return configuredEndpoint() + '/' + encodeShortCode(shortCode);
    },
    shortCodeFromUrl: function (value) {
      try {
        const parsed = new URL(value, window.location.origin);
        const path = parsed.pathname.replace(/\/+$/, '');
        return decodeURIComponent(path.slice(path.lastIndexOf('/') + 1));
      } catch (error) {
        return '';
      }
    }
  };

  document.querySelectorAll('[data-logout]').forEach(function (button) {
    button.addEventListener('click', logout);
  });

  const menu = document.querySelector('[data-menu-toggle]');
  const navigation = document.querySelector('[data-navigation]');
  if (menu && navigation) {
    menu.addEventListener('click', function () {
      const open = navigation.classList.toggle('is-open');
      menu.setAttribute('aria-expanded', String(open));
    });
  }

  document.querySelectorAll('[data-password-input]').forEach(function (input) {
    const control = document.createElement('div');
    control.className = 'password-control';
    input.parentElement.insertBefore(control, input);
    control.appendChild(input);

    const button = document.createElement('button');
    button.type = 'button';
    button.className = 'password-toggle';
    button.setAttribute('aria-label', 'Show password');
    button.textContent = 'Show';
    button.addEventListener('click', function () {
      const visible = input.type === 'text';
      input.type = visible ? 'password' : 'text';
      button.textContent = visible ? 'Show' : 'Hide';
      button.setAttribute('aria-label', visible ? 'Show password' : 'Hide password');
    });
    control.appendChild(button);
  });
})();
