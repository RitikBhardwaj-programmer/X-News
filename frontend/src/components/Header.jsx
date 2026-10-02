import { useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router";

import { useAuth } from "../context/useAuth";
import { getTheme, saveTheme } from "../utils/theme";


function Header() {

    const {
        user,
        logout
    } = useAuth();

    const [theme, setTheme] =
        useState(getTheme);

    const [searchParams] =
        useSearchParams();

    const [query, setQuery] =
        useState(searchParams.get("q") || "");

    const navigate =
        useNavigate();


    function submitSearch(submitEvent) {

        submitEvent.preventDefault();

        const text = query.trim();

        if (text) {
            navigate(`/search?q=${encodeURIComponent(text)}`);
        }
    }


    function toggleTheme() {

        const next =
            theme === "dark"
                ? "light"
                : "dark";

        saveTheme(next);
        setTheme(next);
    }


    return (
        <header className="header">

            <div className="header-inner">

                <Link
                    to="/"
                    className="logo"
                >
                    X-NEWS
                </Link>


                <div className="header-actions">

                    <form
                        className="header-search"
                        role="search"
                        onSubmit={submitSearch}
                    >
                        <input
                            type="search"
                            value={query}
                            onChange={(changeEvent) =>
                                setQuery(changeEvent.target.value)
                            }
                            placeholder="Search articles"
                            aria-label="Search articles"
                            maxLength={200}
                        />
                    </form>

                    {user?.role === "ADMIN" && (
                        <Link
                            to="/review"
                            className="logout-button"
                        >
                            Review
                        </Link>
                    )}

                    <span className="user-email">
                        {user?.email}
                    </span>

                    <button
                        className="theme-toggle"
                        onClick={toggleTheme}
                        aria-label={
                            theme === "dark"
                                ? "Switch to light theme"
                                : "Switch to dark theme"
                        }
                        title={
                            theme === "dark"
                                ? "Light theme"
                                : "Dark theme"
                        }
                    >
                        {theme === "dark" ? "☀" : "☾"}
                    </button>

                    <button
                        className="logout-button"
                        onClick={logout}
                    >
                        Logout
                    </button>

                </div>

            </div>

        </header>
    );
}

export default Header;
