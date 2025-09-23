package org.example;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
import org.example.modelos.Autor;
import org.example.modelos.Genero;
import org.example.modelos.Libro;
import org.example.repositorios.AutorRepository;
import org.example.repositorios.GeneroRepository;
import org.example.repositorios.LibroRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("bibliotecaPU");
        EntityManager em = emf.createEntityManager();

        try {
            AutorRepository autorRepo = new AutorRepository(em);
            GeneroRepository generoRepo = new GeneroRepository(em);
            LibroRepository libroRepo = new LibroRepository(em);

            // 1) Transacción para cargar géneros
            em.getTransaction().begin();
            List<String> generosCsv = leerColumnaUnica("data/genres.csv", "name");
            for (String nombre : generosCsv) {
                if (!nombre.isBlank()) generoRepo.getOrCreate(nombre.trim());
            }
            em.getTransaction().commit();

            // 2) Transacción para cargar autores
            em.getTransaction().begin();
            List<String> autoresCsv = leerColumnaUnica("data/authors.csv", "name");
            for (String nombre : autoresCsv) {
                if (!nombre.isBlank()) autorRepo.getOrCreate(nombre.trim());
            }
            em.getTransaction().commit();

            // 3) Transacción para cargar libros (y vincular géneros)
            em.getTransaction().begin();
            List<Map<String, String>> libros = leerLibros("data/books.csv");
            for (Map<String, String> fila : libros) {
                String titulo = fila.get("title").trim();
                String autorNombre = fila.get("author").trim();
                String generos = fila.get("genres").trim();

                Autor autor = autorRepo.getOrCreate(autorNombre);
                Libro libro = libroRepo.getOrCreate(titulo, autor);

                Set<String> nombresGeneros = Arrays.stream(generos.split(";"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                for (String g : nombresGeneros) {
                    Genero genero = generoRepo.getOrCreate(g);
                    libro.addGenero(genero);
                }
            }
            em.getTransaction().commit();

            // 4) Comprobación con JDBC puro (conteos)
            verificarConJDBC();

            System.out.println("\nCarga finalizada.");

            // 5) Recuperar todos los libros de "Jorge Luis Borges"
            List<Libro> librosBorges = new LibroRepository(em).findByAutorNombre("Jorge Luis Borges");

            System.out.println("\nLibros de Jorge Luis Borges (vía JPA):");
            for (Libro l : librosBorges) {
                String generos = l.getGeneros().stream()
                        .map(Genero::getNombre)
                        .collect(Collectors.joining(";"));
                System.out.printf(" - %s | %s | %s%n", l.getTitulo(), l.getAutor().getNombre(), generos);
            }
        } catch (Exception e) {
            System.err.println("Error durante la carga: " + e.getMessage());
            e.printStackTrace();
        } finally {
            em.close();
            emf.close();
        }
    }

    // --- Utilidades de lectura CSV (sin librerías externas) ---

    private static List<String> leerColumnaUnica(String resourcePath, String headerEsperado) throws IOException {
        List<String> valores = new ArrayList<>();
        try (BufferedReader br = abrirRecurso(resourcePath)) {
            String header = br.readLine();
            if (header == null) return valores;
            if (!normalizar(header).equalsIgnoreCase(normalizar(headerEsperado))) {
                throw new IllegalArgumentException("Header inesperado en " + resourcePath +
                        ": '" + header + "' (se esperaba '" + headerEsperado + "')");
            }
            String linea;
            while ((linea = br.readLine()) != null) {
                linea = linea.trim();
                if (!linea.isBlank()) valores.add(linea);
            }
        }
        return valores;
    }

    private static List<Map<String, String>> leerLibros(String resourcePath) throws IOException {
        try (BufferedReader br = abrirRecurso(resourcePath)) {
            String header = br.readLine();
            if (header == null) return List.of();
            String[] cols = header.split(",", -1);
            if (cols.length < 3 ||
                    !cols[0].equals("title") ||
                    !cols[1].equals("author") ||
                    !cols[2].equals("genres")) {
                throw new IllegalArgumentException("Header inesperado en " + resourcePath + ": " + header);
            }
            List<Map<String, String>> filas = new ArrayList<>();
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] celdas = linea.split(",", -1); // dataset simple (sin comillas)
                if (celdas.length < 3) continue;
                Map<String, String> map = new LinkedHashMap<>();
                map.put("title", celdas[0]);
                map.put("author", celdas[1]);
                map.put("genres", celdas[2]);
                filas.add(map);
            }
            return filas;
        }
    }

    private static BufferedReader abrirRecurso(String resourcePath) {
        // Lee desde classpath: src/main/resources/...
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("No se encontró el recurso: " + resourcePath);
        }
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    private static String normalizar(String s) {
        return s == null ? "" : s.trim().replace("\uFEFF", ""); // quita posible BOM
    }

    // --- Verificación con JDBC puro ---

    private static void verificarConJDBC() {
        String url = "jdbc:h2:file:./data/librosdb;AUTO_SERVER=TRUE";
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            System.out.println("\nVerificación (JDBC):");
            imprimirConteo(conn, "autor");
            imprimirConteo(conn, "genero");
            imprimirConteo(conn, "libro");
            imprimirConteo(conn, "libro_genero");
            // Consulta de ejemplo: libros con sus géneros
            String sql = """
                    SELECT l.titulo, a.nombre AS autor, STRINGAGG(g.nombre, ';') AS generos
                    FROM libro l
                    JOIN autor a ON a.id = l.autor_id
                    LEFT JOIN libro_genero lg ON lg.libro_id = l.id
                    LEFT JOIN genero g ON g.id = lg.genero_id
                    GROUP BY l.titulo, a.nombre
                    ORDER BY l.titulo
                    """;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                System.out.println("\nLibros:");
                while (rs.next()) {
                    System.out.printf(" - %s | %s | %s%n",
                            rs.getString("titulo"),
                            rs.getString("autor"),
                            rs.getString("generos"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error JDBC: " + e.getMessage());
        }
    }

    private static void imprimirConteo(Connection conn, String tabla) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + tabla)) {
            if (rs.next()) {
                System.out.printf("Total en %-13s: %d%n", tabla, rs.getInt(1));
            }
        }
    }
}