/* eslint-disable */
import { useEffect, useRef, useCallback } from 'react';

const TURNSTILE_SCRIPT_URL =
  'https://challenges.cloudflare.com/turnstile/v0/api.js';
const TURNSTILE_SCRIPT_ID = 'cf-turnstile-script';

function loadTurnstileScript() {
  if (document.getElementById(TURNSTILE_SCRIPT_ID)) {
    return;
  }
  const script = document.createElement('script');
  script.id = TURNSTILE_SCRIPT_ID;
  script.src = `${TURNSTILE_SCRIPT_URL}?render=explicit`;
  script.async = true;
  script.defer = true;
  document.head.appendChild(script);
}

export default function TurnstileWidget({
  siteKey,
  language = 'auto',
  onVerified,
  widgetKey,
}) {
  const containerRef = useRef(null);
  const widgetIdRef = useRef(null);

  const onVerifiedRef = useRef(onVerified);
  useEffect(() => {
    onVerifiedRef.current = onVerified;
  }, [onVerified]);

  useEffect(() => {
    loadTurnstileScript();
    let intervalId;

    const renderTurnstile = () => {
      if (!window.turnstile || !containerRef.current) return;
      
      if (widgetIdRef.current) {
        try { window.turnstile.remove(widgetIdRef.current); } catch(e) {}
      }
      containerRef.current.innerHTML = '';

      try {
        widgetIdRef.current = window.turnstile.render(containerRef.current, {
          sitekey: siteKey,
          callback: (token) => {
            if (onVerifiedRef.current) onVerifiedRef.current(token);
          },
          'error-callback': () => {
            if (onVerifiedRef.current) onVerifiedRef.current(null);
          },
          'expired-callback': () => {
            if (onVerifiedRef.current) onVerifiedRef.current(null);
          }
        });
      } catch (e) {
        console.error("Turnstile render error", e);
      }
    };

    if (window.turnstile) {
      renderTurnstile();
    } else {
      intervalId = setInterval(() => {
        if (window.turnstile) {
          clearInterval(intervalId);
          renderTurnstile();
        }
      }, 100);
    }

    return () => {
      if (intervalId) clearInterval(intervalId);
      if (widgetIdRef.current) {
        try { window.turnstile.remove(widgetIdRef.current); } catch(e) {}
        widgetIdRef.current = null;
      }
    };
  }, [siteKey, widgetKey]);

  return <div ref={containerRef} className="cf-turnstile" />;
}
