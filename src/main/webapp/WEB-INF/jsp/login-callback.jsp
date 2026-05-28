login-callback.jsp -> <%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8"/>
  <title>Finishing login...</title>
</head>
<body>
  <p>Finishing login... please wait.</p>
  <script>
    const AUTH_SERVER = 'https://auth.fintrexfinance.com:2083';
    const params = new URLSearchParams(window.location.search);
    const original = params.get('redirect') || '/index';

    (async function finishLogin() {
      try {
        // fetch SSO user info using cookie
        const res = await fetch(AUTH_SERVER + '/me', { credentials: 'include' });
        if (!res.ok) throw new Error('SSO session absent');
        const user = await res.json();

        // create local session
        await fetch('/api/login-callback', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'same-origin',
          body: JSON.stringify(user)
        });

        // navigate to original page
        window.location.href = original;
      } catch (err) {
        console.error('Login callback failed', err);
        // fallback: redirect to auth login again (ensures re-auth)
        const clientCallback = encodeURIComponent(window.location.origin + '/login-callback');
        const redirectParam = encodeURIComponent(original);
        window.location.href = `${AUTH_SERVER}/auth/login?client_redirect_uri=${clientCallback}&redirect=${redirectParam}`;
      }
    })();
  </script>
</body>
</html>