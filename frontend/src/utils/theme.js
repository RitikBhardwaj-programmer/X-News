const THEME_KEY = "xnews_theme";


function readStoredTheme() {

    try {
        return localStorage.getItem(THEME_KEY);
    } catch {
        return null;
    }
}


/*
 * The theme currently in effect: the saved choice,
 * otherwise the operating system preference.
 */
export function getTheme() {

    const stored = readStoredTheme();

    if (stored === "light" || stored === "dark") {
        return stored;
    }

    return window.matchMedia(
        "(prefers-color-scheme: dark)"
    ).matches
        ? "dark"
        : "light";
}


export function saveTheme(theme) {

    document.documentElement.dataset.theme = theme;

    try {
        localStorage.setItem(THEME_KEY, theme);
    } catch {
        // Storage can be blocked (private mode); the choice then lasts for this page only.
    }
}
