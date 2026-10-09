package com.bancoxyz.bff.atm.service;

import com.bancoxyz.bff.atm.client.CoreApiClient;
import com.bancoxyz.bff.atm.client.dto.CoreCuentaResponse;
import com.bancoxyz.bff.atm.client.dto.CoreMovimientoResponse;
import com.bancoxyz.bff.atm.dto.AtmRetiroRequest;
import com.bancoxyz.bff.atm.dto.AtmRetiroResponse;
import com.bancoxyz.bff.atm.dto.AtmSaldoResponse;
import com.bancoxyz.bff.atm.exception.AtmCoreNoDisponibleException;
import org.springframework.stereotype.Service;

@Service
public class AtmService {

    private final CoreApiClient coreApiClient;

    public AtmService(CoreApiClient coreApiClient) {
        this.coreApiClient = coreApiClient;
    }

    public AtmSaldoResponse consultarSaldo(Long cuentaId) {

        CoreCuentaResponse cuenta =
                coreApiClient.obtenerCuenta(cuentaId);

        return new AtmSaldoResponse(
                cuenta.cuentaId(),
                cuenta.saldoActual()
        );
    }

    public AtmRetiroResponse retirar(AtmRetiroRequest request) {

        CoreMovimientoResponse movimiento =
                coreApiClient.realizarRetiro(
                        request.operacionId().toString(),
                        request.cuentaId(),
                        request.monto()
                );

        if (movimiento == null ||
                !"CONFIRMADO".equals(movimiento.estado()) ||
                movimiento.saldoResultante() == null) {
            throw new AtmCoreNoDisponibleException(
                    "No fue posible confirmar el retiro"
            );
        }

        return new AtmRetiroResponse(
                request.cuentaId(),
                request.monto(),
                movimiento.saldoResultante(),
                "APROBADO"
        );
    }
}