import { Link } from "react-router";

import {
    verificationInfo,
    riskInfo,
    formatRelativeTime,
    AI_ESTIMATE_NOTE
} from "../utils/eventLabels";


function EventCard({ event }) {

    const verification =
        verificationInfo(
            event.verificationStatus
        );

    const risk =
        riskInfo(
            event.misinformationRisk
        );

    const sourceCount =
        event.sourceCount || 0;

    const age =
        formatRelativeTime(
            event.createdAt
        );


    return (
        <Link
            to={`/events/${event.id}`}
            className="event-card"
        >

            <div className="event-card-top">

                <span
                    className={`status-pill ${verification.className}`}
                >
                    <span className="status-icon">
                        {verification.icon}
                    </span>

                    {verification.label}
                </span>

                <span className="source-count">
                    {sourceCount}{" "}
                    {sourceCount === 1
                        ? "source"
                        : "sources"}

                    {age && (
                        <>
                            {" · "}
                            <time dateTime={event.createdAt}>
                                {age}
                            </time>
                        </>
                    )}
                </span>

            </div>


            <h2 className="event-card-title">
                {event.title}
            </h2>


            {event.description && (
                <p className="event-card-description">
                    {event.description}
                </p>
            )}


            <div className="event-card-bottom">

                {event.disagreementLevel && (
                    <span
                        className="metric"
                        title={AI_ESTIMATE_NOTE}
                    >
                        <span className="metric-label">
                            Disagreement
                        </span>

                        <strong>
                            {event.disagreementLevel}
                        </strong>
                    </span>
                )}


                {risk && (
                    <span
                        className="metric"
                        title={AI_ESTIMATE_NOTE}
                    >
                        <span className="metric-label">
                            AI risk
                        </span>

                        <strong
                            className={risk.className}
                        >
                            {risk.value}
                        </strong>
                    </span>
                )}


                <span className="view-event">
                    View analysis →
                </span>

            </div>

        </Link>
    );
}

export default EventCard;
