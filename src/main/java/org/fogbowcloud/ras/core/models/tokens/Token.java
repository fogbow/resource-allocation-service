package org.fogbowcloud.ras.core.models.tokens;

public class Token {
    private String tokenValue;

    public Token() {
    }

    public Token(String tokenValue) {
        this.tokenValue = tokenValue;
    }

    public String getTokenValue() {
        return this.tokenValue;
    }
}
