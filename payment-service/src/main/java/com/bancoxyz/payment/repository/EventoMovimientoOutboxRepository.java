package com.bancoxyz.payment.repository;

import com.bancoxyz.payment.entity.EventoMovimientoOutbox;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;

public interface EventoMovimientoOutboxRepository
        extends JpaRepository<EventoMovimientoOutbox, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<EventoMovimientoOutbox>
            findTop10ByEstadoOrderByCreadoEnAsc(String estado);
}
