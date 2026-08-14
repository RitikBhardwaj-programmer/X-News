const API_BASE_URL = "http://localhost:8080/api/v1";


export async function getEvents() {

    const response = await fetch(
        `${API_BASE_URL}/events`
    );

    if (!response.ok) {
        throw new Error(
            `Failed to fetch events (${response.status})`
        );
    }

    return response.json();
}


export async function getEvent(id) {

    const response = await fetch(
        `${API_BASE_URL}/events/${id}`
    );

    if (!response.ok) {
        throw new Error(
            `Failed to fetch event (${response.status})`
        );
    }

    return response.json();
}


export async function analyzeEvent(id) {

    const response = await fetch(
        `${API_BASE_URL}/events/${id}/analyze`,
        {
            method: "POST"
        }
    );

    if (!response.ok) {
        const text = await response.text();

        throw new Error(
            `AI analysis failed (${response.status}): ${text}`
        );
    }

    return response.json();
}