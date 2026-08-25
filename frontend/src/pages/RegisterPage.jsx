import { useState } from "react";
import { useAuth } from "../context/AuthContext";


function RegisterPage({ onLogin }) {

    const {
        register
    } = useAuth();


    const [name, setName] =
        useState("");

    const [email, setEmail] =
        useState("");

    const [password, setPassword] =
        useState("");

    const [error, setError] =
        useState(null);

    const [submitting, setSubmitting] =
        useState(false);


    async function handleSubmit(event) {

        event.preventDefault();

        try {

            setSubmitting(true);
            setError(null);

            await register(
                name,
                email,
                password
            );

        } catch (error) {

            console.error(error);

            setError(
                "Unable to create account."
            );

        } finally {

            setSubmitting(false);
        }
    }


    return (
        <div className="auth-page">

            <div className="auth-card">

                <div className="logo">
                    X-NEWS
                </div>

                <p className="auth-eyebrow">
                    NEWS INTELLIGENCE
                </p>

                <h1>
                    Create account
                </h1>

                <p className="auth-description">
                    Start using X-NEWS.
                </p>


                {error && (
                    <div className="inline-error">
                        {error}
                    </div>
                )}


                <form
                    onSubmit={handleSubmit}
                    className="auth-form"
                >

                    <label>
                        Name

                        <input
                            type="text"
                            value={name}
                            onChange={(e) =>
                                setName(e.target.value)
                            }
                            required
                        />
                    </label>


                    <label>
                        Email

                        <input
                            type="email"
                            value={email}
                            onChange={(e) =>
                                setEmail(e.target.value)
                            }
                            required
                        />
                    </label>


                    <label>
                        Password

                        <input
                            type="password"
                            value={password}
                            onChange={(e) =>
                                setPassword(e.target.value)
                            }
                            minLength={8}
                            required
                        />
                    </label>


                    <button
                        type="submit"
                        className="analyze-button"
                        disabled={submitting}
                    >
                        {submitting
                            ? "Creating..."
                            : "Create account"}
                    </button>

                </form>


                <p className="auth-switch">
                    Already have an account?

                    <button
                        onClick={onLogin}
                    >
                        Sign in
                    </button>
                </p>

            </div>

        </div>
    );
}

export default RegisterPage;