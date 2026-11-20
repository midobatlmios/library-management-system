package com.library.configirations;

/**
 * Only the header name lives here. The signing secret is NOT in the code any more:
 * it comes from secrets.properties or the JWT_SECRET environment variable (see JwtProvider).
 */
public final class JwtConstant {
    public static final String JWT_HEADER = "Authorization";

    private JwtConstant() {
    }
}
