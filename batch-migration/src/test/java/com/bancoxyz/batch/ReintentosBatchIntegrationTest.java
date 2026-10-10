package com.bancoxyz.batch;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
    classes = {
        BatchMigrationApplication.class,
        ReintentosBatchIntegrationTest.Configuracion.class
    },
    properties = {
        "spring.batch.jdbc.initialize-schema=always",
        "spring.batch.job.enabled=false",
        "spring.main.web-application-type=none"
    }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReintentosBatchIntegrationTest {

    private static final Path DATOS = prepararCsv();

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private JobOperator operador;

    @Autowired
    @Qualifier("movimientosJob")
    private Job movimientos;

    @Autowired
    @Qualifier("interesesJob")
    private Job intereses;

    @Autowired
    @Qualifier("estadosJob")
    private Job estados;

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("legacy.data.root", () -> DATOS.toString());
    }

    private static Path prepararCsv() {
        try {
            Path raiz = Files.createTempDirectory("batch-xyz-test-");

            for (int semana = 1; semana <= 3; semana++) {
                Path carpeta = Files.createDirectories(
                    raiz.resolve("semana_" + semana)
                );

                Files.writeString(
                    carpeta.resolve("movimientos_financieros_diarios.csv"),
                    "id,fecha,monto,tipo\n"
                    + semana + ",2024-01-01,100.00,credito\n"
                );

                Files.writeString(
                    carpeta.resolve("intereses_trimestrales.csv"),
                    "cuenta,nombre,saldo,edad,tipo\n"
                    + (100 + semana)
                    + ",Ana Perez,1000.00,35,ahorro\n"
                );

                Files.writeString(
                    carpeta.resolve("estados_financieros_anuales.csv"),
                    "cuenta,fecha,tipo,monto,descripcion\n"
                    + (200 + semana)
                    + ",2024-01-01,deposito,100.00,Prueba\n"
                );
            }

            return raiz;

        } catch (Exception error) {
            throw new IllegalStateException(
                "No fue posible preparar datos de prueba", error
            );
        }
    }

    @BeforeAll
    void crearTablasAisladas() {

        jdbc.execute("""
            CREATE TABLE legacy_movimientos (
                semana VARCHAR(20) NOT NULL,
                linea INT NOT NULL,
                id_origen BIGINT,
                fecha DATE,
                monto DECIMAL(18,2),
                tipo VARCHAR(30),
                estado VARCHAR(30),
                motivo VARCHAR(255),
                texto_original VARCHAR(1000),
                PRIMARY KEY (semana, linea)
            )
            """);

        jdbc.execute("""
            CREATE TABLE legacy_intereses (
                semana VARCHAR(20) NOT NULL,
                linea INT NOT NULL,
                cuenta_id BIGINT,
                nombre VARCHAR(255),
                saldo DECIMAL(18,2),
                edad INT,
                tipo VARCHAR(30),
                interes_calculado DECIMAL(18,2),
                estado VARCHAR(30),
                motivo VARCHAR(255),
                texto_original VARCHAR(1000),
                PRIMARY KEY (semana, linea)
            )
            """);

        jdbc.execute("""
            CREATE TABLE legacy_estados (
                semana VARCHAR(20) NOT NULL,
                linea INT NOT NULL,
                cuenta_id BIGINT,
                fecha DATE,
                transaccion VARCHAR(30),
                monto DECIMAL(18,2),
                descripcion VARCHAR(255),
                estado VARCHAR(30),
                motivo VARCHAR(255),
                texto_original VARCHAR(1000),
                PRIMARY KEY (semana, linea)
            )
            """);
    }

    @Test
    void movimientosSeRecuperaDeFalloTransitorio() throws Exception {
        comprobar(
            movimientos,
            "legacy_movimientos",
            Configuracion.fallosMovimientos
        );
    }

    @Test
    void interesesSeRecuperaDeFalloTransitorio() throws Exception {
        comprobar(
            intereses,
            "legacy_intereses",
            Configuracion.fallosIntereses
        );
    }

    @Test
    void estadosSeRecuperaDeFalloTransitorio() throws Exception {
        comprobar(
            estados,
            "legacy_estados",
            Configuracion.fallosEstados
        );
    }

    private void comprobar(
            Job job,
            String tabla,
            AtomicInteger fallos) throws Exception {

        JobExecution ejecucion = operador.start(
            job,
            new JobParametersBuilder()
                .addString("prueba", UUID.randomUUID().toString())
                .toJobParameters()
        );

        assertEquals(
            BatchStatus.COMPLETED,
            ejecucion.getStatus(),
            "El job no se recupero del fallo"
        );

        assertEquals(
            1,
            fallos.get(),
            "No se inyecto el fallo transitorio esperado"
        );

        Integer registros = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + tabla,
            Integer.class
        );

        assertEquals(
            3,
            registros.intValue(),
            "No se guardaron las tres semanas"
        );

        System.out.println(
            "RECUPERACION EXITOSA: " + job.getName()
            + " | fallo simulado=1 | registros=3"
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuracion {

        static final AtomicInteger fallosMovimientos =
            new AtomicInteger();

        static final AtomicInteger fallosIntereses =
            new AtomicInteger();

        static final AtomicInteger fallosEstados =
            new AtomicInteger();

        @Bean
        @Primary
        DataSource dataSource() {

            DriverManagerDataSource original =
                new DriverManagerDataSource(
                    "jdbc:h2:mem:bancoxyz_batch_retry;"
                    + "MODE=MySQL;DB_CLOSE_DELAY=-1",
                    "sa",
                    ""
                );

            return new DelegatingDataSource(original) {

                @Override
                public Connection getConnection() throws SQLException {
                    return envolver(super.getConnection());
                }

                @Override
                public Connection getConnection(
                        String usuario,
                        String clave) throws SQLException {

                    return envolver(
                        super.getConnection(usuario, clave)
                    );
                }
            };
        }

        private static Connection envolver(Connection conexion) {

            return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {

                    Object respuesta = invocar(
                        conexion, metodo, argumentos
                    );

                    if (!metodo.getName().equals("prepareStatement")
                            || argumentos == null
                            || !(argumentos[0] instanceof String sql)) {
                        return respuesta;
                    }

                    String texto = sql.toLowerCase(Locale.ROOT);
                    AtomicInteger contador = null;

                    if (texto.contains("insert into legacy_movimientos")) {
                        contador = fallosMovimientos;
                    } else if (
                        texto.contains("insert into legacy_intereses")
                    ) {
                        contador = fallosIntereses;
                    } else if (
                        texto.contains("insert into legacy_estados")
                    ) {
                        contador = fallosEstados;
                    }

                    if (contador == null) {
                        return respuesta;
                    }

                    PreparedStatement sentencia =
                        (PreparedStatement) respuesta;

                    AtomicInteger fallaActual = contador;

                    return Proxy.newProxyInstance(
                        PreparedStatement.class.getClassLoader(),
                        new Class<?>[]{PreparedStatement.class},
                        (proxySentencia, metodoSentencia, argumentosSentencia) -> {

                            if (metodoSentencia.getName().equals("executeUpdate")
                                    && fallaActual.compareAndSet(0, 1)) {

                                throw new SQLTransientConnectionException(
                                    "Fallo transitorio simulado",
                                    "08006"
                                );
                            }

                            return invocar(
                                sentencia,
                                metodoSentencia,
                                argumentosSentencia
                            );
                        }
                    );
                }
            );
        }

        private static Object invocar(
                Object destino,
                Method metodo,
                Object[] argumentos) throws Throwable {

            try {
                return metodo.invoke(destino, argumentos);
            } catch (InvocationTargetException error) {
                throw error.getCause();
            }
        }
    }
}
