package com.example.library_system.controller;

import com.example.library_system.entity.Book;
import com.example.library_system.service.BookService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.library_system.dto.BookWithDetailsDTO;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/books")
public class BookController {

    @Autowired
    private BookService bookService;

    // GET /books - Lista alla böcker
    @GetMapping
    public List<Book> getAllBooks() {
        return bookService.getAllBooks();
    }

    // GET /books/search - Sök böcker på title
    @GetMapping("/search")
    public List<Book> searchBooks(@RequestParam(required = false) String title) {
        if (title != null && !title.isEmpty()) {
            return bookService.searchBooksByTitle(title);
        }
        return bookService.getAllBooks(); // Om ingen sökning, returnera alla
    }

    // GET /books/{id} - Hämta specifik bok
    @GetMapping("/{id}")
    public ResponseEntity<Book> getBookById(@PathVariable Long id) {
        Optional<Book> book = bookService.getBookById(id);
        if (book.isPresent()) {
            return ResponseEntity.ok(book.get());
        }
        return ResponseEntity.notFound().build();
    }

    // GET /books/details - Hämta alla böcker med författardetaljer
    @GetMapping("/details")
    public List<BookWithDetailsDTO> getAllBooksWithDetails() {
        return bookService.getAllBooksWithDetails();
    }

    // POST /books - Skapa ny bok
    @PostMapping
    public ResponseEntity<Book> createBook(@RequestBody Book book) {
        Book createdBook = bookService.createBook(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdBook);
    }

    // PUT /books/{id} - Uppdatera bok
    @PutMapping("/{id}")
    public ResponseEntity<Book> updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        Book updatedBook = bookService.updateBook(id, bookDetails);
        if (updatedBook != null) {
            return ResponseEntity.ok(updatedBook);
        }
        return ResponseEntity.notFound().build();
    }

    // DELETE /books/{id} - Ta bort bok
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id) {
        if (bookService.deleteBook(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}