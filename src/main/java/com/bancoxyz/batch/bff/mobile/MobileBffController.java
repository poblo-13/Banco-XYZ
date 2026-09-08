package com.bancoxyz.batch.bff.mobile;

import com.bancoxyz.batch.repository.InteresCalculadoRepository;
import com.bancoxyz.batch.repository.MovimientoAnualRepository;
import com.bancoxyz.batch.repository.TransaccionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mobile")
public class MobileBffController {

    private final TransaccionRepository transaccionRepository;
    private final InteresCalculadoRepository interesRepository;
    private final MovimientoAnualRepository movimientoRepository;

    public MobileBffController(
            TransaccionRepository transaccionRepository,
            InteresCalculadoRepository interesRepository,
            MovimientoAnualRepository movimientoRepository) {

        this.transaccionRepository = transaccionRepository;
        this.interesRepository = interesRepository;
        this.movimientoRepository = movimientoRepository;
    }

    @GetMapping("/resumen")
    public Map<String, Object> resumen() {

        var transacciones = transaccionRepository.findAll();

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("canal", "MOBILE");
        response.put("descripcion",
                "Respuesta ligera con información esencial");

        response.put("totalTransacciones",
                transacciones.size());

        response.put("totalCuentas",
                interesRepository.count());

        response.put("movimientosAnuales",
                movimientoRepository.count());

        response.put("transaccionesRecientes",
                transacciones.stream()
                        .limit(3)
                        .toList());

        return response;
    }
}