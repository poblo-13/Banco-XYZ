package com.bancoxyz.batch.repository;

import com.bancoxyz.batch.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransaccionRepository extends JpaRepository<Transaccion, Long> {
}