const API_BASE_URL =
    import.meta.env.VITE_API_URL;


function authHeaders() {

    const token =
        localStorage.getItem("xnews_token");

    return token
        ? {
            Authorization: `Bearer ${token}`
        }
        : {};
}


async function handleResponse(response) {

    if (!response.ok) {

        const text =
            await response.text();

        throw new Error(
            text ||
            `Request failed (${response.status})`
        );
    }

    return response.json();
}


export async function login(
    email,
    password
) {

    const response =
        await fetch(
            `${API_BASE_URL}/auth/login`,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/json"
                },

                body: JSON.stringify({
                    email,
                    password
                })
            }
        );

    return handleResponse(response);
}


export async function register(
    name,
    email,
    password
) {

    const response =
        await fetch(
            `${API_BASE_URL}/auth/register`,
            {
                method: "POST",

                headers: {
                    "Content-Type":
                        "application/json"
                },

                body: JSON.stringify({
                    name,
                    email,
                    password
                })
            }
        );

    return handleResponse(response);
}


export async function getCurrentUser(token) {

    const response =
        await fetch(
            `${API_BASE_URL}/users/me`,
            {
                headers: {
                    Authorization:
                        `Bearer ${token}`
                }
            }
        );

    return handleResponse(response);
}


export async function getEvents() {

    const response =
        await fetch(
            `${API_BASE_URL}/events`,
            {
                headers: {
                    ...authHeaders()
                }
            }
        );

    return handleResponse(response);
}


export async function getEvent(id) {

    const response =
        await fetch(
            `${API_BASE_URL}/events/${id}`,
            {
                headers: {
                    ...authHeaders()
                }
            }
        );

    return handleResponse(response);
}


export async function analyzeEvent(id) {

    const response =
        await fetch(
            `${API_BASE_URL}/events/${id}/analyze`,
            {
                method: "POST",

                headers: {
                    ...authHeaders()
                }
            }
        );

    return handleResponse(response);
}