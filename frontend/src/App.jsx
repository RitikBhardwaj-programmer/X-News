import { useEffect, useState } from "react";

import EventCard from "./components/EventCard";
import EventPage from "./pages/EventPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";

import { getEvents } from "./services/api";
import { useAuth } from "./context/AuthContext";


function App() {

    const {
        loading: authLoading,
        isAuthenticated,
        logout,
        user
    } = useAuth();


    const [authPage, setAuthPage] =
        useState("login");


    const [events, setEvents] =
        useState([]);

    const [selectedEventId, setSelectedEventId] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [error, setError] =
        useState(null);


    useEffect(() => {

        if (!isAuthenticated) {
            return;
        }


        async function loadEvents() {

            try {

                setLoading(true);
                setError(null);

                const data =
                    await getEvents();

                setEvents(data);

            } catch (error) {

                console.error(error);

                setError(
                    "Unable to load news."
                );

            } finally {

                setLoading(false);
            }
        }

        loadEvents();

    }, [isAuthenticated]);


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


    /*
     * NOT AUTHENTICATED
     */

    if (!isAuthenticated) {

        if (authPage === "register") {

            return (
                <RegisterPage
                    onLogin={() =>
                        setAuthPage("login")
                    }
                />
            );
        }


        return (
            <LoginPage
                onRegister={() =>
                    setAuthPage("register")
                }
            />
        );
    }


    /*
     * EVENT PAGE
     */

    if (selectedEventId !== null) {

        return (
            <EventPage
                eventId={selectedEventId}
                onBack={() =>
                    setSelectedEventId(null)
                }
            />
        );
    }


    /*
     * MAIN APPLICATION
     */

    return (
        <div className="app">

            <header className="header">

                <div className="header-inner">

                    <div className="logo">
                        X-NEWS
                    </div>


                    <div className="header-actions">

                        <span className="user-email">
                            {user?.email}
                        </span>

                        <button
                            className="logout-button"
                            onClick={logout}
                        >
                            Logout
                        </button>

                    </div>

                </div>

            </header>


            <main className="container">

                {loading ? (

                    <div className="loading-screen">

                        <div className="loading-spinner" />

                        <p>
                            Loading the latest stories...
                        </p>

                    </div>

                ) : error ? (

                    <div className="error-card">

                        <h2>
                            Unable to load news
                        </h2>

                        <p>
                            {error}
                        </p>

                        <button
                            className="analyze-button"
                            onClick={() =>
                                window.location.reload()
                            }
                        >
                            Try again
                        </button>

                    </div>

                ) : (

                    <>

                        <section className="home-hero">

                            <div>

                                <div className="hero-eyebrow">
                                    NEWS INTELLIGENCE
                                </div>

                                <h1>
                                    Understand the story,
                                    <br />
                                    not just the headline.
                                </h1>

                                <p>
                                    X-NEWS compares reporting
                                    across sources and uses AI
                                    to surface bias, disagreement
                                    and misinformation risk.
                                </p>

                            </div>

                        </section>


                        <section className="news-section">

                            <div className="news-section-header">

                                <div>

                                    <span className="section-eyebrow">
                                        LIVE FEED
                                    </span>

                                    <h2>
                                        Latest stories
                                    </h2>

                                </div>

                                <span className="section-count">
                                    {events.length}{" "}
                                    {events.length === 1
                                        ? "story"
                                        : "stories"}
                                </span>

                            </div>


                            {events.length === 0 ? (

                                <div className="empty-state">

                                    <h3>
                                        No stories yet
                                    </h3>

                                    <p>
                                        New stories will appear
                                        here as they are collected.
                                    </p>

                                </div>

                            ) : (

                                <div className="event-list">

                                    {events.map(
                                        (event) => (

                                            <EventCard
                                                key={event.id}
                                                event={event}
                                                onClick={() =>
                                                    setSelectedEventId(
                                                        event.id
                                                    )
                                                }
                                            />

                                        )
                                    )}

                                </div>

                            )}

                        </section>

                    </>

                )}

            </main>

        </div>
    );
}

export default App;