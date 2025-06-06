package com.example.library_system.repository;

import com.example.library_system.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    // JpaRepository ger oss automatiskt metoder som:
    // findAll(), findById(), save(), deleteById() etc.

    // Sök böcker på titel
    List<Book> findByTitleContainingIgnoreCase(String title);

    // Sök böcker på författar-ID
    List<Book> findByAuthorId(Long authorId);

    // Kombinerad sökning på titel ELLER författar-ID
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR b.authorId = :authorId")
    List<Book> searchBooks(@Param("searchTerm") String searchTerm, @Param("authorId") Long authorId);
}