package com.bancoxyz.batch.bff.web;

import com.bancoxyz.batch.repository.InteresCalculadoRepository;
import com.bancoxyz.batch.repository.MovimientoAnualRepository;
import com.bancoxyz.batch.repository.TransaccionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/web")
public class WebBffController {

    private final TransaccionRepository transaccionRepository;
    private final InteresCalculadoRepository interesRepository;
    private final MovimientoAnualRepository movimientoRepository;

    public WebBffController(
            TransaccionRepository transaccionRepository,
            InteresCalculadoRepository interesRepository,
            MovimientoAnualRepository movimientoRepository) {

        this.transaccionRepository = transaccionRepository;
        this.interesRepository = interesRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("canal", "WEB");
        response.put("descripcion",
                "Respuesta completa optimizada para navegador");

        response.put("transacciones",
                transaccionRepository.findAll());

        response.put("intereses",
                interesRepository.findAll());

        response.put("movimientosAnuales",
                movimientoRepository.findAll());

        return response;
    }
}