package com.bancoxyz.core.service;

import com.bancoxyz.core.entity.Cuenta;
import com.bancoxyz.core.exception.RecursoNoEncontradoException;
import com.bancoxyz.core.repository.CuentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CuentaService {

    private final CuentaRepository cuentaRepository;

    public CuentaService(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Transactional(readOnly = true)
    public List<Cuenta> listarTodas() {
        return cuentaRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Cuenta obtenerPorId(Long cuentaId) {
        return cuentaRepository.findById(cuentaId)
                .orElseThrow(() ->
                        new RecursoNoEncontradoException(
                                "Cuenta no encontrada: " + cuentaId
                        )
                );
    }

    @Transactional
    public Cuenta guardar(Cuenta cuenta) {
        return cuentaRepository.save(cuenta);
    }
}