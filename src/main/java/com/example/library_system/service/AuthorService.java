package com.example.library_system.service;

import com.example.library_system.entity.Author;
import com.example.library_system.repository.AuthorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AuthorService {

    @Autowired
    private AuthorRepository authorRepository;

    // Hämta alla författare
    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    // Hämta författare med ID
    public Optional<Author> getAuthorById(Long id) {
        return authorRepository.findById(id);
    }

    // Hämta författare via efternamn
    public List<Author> getAuthorsByLastName(String lastName) {
        return authorRepository.findByLastNameIgnoreCase(lastName);
    }

    // Skapa ny författare
    public Author createAuthor(Author author) {
        return authorRepository.save(author);
    }

    // Uppdatera författare
    public Author updateAuthor(Long id, Author authorDetails) {
        Optional<Author> optionalAuthor = authorRepository.findById(id);
        if (optionalAuthor.isPresent()) {
            Author author = optionalAuthor.get();
            author.setFirstName(authorDetails.getFirstName());
            author.setLastName(authorDetails.getLastName());
            author.setBirthYear(authorDetails.getBirthYear());
            author.setNationality(authorDetails.getNationality());
            return authorRepository.save(author);
        }
        return null;
    }

    // Ta bort författare
    public boolean deleteAuthor(Long id) {
        if (authorRepository.existsById(id)) {
            authorRepository.deleteById(id);
            return true;
        }
        return false;
    }
}