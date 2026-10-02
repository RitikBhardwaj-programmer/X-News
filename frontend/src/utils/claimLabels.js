// Readable predicates for numeric claims (V4 roadmap step 6).
const PREDICATES = {
    innings_score: "innings score",
    all_out_for: "all out for",
    chased: "chased",
    set_target: "set a target of",
    runs_scored: "scored",
    bowling_figures: "bowling figures",
    won_by: "won by",
    lost_by: "lost by"
};

export function claimText(claim) {

    return `${claim.subject} ${PREDICATES[claim.predicate] || claim.predicate} ${claim.valueText}`;
}
