package com.bancoxyz.payment.repository;

import com.bancoxyz.payment.entity.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MovimientoRepository
        extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByCuentaIdOrderByFechaDescIdDesc(Long cuentaId);

    List<Movimiento> findByCuentaIdAndEstadoOrderByFechaDescIdDesc(
            Long cuentaId,
            Movimiento.Estado estado
    );

    Optional<Movimiento> findByOperacionId(String operacionId);

    List<Movimiento> findByEstado(Movimiento.Estado estado);
}