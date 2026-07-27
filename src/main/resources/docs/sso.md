# Configuring SSO in Acme ERP

Acme ERP supports SAML 2.0 and OpenID Connect for single sign-on.

## OpenID Connect (recommended)

1. In the admin console, open **Settings → Authentication → Single sign-on**.
2. Choose *OpenID Connect* and enter the discovery URL from your identity provider,
   for example `https://login.example.com/.well-known/openid-configuration`.
3. Paste the client ID and client secret issued by the provider.
4. Map the `email` claim to the Acme ERP username field. Unmapped users are rejected.
5. Click **Test connection**, then **Enable**.

Existing local passwords stop working the moment SSO is enabled. Keep one
break-glass local administrator account with `sso.exempt = true`.

## SAML 2.0

Upload the IdP metadata XML on the same screen. Acme ERP's entity ID is
`https://<your-host>/saml/metadata` and the ACS endpoint is `/saml/SSO`.
Clock skew above 90 seconds causes assertion validation to fail — sync NTP first.

## Troubleshooting

- **"Invalid audience"** — the entity ID in the IdP does not match the value above.
- **Users land on a blank screen after login** — the `email` claim is missing from the token.
- **Loop between Acme and the IdP** — session cookies are being dropped; check that
  `acme.session.same-site` is `Lax` when the IdP is on a different domain.
