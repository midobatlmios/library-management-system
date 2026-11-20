package com.library.service;

import com.library.exception.LibraryException;
import com.library.payload.dto.ReservationDTO;
import com.library.payload.request.ReservationRequest;
import com.library.payload.request.ReservationSearchRequest;
import com.library.payload.response.PagesResponse;
import com.library.payload.response.QueuePositionResponse;

public interface ReservationService {

    /** userId null = the logged-in user reserves; otherwise an admin places the hold for that user. */
    ReservationDTO create(ReservationRequest request, Long userId) throws LibraryException;

    ReservationDTO cancel(Long reservationId, String reason) throws LibraryException;

    /** Borrows the held copy: creates the loan and marks the reservation FULFILLED. */
    ReservationDTO fulfill(Long reservationId) throws LibraryException;

    ReservationDTO getById(Long reservationId) throws LibraryException;

    QueuePositionResponse getQueuePosition(Long reservationId) throws LibraryException;

    /** Logged-in user's reservations (request.userId is ignored). */
    PagesResponse<ReservationDTO> searchMine(ReservationSearchRequest request) throws LibraryException;

    /** Admin: all reservations. */
    PagesResponse<ReservationDTO> searchAll(ReservationSearchRequest request) throws LibraryException;

    /** Held copies not picked up in time -> EXPIRED, and the copy moves on. Returns how many expired. */
    int expireUncollected();
}
