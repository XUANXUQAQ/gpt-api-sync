package github.gpt.api.sync.config;

import lombok.Getter;

public enum AuthHeaderType {
    NEW_API("New-Api-User"),
    VELOERA("Veloera-User");

    @Getter
    private final String headerName;

    AuthHeaderType(String headerName) {
        this.headerName = headerName;
    }
}
