package com.example.library_system.service;

import com.example.library_system.dto.BookWithDetailsDTO;
import com.example.library_system.entity.Author;
import com.example.library_system.entity.Book;
import com.example.library_system.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private AuthorService authorService;

    // Hämta alla böcker
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    // Hämta bok med ID
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    // Skapa ny bok
    public Book createBook(Book book) {
        return bookRepository.save(book);
    }

    // Uppdatera bok
    public Book updateBook(Long id, Book bookDetails) {
        Optional<Book> optionalBook = bookRepository.findById(id);
        if (optionalBook.isPresent()) {
            Book book = optionalBook.get();
            book.setTitle(bookDetails.getTitle());
            book.setPublicationYear(bookDetails.getPublicationYear());
            book.setAvailableCopies(bookDetails.getAvailableCopies());
            book.setTotalCopies(bookDetails.getTotalCopies());
            book.setAuthorId(bookDetails.getAuthorId());
            return bookRepository.save(book);
        }
        return null;
    }

    // Ta bort bok
    public boolean deleteBook(Long id) {
        if (bookRepository.existsById(id)) {
            bookRepository.deleteById(id);
            return true;
        }
        return false;
    }

    // Kontrollera om bok är tillgänglig
    public boolean isBookAvailable(Long bookId) {
        Optional<Book> book = bookRepository.findById(bookId);
        return book.isPresent() && book.get().getAvailableCopies() > 0;
    }

    // Minska tillgängliga kopior (för utlåning)
    public boolean decreaseAvailableCopies(Long bookId) {
        Optional<Book> optionalBook = bookRepository.findById(bookId);
        if (optionalBook.isPresent()) {
            Book book = optionalBook.get();
            if (book.getAvailableCopies() > 0) {
                book.setAvailableCopies(book.getAvailableCopies() - 1);
                bookRepository.save(book);
                return true;
            }
        }
        return false;
    }

    // Öka tillgängliga kopior (för återlämning)
    public boolean increaseAvailableCopies(Long bookId) {
        Optional<Book> optionalBook = bookRepository.findById(bookId);
        if (optionalBook.isPresent()) {
            Book book = optionalBook.get();
            if (book.getAvailableCopies() < book.getTotalCopies()) {
                book.setAvailableCopies(book.getAvailableCopies() + 1);
                bookRepository.save(book);
                return true;
            }
        }
        return false;
    }

    // Sök böcker på titel
    public List<Book> searchBooksByTitle(String title) {
        return bookRepository.findByTitleContainingIgnoreCase(title);
    }

    // Hämta bok med detaljer (inkl. författarinfo)
    public BookWithDetailsDTO getBookWithDetails(Long bookId) {
        Optional<Book> bookOpt = bookRepository.findById(bookId);
        if (bookOpt.isPresent()) {
            Book book = bookOpt.get();
            Optional<Author> authorOpt = authorService.getAuthorById(book.getAuthorId());

            BookWithDetailsDTO dto = new BookWithDetailsDTO();
            dto.setBookId(book.getBookId());
            dto.setTitle(book.getTitle());
            dto.setPublicationYear(book.getPublicationYear());
            dto.setAvailableCopies(book.getAvailableCopies());
            dto.setTotalCopies(book.getTotalCopies());
            dto.setAuthorId(book.getAuthorId());

            if (authorOpt.isPresent()) {
                Author author = authorOpt.get();
                dto.setAuthorFirstName(author.getFirstName());
                dto.setAuthorLastName(author.getLastName());
                dto.setAuthorNationality(author.getNationality());
            }

            return dto;
        }
        return null;
    }

    // Hämta alla böcker med detaljer
    public List<BookWithDetailsDTO> getAllBooksWithDetails() {
        List<Book> books = bookRepository.findAll();
        List<BookWithDetailsDTO> dtos = new ArrayList<>();

        for (Book book : books) {
            BookWithDetailsDTO dto = getBookWithDetails(book.getBookId());
            if (dto != null) {
                dtos.add(dto);
            }
        }

        return dtos;
    }
}