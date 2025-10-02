package org.example;

import org.example.modelos.Autor;
import org.example.modelos.Estado;
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

        try (emf; EntityManager em = emf.createEntityManager()) {
            AutorRepository autorRepo = new AutorRepository(em);
            GeneroRepository generoRepo = new GeneroRepository(em);
            LibroRepository  libroRepo  = new LibroRepository(em);

            em.getTransaction().begin();
            List<Map<String, String>> libros = leerLibros("data/books.csv");
            for (Map<String, String> fila : libros) {
                String titulo      = fila.get("title").trim();
                String autorNombre = fila.get("author").trim();
                String generosStr  = fila.get("genres").trim();
                String statusStr   = fila.get("status").trim();

                Autor autor = autorRepo.getOrCreate(autorNombre);
                Estado estado = parseEstado(statusStr);
                Libro libro = libroRepo.getOrCreate(titulo, autor, estado);

                Set<String> nombresGeneros = Arrays.stream(generosStr.split(";"))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                for (String g : nombresGeneros) {
                    Genero genero = generoRepo.getOrCreate(g);
                    libro.addGenero(genero);
                }
            }
            em.getTransaction().commit();

            // verificarConJDBC();

            System.out.println("\nCarga finalizada.");

            List<Libro> librosBorges = libroRepo.findByAutorNombre("Jorge Luis Borges");

            System.out.println("\nLibros de Jorge Luis Borges (vía JPA):");
            for (Libro l : librosBorges) {
                String generos = l.getGeneros().stream()
                        .map(Genero::getNombre)
                        .collect(Collectors.joining(";"));
                System.out.printf(" - %s | %s | %s | %s%n", l.getTitulo(), l.getAutor().getNombre(), generos, l.getEstado());
            }

            List<Libro> librosFiccion = libroRepo.findByGeneroNombre("Ficción");

            System.out.println("\nLibros del género Ficción (vía JPA):");
            for (Libro l : librosFiccion) {
                String generos = l.getGeneros().stream()
                        .map(Genero::getNombre)
                        .collect(Collectors.joining(";"));
                System.out.printf(" - %s | %s | %s | %s%n",
                        l.getTitulo(),
                        l.getAutor().getNombre(),
                        generos,
                        l.getEstado());
            }
        } catch (Exception e) {
            System.err.println("Error durante la carga: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static List<Map<String, String>> leerLibros(String resourcePath) throws IOException {
        try (BufferedReader br = abrirRecurso(resourcePath)) {
            String header = br.readLine();
            if (header == null) return List.of();

            String[] cols = header.split(",", -1);
            if (cols.length < 4 ||
                    !cols[0].equals("title") ||
                    !cols[1].equals("author") ||
                    !cols[2].equals("genres") ||
                    !cols[3].equals("status")) {
                throw new IllegalArgumentException("Header inesperado en " + resourcePath + ": " + header);
            }
            List<Map<String, String>> filas = new ArrayList<>();
            String linea;
            while ((linea = br.readLine()) != null) {
                if (linea.isBlank()) continue;
                String[] celdas = linea.split(",", -1);
                if (celdas.length < 4) {
                    throw new IllegalArgumentException("Fila con columnas insuficientes: " + linea);
                }

                Map<String, String> map = new LinkedHashMap<>();
                map.put("title",  celdas[0]);
                map.put("author", celdas[1]);
                map.put("genres", celdas[2]);
                map.put("status", celdas[3]);
                filas.add(map);
            }
            return filas;
        }
    }

    private static BufferedReader abrirRecurso(String resourcePath) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IllegalArgumentException("No se encontró el recurso: " + resourcePath);
        }
        return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
    }

    private static String normalizar(String s) {
        return s == null ? "" : s.trim().replace("\uFEFF", ""); // quita posible BOM
    }

    static Estado parseEstado(String valor) {
        return Estado.valueOf(valor.trim().toUpperCase());
    }

    private static void verificarConJDBC() {
        String url = "jdbc:h2:file:./data/librosdb;AUTO_SERVER=TRUE";
        try (Connection conn = DriverManager.getConnection(url, "sa", "")) {
            System.out.println("\nVerificación (JDBC):");
            imprimirConteo(conn, "autor");
            imprimirConteo(conn, "genero");
            imprimirConteo(conn, "libro");
            imprimirConteo(conn, "libro_genero");
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