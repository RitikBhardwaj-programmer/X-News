import { useEffect, useState } from "react";

import EventCard from "../components/EventCard";
import Header from "../components/Header";

import { getEvents } from "../services/api";


function HomePage() {

    const [events, setEvents] =
        useState([]);

    const [loading, setLoading] =
        useState(true);

    const [error, setError] =
        useState(null);


    useEffect(() => {

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

    }, []);


    return (
        <div className="app">

            <Header />


            <main className="container">

                {error ? (

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


                        <section
                            className="news-section"
                            aria-busy={loading}
                        >

                            <div className="news-section-header">

                                <div>

                                    <span className="section-eyebrow">
                                        LIVE FEED
                                    </span>

                                    <h2>
                                        Latest stories
                                    </h2>

                                </div>

                                {!loading && (
                                    <span className="section-count">
                                        {events.length}{" "}
                                        {events.length === 1
                                            ? "story"
                                            : "stories"}
                                    </span>
                                )}

                            </div>


                            {loading ? (

                                <div className="event-list">

                                    {[0, 1, 2, 3].map((key) => (
                                        <div
                                            className="skeleton skeleton-card"
                                            key={key}
                                        />
                                    ))}

                                </div>

                            ) : events.length === 0 ? (

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

export default HomePage;
