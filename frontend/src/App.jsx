import {
    Navigate,
    Route,
    Routes,
    useLocation
} from "react-router";

import EventPage from "./pages/EventPage";
import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";

import { useAuth } from "./context/AuthContext";


/*
 * Sends signed-out users to /login, remembering
 * where they were going so login can return them.
 */
function RequireAuth({ children }) {

    const { isAuthenticated } = useAuth();

    const location = useLocation();

    if (!isAuthenticated) {

        return (
            <Navigate
                to="/login"
                replace
                state={{ from: location.pathname }}
            />
        );
    }

    return children;
}


/*
 * Keeps signed-in users off the login and register pages.
 */
function GuestOnly({ children }) {

    const { isAuthenticated } = useAuth();

    const location = useLocation();

    if (isAuthenticated) {

        return (
            <Navigate
                to={location.state?.from || "/"}
                replace
            />
        );
    }

    return children;
}


function App() {

    const {
        loading: authLoading
    } = useAuth();


    /*
     * AUTHENTICATION LOADING
     */

    if (authLoading) {

        return (
            <div className="app">

                <div className="loading-screen">

                    <div className="loading-spinner" />

                    <p>
                        Loading X-NEWS...
                    </p>

                </div>

            </div>
        );
    }


    return (
        <Routes>

            <Route
                path="/"
                element={
                    <RequireAuth>
                        <HomePage />
                    </RequireAuth>
                }
            />

            <Route
                path="/events/:id"
                element={
                    <RequireAuth>
                        <EventPage />
                    </RequireAuth>
                }
            />

            <Route
                path="/login"
                element={
                    <GuestOnly>
                        <LoginPage />
                    </GuestOnly>
                }
            />

            <Route
                path="/register"
                element={
                    <GuestOnly>
                        <RegisterPage />
                    </GuestOnly>
                }
            />

            <Route
                path="*"
                element={
                    <Navigate to="/" replace />
                }
            />

        </Routes>
    );
}

export default App;
