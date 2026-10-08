export function verificationInfo(status) {

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


export const AI_ESTIMATE_NOTE =
    "AI estimate, not a fact-check";


// The AI-written title when there is one; otherwise the first headline.
export function eventTitle(event) {
    return event.generatedTitle || event.title;
}

export const INDEPENDENCE_NOTE =
    "Outlets are counted by name. Copies of the same agency story (PTI, ANI) count as separate outlets.";


const RELATIVE_UNITS = [
    ["year", 60 * 60 * 24 * 365],
    ["month", 60 * 60 * 24 * 30],
    ["week", 60 * 60 * 24 * 7],
    ["day", 60 * 60 * 24],
    ["hour", 60 * 60],
    ["minute", 60]
];

const relativeFormatter =
    new Intl.RelativeTimeFormat(
        "en",
        { numeric: "auto" }
    );


export function formatRelativeTime(dateValue) {

    if (!dateValue) {
        return null;
    }

    const date = new Date(dateValue);

    if (Number.isNaN(date.getTime())) {
        return null;
    }

    const secondsAgo =
        (Date.now() - date.getTime()) / 1000;

    for (const [unit, seconds] of RELATIVE_UNITS) {

        if (Math.abs(secondsAgo) >= seconds) {
            return relativeFormatter.format(
                -Math.round(secondsAgo / seconds),
                unit
            );
        }
    }

    return "just now";
}
