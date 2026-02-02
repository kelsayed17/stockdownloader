"""Authentication and crumb management for Yahoo Finance APIs."""

import logging
import requests

logger = logging.getLogger(__name__)

_SESSION = requests.Session()
_CRUMB: str | None = None


def get_session() -> requests.Session:
    """Get or create a session with Yahoo Finance cookies."""
    global _CRUMB
    if _CRUMB is None:
        _refresh_crumb()
    return _SESSION


def get_crumb() -> str:
    """Get the current crumb for Yahoo Finance API requests."""
    global _CRUMB
    if _CRUMB is None:
        _refresh_crumb()
    return _CRUMB or ""


def _refresh_crumb() -> None:
    """Refresh the session cookies and crumb from Yahoo Finance."""
    global _CRUMB
    try:
        _SESSION.headers.update({
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
        })
        _SESSION.get("https://finance.yahoo.com", timeout=10)
        resp = _SESSION.get("https://query2.finance.yahoo.com/v1/test/getcrumb", timeout=10)
        if resp.status_code == 200 and resp.text:
            _CRUMB = resp.text
            logger.info("Yahoo Finance crumb refreshed successfully")
        else:
            logger.warning(f"Failed to get crumb: status={resp.status_code}")
            _CRUMB = ""
    except Exception as e:
        logger.error(f"Error refreshing Yahoo crumb: {e}")
        _CRUMB = ""


def reset() -> None:
    """Reset the session and crumb."""
    global _CRUMB
    _CRUMB = None
    _SESSION.cookies.clear()
