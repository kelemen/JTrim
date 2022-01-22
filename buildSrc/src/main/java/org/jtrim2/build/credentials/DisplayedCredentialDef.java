package org.jtrim2.build.credentials;

import java.util.Objects;

public final class DisplayedCredentialDef {
    private final Object id;
    private final String displayName;
    private final CredentialType credentialType;

    public DisplayedCredentialDef(
            Object id,
            String displayName,
            CredentialType credentialType) {

        this.id = Objects.requireNonNull(id, "id");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.credentialType = Objects.requireNonNull(credentialType, "credentialType");
    }

    public Object getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CredentialType getCredentialType() {
        return credentialType;
    }
}
