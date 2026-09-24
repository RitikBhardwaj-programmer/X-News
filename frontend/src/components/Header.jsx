import { useState } from "react";
import { Link } from "react-router";

import { useAuth } from "../context/AuthContext";
import { getTheme, saveTheme } from "../utils/theme";


function Header() {

    const {
        user,
        logout
    } = useAuth();

    const [theme, setTheme] =
        useState(getTheme);


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
