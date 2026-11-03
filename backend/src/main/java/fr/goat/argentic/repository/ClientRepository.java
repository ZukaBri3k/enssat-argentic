package fr.goat.argentic.repository;

import fr.goat.argentic.model.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
