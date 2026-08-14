function getVerificationStatus(status) {

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


function getRiskLevel(risk) {

    if (risk == null) {
        return null;
    }

    if (risk < 0.3) {
        return {
            label: "Low risk",
            className: "risk-low"
        };
    }

    if (risk < 0.7) {
        return {
            label: "Moderate risk",
            className: "risk-medium"
        };
    }

    return {
        label: "High risk",
        className: "risk-high"
    };
}


function EventCard({ event, onClick }) {

    const verification =
        getVerificationStatus(
            event.verificationStatus
        );

    const risk =
        getRiskLevel(
            event.misinformationRisk
        );

    const sourceCount =
        event.articles?.length || 0;


    return (
        <article
            className="event-card"
            onClick={onClick}
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
                    <span className="metric">
                        <span className="metric-label">
                            Disagreement
                        </span>

                        <strong>
                            {event.disagreementLevel}
                        </strong>
                    </span>
                )}


                {risk && (
                    <span className="metric">
                        <span className="metric-label">
                            AI risk
                        </span>

                        <strong
                            className={risk.className}
                        >
                            {Math.round(
                                event.misinformationRisk * 100
                            )}%
                        </strong>
                    </span>
                )}


                <span className="view-event">
                    View analysis →
                </span>

            </div>

        </article>
    );
}

export default EventCard;