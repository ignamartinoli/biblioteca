package org.example.modelos;

import jakarta.persistence.*;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "genero", uniqueConstraints = {
        @UniqueConstraint(name = "uk_genero_nombre", columnNames = { "nombre" })
})
public class Genero {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="nombre", nullable = false, unique = true, length = 100)
    private String nombre;

    @OneToMany(mappedBy = "genero")
    private Set<LibroGenero> libroGeneros = new LinkedHashSet<>();

    protected Genero() {}

    public Genero(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}
