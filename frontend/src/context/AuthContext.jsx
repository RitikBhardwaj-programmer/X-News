import {
    createContext,
    useContext,
    useEffect,
    useState
} from "react";

import {
    login as apiLogin,
    register as apiRegister,
    getCurrentUser
} from "../services/api";


const AuthContext = createContext(null);


export function AuthProvider({ children }) {

    const [token, setToken] = useState(
        () => localStorage.getItem("xnews_token")
    );

    const [user, setUser] = useState(null);

    const [loading, setLoading] = useState(true);


    useEffect(() => {

        async function restoreSession() {

            if (!token) {
                setLoading(false);
                return;
            }

            try {

                const currentUser =
                    await getCurrentUser(token);

                setUser(currentUser);

            } catch (error) {

                console.error(
                    "Session restoration failed:",
                    error
                );

                localStorage.removeItem("xnews_token");
                setToken(null);
                setUser(null);

            } finally {

                setLoading(false);
            }
        }

        restoreSession();

    }, [token]);


    async function login(email, password) {

        const data =
            await apiLogin(email, password);

        localStorage.setItem(
            "xnews_token",
            data.token
        );

        setToken(data.token);

        const currentUser =
            await getCurrentUser(data.token);

        setUser(currentUser);

        return currentUser;
    }


    async function register(
        name,
        email,
        password
    ) {

        const data =
            await apiRegister(
                name,
                email,
                password
            );

        localStorage.setItem(
            "xnews_token",
            data.token
        );

        setToken(data.token);

        const currentUser =
            await getCurrentUser(data.token);

        setUser(currentUser);

        return currentUser;
    }


    function logout() {

        localStorage.removeItem(
            "xnews_token"
        );

        setToken(null);
        setUser(null);
    }


    return (
        <AuthContext.Provider
            value={{
                token,
                user,
                loading,
                isAuthenticated: !!token && !!user,
                login,
                register,
                logout
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}


export function useAuth() {

    const context =
        useContext(AuthContext);

    if (!context) {
        throw new Error(
            "useAuth must be used inside AuthProvider"
        );
    }

    return context;
}