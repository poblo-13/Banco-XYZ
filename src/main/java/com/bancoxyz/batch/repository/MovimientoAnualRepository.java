package com.bancoxyz.batch.repository;

import com.bancoxyz.batch.model.MovimientoAnual;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoAnualRepository
        extends JpaRepository<MovimientoAnual, Long> {

    List<MovimientoAnual> findAllByOrderByCuentaIdAscFechaAsc();
}