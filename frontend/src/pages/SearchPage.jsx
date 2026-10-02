import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router";

import Header from "../components/Header";
import { searchArticles } from "../services/api";
import { formatRelativeTime } from "../utils/eventLabels";


function SearchPage() {

    const [searchParams] =
        useSearchParams();

    const query =
        (searchParams.get("q") || "").trim();

    const [loaded, setLoaded] =
        useState(null);

    const [error, setError] =
        useState(null);


    useEffect(() => {

        if (!query) {
            return;
        }

        let cancelled = false;

        searchArticles(query)
            .then((data) => {
                if (!cancelled) {
                    setError(null);
                    setLoaded({ query, data });
                }
            })
            .catch((searchError) => {
                console.error(searchError);
                if (!cancelled) {
                    setError("Search failed. Please try again.");
                }
            });

        return () => {
            cancelled = true;
        };

    }, [query]);


    // Ignore results left over from a previous query.
    const response =
        loaded?.query === query
            ? loaded.data
            : null;

    const loading =
        query && !response && !error;


    return (
        <div className="app">

            <Header />

            <main className="container">

                <Link
                    to="/"
                    className="back-button"
                >
                    ← Back to news
                </Link>

                <section className="news-section">

                    <div className="news-section-header">

                        <div>
                            <span className="section-eyebrow">
                                SEARCH
                            </span>

                            <h2>
                                {query
                                    ? `Results for "${query}"`
                                    : "Search articles"}
                            </h2>
                        </div>

                        {response && (
                            <span className="section-count">
                                {response.results.length}{" "}
                                {response.results.length === 1
                                    ? "article"
                                    : "articles"}
                            </span>
                        )}

                    </div>

                    {response?.mode === "text" && (
                        <p className="search-note">
                            Matching words only: meaning-based
                            search is unavailable right now.
                        </p>
                    )}

                    {!query ? (
                        <div className="empty-state">
                            <h3>
                                Type a search in the header
                            </h3>
                            <p>
                                Search finds articles by their
                                words and by their meaning.
                            </p>
                        </div>
                    ) : error ? (
                        <div className="inline-error">
                            {error}
                        </div>
                    ) : loading ? (
                        <div className="event-list">
                            {[0, 1, 2].map((key) => (
                                <div
                                    className="skeleton skeleton-card"
                                    key={key}
                                />
                            ))}
                        </div>
                    ) : response.results.length === 0 ? (
                        <div className="empty-state">
                            <h3>
                                No articles found
                            </h3>
                            <p>
                                Try other words or a shorter search.
                            </p>
                        </div>
                    ) : (
                        <div className="event-list">
                            {response.results.map((result) => (
                                <SearchResult
                                    key={result.articleId}
                                    result={result}
                                />
                            ))}
                        </div>
                    )}

                </section>

            </main>

        </div>
    );
}


// An article that belongs to an event opens the event page; otherwise
// the outlet's own page.
function SearchResult({ result }) {

    const outlet =
        (result.source || "").split(" -> ")[0];

    const age =
        formatRelativeTime(result.publishedAt);

    const body = (
        <>
            <div className="event-card-top">
                <span className="source-count">
                    {outlet}
                    {age && (
                        <>
                            {" · "}
                            <time dateTime={result.publishedAt}>
                                {age}
                            </time>
                        </>
                    )}
                </span>
            </div>

            <h3 className="event-card-title">
                {result.title}
            </h3>
        </>
    );

    return result.eventId ? (
        <Link
            to={`/events/${result.eventId}`}
            className="event-card"
        >
            {body}
        </Link>
    ) : (
        <a
            href={result.url}
            className="event-card"
            target="_blank"
            rel="noreferrer"
        >
            {body}
        </a>
    );
}

export default SearchPage;
