package com.bancoxyz.account.repository;

import com.bancoxyz.account.entity.OperacionCuenta;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperacionCuentaRepository
        extends JpaRepository<OperacionCuenta, String> {
}