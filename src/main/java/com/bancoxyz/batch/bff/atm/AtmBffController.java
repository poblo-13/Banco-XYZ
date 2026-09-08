package com.bancoxyz.batch.bff.atm;

import com.bancoxyz.batch.repository.InteresCalculadoRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/atm")
public class AtmBffController {

    private final InteresCalculadoRepository interesRepository;

    public AtmBffController(
            InteresCalculadoRepository interesRepository) {
        this.interesRepository = interesRepository;
    }

    @GetMapping("/saldo")
    public ResponseEntity<Map<String, Object>> consultarSaldo() {

        var cuentas = interesRepository.findAll();

        if (cuentas.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("canal", "ATM");
        response.put("operacion", "CONSULTA_SALDO");
        response.put("descripcion",
                "Respuesta específica para operación de cajero");
        response.put("cuenta", cuentas.get(0));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/retiro")
    public ResponseEntity<Map<String, Object>> retiro(
            @RequestBody Map<String, Object> solicitud) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("canal", "ATM");
        response.put("operacion", "RETIRO");
        response.put("estado", "SOLICITUD_RECIBIDA");
        response.put("datos", solicitud);

        return ResponseEntity.ok(response);
    }
}