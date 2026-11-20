package com.library.payload.response;

/**
 * { "isInWishlist": true }
 * A record (not a Lombok class) so the JSON key is exactly "isInWishlist": with a Lombok
 * boolean getter, Jackson would also add a second key "inWishlist".
 */
public record WishlistCheckResponse(boolean isInWishlist) {
}
