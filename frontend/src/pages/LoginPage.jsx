import { useState } from "react";
import { useAuth } from "../context/AuthContext";


function LoginPage({ onRegister }) {

    const {
        login
    } = useAuth();


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

            await login(
                email,
                password
            );

        } catch (error) {

            console.error(error);

            setError(
                "Invalid email or password."
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
                    Welcome back
                </h1>

                <p className="auth-description">
                    Sign in to continue to X-NEWS.
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
                            required
                        />
                    </label>


                    <button
                        type="submit"
                        className="analyze-button"
                        disabled={submitting}
                    >
                        {submitting
                            ? "Signing in..."
                            : "Sign in"}
                    </button>

                </form>


                <p className="auth-switch">
                    Don't have an account?

                    <button
                        onClick={onRegister}
                    >
                        Create account
                    </button>
                </p>

            </div>

        </div>
    );
}

export default LoginPage;