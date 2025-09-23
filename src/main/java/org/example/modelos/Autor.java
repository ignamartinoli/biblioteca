package org.example.modelos;

import jakarta.persistence.*;

@Entity
@Table(name = "autor", uniqueConstraints = {
        @UniqueConstraint(name = "uk_autor_nombre", columnNames = {"nombre"})
})
public class Autor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true, length = 200)
    private String nombre;

    protected Autor() {} // JPA

    public Autor(String nombre) {
        this.nombre = nombre;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
}