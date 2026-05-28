<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Fintrex - Drop-in Net</title>
  <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600&display=swap" rel="stylesheet" />
  <link href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css" rel="stylesheet" />
  <style>
      
      
      
      
    * { box-sizing: border-box; }
    html, body {
      margin: 0; height: 100%;
      font-family: 'Inter', sans-serif;
      color: #2c2c2c;
      display: flex; justify-content: center; align-items: center;
      padding: 0 2rem; user-select: none;
      background: linear-gradient(135deg, #e6f4ea 50%, #e8e0f9 50%);
    }
    .auth-box {
      background: #fff; border-radius: 14px; width: 480px;
      padding: 3rem 3.5rem;
      box-shadow: 0 8px 20px rgba(88,99,93,.15), inset 0 0 0 1px #d9e4dd;
      text-align: center;
    }
    .login-logo { margin-bottom: 2.5rem; }
    .login-logo img { width: 220px; height: auto; filter: drop-shadow(0 3px 6px rgba(0,0,0,.07)); }
    button.btn-primary {
      margin-top: 1.2rem; width: 100%;
      background: linear-gradient(90deg,#4caf50 0%,#7f56da 100%);
      border: none; padding: 1rem 0;
      font-size: 1.18rem; font-weight: 700;
      border-radius: 12px; color: #fff;
      cursor: pointer;
      box-shadow: 0 6px 16px rgba(127,86,218,.6);
      transition: background .4s, box-shadow .3s;
      display: flex; justify-content: center; align-items: center; gap: .7rem;
    }
    button.btn-primary:hover:not(:disabled) {
      background: linear-gradient(90deg,#66bb6a 0%,#9160ff 100%);
      box-shadow: 0 8px 25px rgba(145,96,255,.85);
    }
    #response {
      display: none; text-align: left; margin-top: 1rem;
      white-space: pre-wrap; background: #f0f0f0;
      padding: 1rem; border-radius: 8px;
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    }
    @media (max-width:520px){
      .auth-box { width:100%; padding:2.5rem 2rem }
      .login-logo img { width:180px }
    }
  </style>
</head>
<body>
  <main class="auth-box" role="main" aria-labelledby="loginTitle">
    <div class="login-logo" aria-hidden="true">
      <img src="${pageContext.request.contextPath}/files/images/fintrex-din-purple.png" alt="Fintrex logo" />
    </div>

    <button id="ssoLoginBtn" class="btn-primary" aria-live="polite" aria-busy="false">Login</button>

    <div id="response" role="status" aria-live="polite"></div>
  </main>

  <script>
    const AUTH_SERVER = 'https://auth.fintrexfinance.com:2083';
    const CTX = '<%= request.getContextPath() %>';
    const responseEl = document.getElementById('response');

    function showResponseBox(msg){
      responseEl.style.display = 'block';
      responseEl.textContent = msg;
    }

    function getRedirectTarget(){
      const p = new URLSearchParams(window.location.search);
      return p.get('redirect') || '/index';
    }

   async function redirectToLoginSSO() {
  const target = getRedirectTarget();
  const clientRedirect = encodeURIComponent(
    window.location.origin + CTX + '/login?redirect=' + encodeURIComponent(target)
  );

  try {
    // ✅ Step 1: Check if user already has a valid SSO cookie
    const res = await fetch(AUTH_SERVER + '/me', { credentials: 'include' });

    if (res.ok) {
      // ✅ User already authenticated with SSO
      const ssoUser = await res.json();

      // Create local session directly
      const localRes = await fetch(CTX + '/api/login-callback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify(ssoUser)
      });

      if (localRes.ok) {
        window.location.replace(CTX + target);
      } else {
        showResponseBox("⚠️ Unable to create local session. Please try again.");
      }
    } else if (res.status === 401) {
      // ❌ No valid SSO session → go to auth login
      window.location.href = AUTH_SERVER + '/auth/login?client_redirect_uri=' + clientRedirect;
    } else {
      // ❌ Other unexpected response (network / CORS / etc.)
      showResponseBox("⚠️ Could not verify SSO session (status " + res.status + "). Please try Login again.");
    }

  } catch (err) {
    console.error('Error checking SSO session:', err);
    showResponseBox("⚠️ Connection issue verifying SSO. Please try Login again.");
  }
}



    async function fetchSSOUser(){
      const res = await fetch(AUTH_SERVER + '/me', { credentials: 'include' });
      if(!res.ok) throw new Error('No valid SSO session');
      return await res.json();
    }

    async function createLocalSession(user){
      const res = await fetch(CTX + '/api/login-callback', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'same-origin',
        body: JSON.stringify(user)
      });
      if(!res.ok) throw new Error('Local session creation failed');
    }

    async function whoAmI(){
      try {
        const res = await fetch(CTX + '/api/me', { credentials: 'same-origin' });
        if(!res.ok) return null;
        return await res.json();
      } catch { return null; }
    }

    (async function init(){
      try {
        // 1. Check local session first
        const meLocal = await whoAmI();
        if(meLocal){
          // Re-check SSO validity before redirect
          const check = await fetch(AUTH_SERVER + '/me', { credentials: 'include' });
          if(check.ok){
            window.location.replace(CTX + getRedirectTarget());
            return;
          } else {
            showResponseBox("⚠️ Could not verify SSO session (status " + check.status + "). Please login again.");
            return;
          }
        }
        

        // 2. No local session -> Try to silently build one from SSO cookie
        
        const ssoUser = await fetchSSOUser();
        await createLocalSession(ssoUser);

        // Double-check SSO before redirect
        const verify = await fetch(AUTH_SERVER + '/me', { credentials: 'include' });
        if(verify.ok){
          window.location.replace(CTX + getRedirectTarget());
        } else {
          showResponseBox("⚠️ Could not verify SSO session. Please login again.");
        }

      } catch(err) {
        console.log('No active session:', err);
        showResponseBox("You are not logged in. Please click Login to continue.");
      }
    })();

    document.getElementById('ssoLoginBtn').addEventListener('click', redirectToLoginSSO);
    
  </script>
</body>
</html>
