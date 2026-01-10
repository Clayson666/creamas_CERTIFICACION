package com.example.demo.repository;

import com.example.demo.model.Participante;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {
    boolean existsByCodigoVerificacion(String codigoVerificacion);
}
