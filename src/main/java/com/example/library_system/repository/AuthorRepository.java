package com.example.library_system.repository;

import com.example.library_system.entity.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthorRepository extends JpaRepository<Author, Long> {

    // Hitta författare via efternamn
    List<Author> findByLastNameIgnoreCase(String lastName);

    // Hitta författare via förnamn
    List<Author> findByFirstNameIgnoreCase(String firstName);

    // Hitta författare via nationalitet
    List<Author> findByNationalityIgnoreCase(String nationality);
}