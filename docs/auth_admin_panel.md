# Auth Admin Panel

The auth system's admin panel runs on http://localhost:80/auth/admin. The username is `admin`, and the password is the value of `KEYCLOAK_PASSWORD` in the `.env` file. Note that users must only be created and deleted via Omni Chat's API because custom user data gets stored on a DB separate from the auth system's.