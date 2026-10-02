import { useEffect, useState } from "react";
import { Link } from "react-router";

import Header from "../components/Header";
import { useAuth } from "../context/useAuth";
import { getClaimsForReview, reviewClaim } from "../services/api";
import { claimText } from "../utils/claimLabels";


const STATUSES = ["pending", "approved", "rejected"];


// Admin review queue for numeric claims: a claim is shown on event pages
// only after it is approved here. The server enforces the admin role.
function ReviewPage() {

    const { user } = useAuth();

    const [status, setStatus] =
        useState("pending");

    const [loaded, setLoaded] =
        useState(null);

    const [error, setError] =
        useState(null);

    const [busyId, setBusyId] =
        useState(null);

    const isAdmin = user?.role === "ADMIN";


    useEffect(() => {

        if (!isAdmin) {
            return;
        }

        let cancelled = false;

        getClaimsForReview(status)
            .then((data) => {
                if (!cancelled) {
                    setError(null);
                    setLoaded({ status, data });
                }
            })
            .catch((loadError) => {
                console.error(loadError);
                if (!cancelled) {
                    setError("Unable to load claims.");
                }
            });

        return () => {
            cancelled = true;
        };

    }, [status, isAdmin]);


    const claims =
        loaded?.status === status
            ? loaded.data
            : null;


    async function decide(claim, decision) {

        try {
            setBusyId(claim.claimId);
            await reviewClaim(claim.claimId, decision);
            // A decided claim leaves the pending list.
            setLoaded({ status, data: claims.filter((c) => c.claimId !== claim.claimId) });
        } catch (reviewError) {
            console.error(reviewError);
            setError("The review could not be saved. Please try again.");
        } finally {
            setBusyId(null);
        }
    }


    return (
        <div className="app">

            <Header />

            <main className="container">

                <Link to="/" className="back-button">
                    ← Back to news
                </Link>

                <section className="news-section">

                    <div className="news-section-header">
                        <div>
                            <span className="section-eyebrow">
                                REVIEW
                            </span>
                            <h2>
                                Numeric claims
                            </h2>
                        </div>
                    </div>

                    {!isAdmin ? (
                        <div className="empty-state">
                            <h3>Admins only</h3>
                            <p>Claims are reviewed by administrators before they appear.</p>
                        </div>
                    ) : (
                        <>
                            <div className="review-tabs" role="tablist">
                                {STATUSES.map((s) => (
                                    <button
                                        key={s}
                                        role="tab"
                                        aria-selected={s === status}
                                        className={s === status ? "review-tab active" : "review-tab"}
                                        onClick={() => setStatus(s)}
                                    >
                                        {s}
                                    </button>
                                ))}
                            </div>

                            {error && (
                                <div className="inline-error">{error}</div>
                            )}

                            {claims === null ? (
                                <div className="skeleton skeleton-card" />
                            ) : claims.length === 0 ? (
                                <div className="empty-state">
                                    <h3>Nothing here</h3>
                                    <p>No {status} claims.</p>
                                </div>
                            ) : (
                                <div className="review-list">
                                    {claims.map((claim) => (
                                        <article className="review-card" key={claim.claimId}>

                                            <div className="review-claim">
                                                {claimText(claim)}
                                            </div>

                                            <blockquote className="review-quote">
                                                “{claim.quote}”
                                            </blockquote>

                                            <div className="source-count">
                                                {claim.outlet} · {claim.field} ·{" "}
                                                <a href={claim.articleUrl} target="_blank" rel="noreferrer">
                                                    {claim.articleTitle}
                                                </a>
                                                {claim.eventId && (
                                                    <>
                                                        {" · "}
                                                        <Link to={`/events/${claim.eventId}`}>event</Link>
                                                    </>
                                                )}
                                                {" · "}
                                                {claim.supportingArticles} agree, {claim.conflictingArticles} conflict
                                            </div>

                                            {status === "pending" && (
                                                <div className="review-actions">
                                                    <button
                                                        className="analyze-button"
                                                        disabled={busyId === claim.claimId}
                                                        onClick={() => decide(claim, "approved")}
                                                    >
                                                        Approve
                                                    </button>
                                                    <button
                                                        className="logout-button"
                                                        disabled={busyId === claim.claimId}
                                                        onClick={() => decide(claim, "rejected")}
                                                    >
                                                        Reject
                                                    </button>
                                                </div>
                                            )}

                                        </article>
                                    ))}
                                </div>
                            )}
                        </>
                    )}

                </section>

            </main>

        </div>
    );
}

export default ReviewPage;
