import { useEffect, useState } from "react";
import { Link, useParams } from "react-router";

import Header from "../components/Header";

import {
    getEvent,
    getEventCoverage,
    getEventMatches,
    getEventEntities,
    analyzeEvent
} from "../services/api";

import {
    verificationInfo,
    formatRelativeTime
} from "../utils/eventLabels";


function EventPage() {

    const { id: eventId } =
        useParams();

    const [event, setEvent] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [analyzing, setAnalyzing] =
        useState(false);

    const [error, setError] =
        useState(null);

    const [notFound, setNotFound] =
        useState(false);

    const [loadedCoverage, setLoadedCoverage] =
        useState(null);


    // Coverage is extra context: if it fails, the event still shows.
    useEffect(() => {

        getEventCoverage(eventId)
            .then((data) => setLoadedCoverage({ eventId, data }))
            .catch((error) => console.error(error));

    }, [eventId]);

    // Ignore a result left over from a previously viewed event.
    const coverage =
        loadedCoverage?.eventId === eventId
            ? loadedCoverage.data
            : null;


    const [loadedMatches, setLoadedMatches] =
        useState(null);

    // Match confidence is also extra context; articles from before
    // decisions were recorded simply have none.
    useEffect(() => {

        getEventMatches(eventId)
            .then((data) => setLoadedMatches({ eventId, data }))
            .catch((error) => console.error(error));

    }, [eventId]);

    const matchesById = new Map(
        (loadedMatches?.eventId === eventId ? loadedMatches.data : [])
            .map((match) => [match.articleId, match])
    );


    const [loadedEntities, setLoadedEntities] =
        useState(null);

    // Entities are extra context too; older articles have none recorded.
    useEffect(() => {

        getEventEntities(eventId)
            .then((data) => setLoadedEntities({ eventId, data }))
            .catch((error) => console.error(error));

    }, [eventId]);

    const entities =
        loadedEntities?.eventId === eventId
            ? loadedEntities.data
            : [];


    useEffect(() => {

        async function loadEvent() {

            try {

                setLoading(true);
                setError(null);
                setNotFound(false);

                const data =
                    await getEvent(eventId);

                setEvent(data);

            } catch (error) {

                console.error(error);

                setNotFound(error.status === 404);

                setError(
                    error.status === 404
                        ? "This event doesn't exist. It may have been removed, or the link is wrong."
                        : "Unable to load this event."
                );

            } finally {

                setLoading(false);
            }
        }

        loadEvent();

    }, [eventId]);


    async function handleAnalyze() {

        try {

            setAnalyzing(true);
            setError(null);

            const analyzedEvent =
                await analyzeEvent(eventId);

            setEvent(analyzedEvent);

        } catch (error) {

            console.error(error);

            setError(
                error.status === 503
                    ? "The AI service is busy right now. Please try again in a minute."
                    : "AI analysis failed. Please try again."
            );

        } finally {

            setAnalyzing(false);
        }
    }


    if (loading) {

        return (
            <div className="app">

                <Header />

                <main
                    className="container"
                    aria-busy="true"
                >
                    <div className="skeleton skeleton-line short" />
                    <div className="skeleton skeleton-title" />
                    <div className="skeleton skeleton-line" />
                    <div className="skeleton skeleton-line" />

                    <div className="status-grid">
                        <div className="skeleton skeleton-block" />
                        <div className="skeleton skeleton-block" />
                        <div className="skeleton skeleton-block" />
                    </div>
                </main>

            </div>
        );
    }


    if (error && !event) {

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

                    <div className="error-card">
                        <h2>
                            {notFound
                                ? "Event not found"
                                : "Something went wrong"}
                        </h2>

                        <p>
                            {error}
                        </p>
                    </div>

                </main>

            </div>
        );
    }


    const verification =
        verificationInfo(
            event.verificationStatus
        );

    const sourceCount = event.articles?.length || 0;

    const outletCount = coverage?.outletCount;

    // Articles in the order outlets published them, with outlet, section
    // and time from the coverage timeline.
    const timelineById = new Map(
        (coverage?.timeline || []).map(
            (entry, position) => [entry.articleId, { ...entry, position }]
        )
    );

    const orderedArticles = [...(event.articles || [])].sort(
        (a, b) =>
            (timelineById.get(a.id)?.position ?? Infinity)
            - (timelineById.get(b.id)?.position ?? Infinity)
    );

    // Citation markers use the same numbers as the source list.
    const articleNumberById = new Map(
        orderedArticles.map((article, index) => [article.id, index + 1])
    );


    return (
        <div className="app">

            <Header />


            {/* MAIN */}

            <main className="container">

                <Link
                    to="/"
                    className="back-button"
                >
                    ← Back to news
                </Link>


                {/* EVENT HEADER */}

                <section className="event-header">

                    <div className="event-kicker">
                        {sourceCount}{" "}
                        {sourceCount === 1
                            ? "ARTICLE"
                            : "ARTICLES"}

                        {outletCount != null && (
                            <>
                                {" · "}
                                {outletCount}{" "}
                                {outletCount === 1
                                    ? "OUTLET"
                                    : "OUTLETS"}
                            </>
                        )}
                    </div>

                    <h1 className="event-title">
                        {event.title}
                    </h1>

                    {event.description && (
                        <p className="event-description">
                            {event.description}
                        </p>
                    )}

                </section>


                {/* STATUS GRID */}

                <section className="status-grid">

                    <div className="status-card">

                        <div className="status-label">
                            Verification
                        </div>

                        <div
                            className={`status-main ${verification.className}`}
                        >
                            <span className="large-status-icon">
                                {verification.icon}
                            </span>

                            {verification.label}
                        </div>

                    </div>


                    <div className="status-card">

                        <div className="status-label">
                            Cross-source disagreement
                        </div>

                        <div className="status-main">
                            {event.disagreementLevel ||
                                "—"}
                        </div>

                        <div className="status-help">
                            Based on differences
                            between sources
                        </div>

                    </div>


                    <div className="status-card">

                        <div className="status-label">
                            Coverage
                        </div>

                        <div className="status-main">
                            {outletCount != null
                                ? `${outletCount} ${outletCount === 1 ? "outlet" : "outlets"}`
                                : "—"}
                        </div>

                        <div className="status-help">
                            {coverage?.firstSeen
                                ? `First report ${formatRelativeTime(coverage.firstSeen)}`
                                    + ` · latest ${formatRelativeTime(coverage.lastSeen)}`
                                : "Which outlets reported this, and when"}
                        </div>

                    </div>

                </section>

                <p className="ai-note">
                    Disagreement is an AI estimate, not a
                    fact-check. Treat it as a starting point,
                    not a verdict.
                </p>


                {/* ERROR */}

                {error && (
                    <div className="inline-error">
                        {error}
                    </div>
                )}


                {/* AI ANALYSIS */}

                <section className="content-section">

                    <div className="section-heading">

                        <div>
                            <span className="section-eyebrow">
                                AI ANALYSIS
                            </span>

                            <h2>
                                What the story says
                            </h2>
                        </div>

                    </div>


                    {!event.summary ? (

                        <div className="analysis-empty">

                            <div className="analysis-empty-icon">
                                ✦
                            </div>

                            <h3>
                                Analyze this story
                            </h3>

                            <p>
                                X-NEWS will compare the
                                available sources: what they
                                agree on and how each outlet
                                frames the story, every point
                                citing the articles it comes
                                from.
                            </p>

                            <button
                                className="analyze-button"
                                onClick={handleAnalyze}
                                disabled={analyzing}
                                aria-busy={analyzing}
                            >
                                {analyzing ? (
                                    <>
                                        <span className="button-spinner" />
                                        Analyzing with Gemini...
                                    </>
                                ) : (
                                    <>
                                        ✦ Analyze
                                    </>
                                )}
                            </button>

                        </div>

                    ) : event.analysis ? (

                        <>

                            <div className="analysis-card">

                                <div className="analysis-card-label">
                                    WHAT THE SOURCES AGREE ON
                                </div>

                                <ul className="cited-list">
                                    {event.analysis.agreedFacts.map((fact, index) => (
                                        <li key={index}>
                                            {fact.text}
                                            <Citations
                                                articles={fact.articles}
                                                numberById={articleNumberById}
                                            />
                                        </li>
                                    ))}
                                </ul>

                            </div>


                            {event.analysis.framing.length > 0 && (

                                <div className="analysis-card">

                                    <div className="analysis-card-label">
                                        HOW EACH OUTLET FRAMES IT
                                    </div>

                                    <ul className="cited-list">
                                        {event.analysis.framing.map((framing) => (
                                            <li key={framing.outlet}>
                                                <strong>{framing.outlet}:</strong>{" "}
                                                {framing.text}
                                                <Citations
                                                    articles={framing.articles}
                                                    numberById={articleNumberById}
                                                />
                                            </li>
                                        ))}
                                    </ul>

                                </div>

                            )}

                            <p className="match-note">
                                Every point cites the articles below
                                that state it; points without a source
                                are left out. AI-written, not a
                                fact-check.
                            </p>

                        </>

                    ) : (

                        <>

                            <div className="analysis-card">

                                <div className="analysis-card-label">
                                    SUMMARY
                                </div>

                                <p>
                                    {event.summary}
                                </p>

                            </div>


                            {event.biasAnalysis && (

                                <div className="analysis-card">

                                    <div className="analysis-card-label">
                                        CROSS-SOURCE BIAS
                                    </div>

                                    <p>
                                        {event.biasAnalysis}
                                    </p>

                                </div>

                            )}

                        </>

                    )}

                </section>


                {/* ENTITIES */}

                {entities.length > 0 && (

                    <section className="content-section">

                        <div className="section-heading">
                            <div>
                                <span className="section-eyebrow">
                                    MENTIONED
                                </span>

                                <h2>
                                    People, places and organisations
                                </h2>
                            </div>
                        </div>

                        <div className="entity-list">
                            {entities.map((entity) => (
                                <span
                                    key={entity.entityId}
                                    className="entity-chip"
                                >
                                    {entity.name}
                                    <span className="entity-count">
                                        {entity.articleCount}
                                    </span>
                                </span>
                            ))}
                        </div>

                        <p className="match-note">
                            Found automatically in headlines and
                            summaries; the number is how many
                            articles mention it.
                        </p>

                    </section>
                )}


                {/* SOURCES */}

                <section className="content-section">

                    <div className="section-heading">

                        <div>
                            <span className="section-eyebrow">
                                SOURCE COVERAGE
                            </span>

                            <h2>
                                What different sources report
                            </h2>
                        </div>

                        <span className="section-count">
                            {sourceCount}{" "}
                            {sourceCount === 1
                                ? "article"
                                : "articles"}
                            {coverage && " · oldest first"}
                        </span>

                    </div>


                    {matchesById.size > 0 && (
                        <p className="match-note">
                            Each article shows how it joined this
                            event: the matcher's confidence, an
                            estimate rather than a verdict.
                        </p>
                    )}

                    <div className="source-list">

                        {orderedArticles.map(
                            (article, index) => {

                                const entry =
                                    timelineById.get(article.id);

                                const seen =
                                    entry?.publishedAt
                                    || entry?.observedAt;

                                const match =
                                    matchesById.get(article.id);

                                return (

                                <article
                                    className="source-card"
                                    id={`article-${article.id}`}
                                    key={article.id}
                                >

                                    <div className="source-number">
                                        {String(index + 1).padStart(2, "0")}
                                    </div>

                                    <div className="source-content">

                                        <div className="source-name">
                                            {entry
                                                ? entry.outlet
                                                    + (entry.feed ? ` → ${entry.feed}` : "")
                                                : article.source}

                                            {seen && (
                                                <>
                                                    {" · "}
                                                    <time dateTime={seen}>
                                                        {formatRelativeTime(seen)}
                                                    </time>
                                                </>
                                            )}

                                            {match && (
                                                <span
                                                    className="match-confidence"
                                                    title={matchTitle(match)}
                                                >
                                                    {matchLabel(match)}
                                                </span>
                                            )}
                                        </div>

                                        <h3>
                                            {article.title}
                                        </h3>

                                        {article.description && (
                                            <p>
                                                {article.description}
                                            </p>
                                        )}

                                        {article.url && (
                                            <a
                                                href={article.url}
                                                target="_blank"
                                                rel="noreferrer"
                                            >
                                                Read original article
                                                <span>
                                                    ↗
                                                </span>
                                            </a>
                                        )}

                                    </div>

                                </article>

                                );
                            }
                        )}

                    </div>

                </section>


                {/* FACT CHECK */}

                <section className="content-section">

                    <div className="section-heading">

                        <div>
                            <span className="section-eyebrow">
                                VERIFICATION
                            </span>

                            <h2>
                                Fact check
                            </h2>
                        </div>

                    </div>


                    <div className="fact-check-card">

                        <div
                            className={`fact-check-badge ${verification.className}`}
                        >

                            <span>
                                {verification.icon}
                            </span>

                            {verification.label}

                        </div>


                        {event.factChecks &&
                        event.factChecks.length > 0 ? (

                            event.factChecks.map(
                                (factCheck) => (

                                    <div
                                        className="fact-check-content"
                                        key={factCheck.id}
                                    >

                                        <div className="fact-check-row">

                                            <span>
                                                Agency
                                            </span>

                                            <strong>
                                                {factCheck.agency}
                                            </strong>

                                        </div>


                                        <div className="fact-check-row">

                                            <span>
                                                Claim
                                            </span>

                                            <p>
                                                {factCheck.claim}
                                            </p>

                                        </div>


                                        {factCheck.explanation && (

                                            <div className="fact-check-row">

                                                <span>
                                                    Explanation
                                                </span>

                                                <p>
                                                    {factCheck.explanation}
                                                </p>

                                            </div>

                                        )}


                                        {factCheck.sourceUrl && (

                                            <a
                                                href={factCheck.sourceUrl}
                                                target="_blank"
                                                rel="noreferrer"
                                                className="fact-check-link"
                                            >
                                                View source ↗
                                            </a>

                                        )}

                                    </div>

                                )
                            )

                        ) : (

                            <div className="unverified-message">

                                <h3>
                                    No trusted fact-check
                                    available
                                </h3>

                                <p>
                                    This story has not been
                                    verified by a trusted
                                    fact-checking source.
                                    The AI analysis above is
                                    an assessment, not a
                                    confirmation of truth or
                                    falsehood.
                                </p>

                            </div>

                        )}

                    </div>

                </section>

            </main>

        </div>
    );
}


// [1] [3]: links to the cited articles in the source list.
function Citations({ articles, numberById }) {

    return (
        <span className="citations">
            {articles
                .filter((id) => numberById.has(id))
                .map((id) => (
                    <a
                        key={id}
                        href={`#article-${id}`}
                        className="citation"
                    >
                        [{numberById.get(id)}]
                    </a>
                ))}
        </span>
    );
}


// "joined · 97%" or "started this event". Probabilities are rounded to
// whole percent and never shown as a flat 100%.
function matchLabel(match) {

    if (match.decision === "started") {
        return "started this event";
    }

    return `joined · ${formatPercent(match.probability)}`;
}

function matchTitle(match) {

    const estimate =
        match.decision === "started"
            ? `No existing event reached the ${match.matcher} matcher's threshold`
                + ` (best ${formatPercent(match.probability)}, needed ${formatPercent(match.threshold)}).`
            : `The ${match.matcher} matcher estimated ${formatPercent(match.probability)}`
                + ` that this article belongs here (threshold ${formatPercent(match.threshold)}).`;

    return `${estimate} A model estimate, not a fact-check.`;
}

function formatPercent(probability) {

    if (probability === null || probability === undefined) {
        return "n/a";
    }

    const percent = Math.round(probability * 100);

    return percent >= 100 ? ">99%" : `${percent}%`;
}

export default EventPage;