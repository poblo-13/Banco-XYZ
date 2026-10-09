package com.bancoxyz.account.service;

import com.bancoxyz.account.entity.Cuenta;
import com.bancoxyz.account.repository.CuentaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cuenta no encontrada: " + cuentaId
                ));
    }

    @Transactional
    public Cuenta guardar(Cuenta cuenta) {
        return cuentaRepository.save(cuenta);
    }
}