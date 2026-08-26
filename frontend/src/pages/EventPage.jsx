import { useEffect, useState } from "react";

import {
    getEvent,
    analyzeEvent
} from "../services/api";


function verificationInfo(status) {

    switch (status) {

        case "VERIFIED":
            return {
                icon: "✓",
                label: "Verified",
                className: "verified"
            };

        case "FALSE":
            return {
                icon: "!",
                label: "False",
                className: "false"
            };

        case "CONTESTED":
            return {
                icon: "!",
                label: "Contested",
                className: "contested"
            };

        default:
            return {
                icon: "?",
                label: "Unverified",
                className: "unverified"
            };
    }
}


function riskInfo(risk) {

    if (risk == null) {
        return {
            value: "—",
            label: "Not analyzed",
            className: ""
        };
    }

    const percentage =
        Math.round(risk * 100);

    if (risk < 0.3) {
        return {
            value: `${percentage}%`,
            label: "Low risk",
            className: "risk-low"
        };
    }

    if (risk < 0.7) {
        return {
            value: `${percentage}%`,
            label: "Moderate risk",
            className: "risk-medium"
        };
    }

    return {
        value: `${percentage}%`,
        label: "High risk",
        className: "risk-high"
    };
}


function EventPage({
                       eventId,
                       onBack
                   }) {

    const [event, setEvent] =
        useState(null);

    const [loading, setLoading] =
        useState(true);

    const [analyzing, setAnalyzing] =
        useState(false);

    const [error, setError] =
        useState(null);


    useEffect(() => {

        async function loadEvent() {

            try {

                setLoading(true);

                const data =
                    await getEvent(eventId);

                setEvent(data);

            } catch (error) {

                console.error(error);

                setError(
                    "Unable to load this event."
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
                "AI analysis failed. Please try again."
            );

        } finally {

            setAnalyzing(false);
        }
    }


    if (loading) {

        return (
            <div className="app">
                <div className="loading-screen">
                    <div className="loading-spinner" />
                    <p>
                        Loading story...
                    </p>
                </div>
            </div>
        );
    }


    if (error && !event) {

        return (
            <div className="app">

                <header className="header">
                    <div className="header-inner">

                        <div className="logo">
                            X-NEWS
                        </div>

                        <div className="tagline">
                            AI-powered news intelligence
                        </div>

                    </div>
                </header>

                <main className="container">

                    <button
                        className="back-button"
                        onClick={onBack}
                    >
                        ← Back to news
                    </button>

                    <div className="error-card">
                        <h2>
                            Something went wrong
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

    const risk =
        riskInfo(
            event.misinformationRisk
        );

    const sourceCount = event.articles?.length || 0;


    return (
        <div className="app">

            {/* HEADER */}

            <header className="header">

                <div className="header-inner">

                    <div className="logo">
                        X-NEWS
                    </div>

                    <div className="tagline">
                        AI-powered news intelligence
                    </div>

                </div>

            </header>


            {/* MAIN */}

            <main className="container">

                <button
                    className="back-button"
                    onClick={onBack}
                >
                    ← Back to news
                </button>


                {/* EVENT HEADER */}

                <section className="event-header">

                    <div className="event-kicker">
                        {sourceCount}{" "}
                        {sourceCount === 1
                            ? "SOURCE"
                            : "SOURCES"}
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
                            Misinformation risk
                        </div>

                        <div
                            className={`status-main ${risk.className}`}
                        >
                            {risk.value}
                        </div>

                        <div className="status-help">
                            {risk.label}
                        </div>

                    </div>

                </section>


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
                                available sources and
                                generate a neutral summary,
                                bias analysis, disagreement
                                level and misinformation
                                risk.
                            </p>

                            <button
                                className="analyze-button"
                                onClick={handleAnalyze}
                                disabled={analyzing}
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
                                ? "source"
                                : "sources"}
                        </span>

                    </div>


                    <div className="source-list">

                        {event.articles?.map(
                            (article, index) => (

                                <article
                                    className="source-card"
                                    key={article.id}
                                >

                                    <div className="source-number">
                                        0{index + 1}
                                    </div>

                                    <div className="source-content">

                                        <div className="source-name">
                                            {article.source}
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

                            )
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
                                    The AI risk score should
                                    therefore be treated as
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

export default EventPage;