package com.example.library_system.config;

import com.example.library_system.entity.*;
import com.example.library_system.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner init(UserRepository userRepository,
                           RoleRepository roleRepository,
                           BookRepository bookRepository,
                           AuthorRepository authorRepository) {

        return args -> {
            logger.info("=====================================");
            logger.info("Starting Data Initialization...");
            logger.info("=====================================");

            // Skapa roller om de inte finns
            Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
                logger.info("Creating ADMIN role");
                Role newRole = new Role("ADMIN", "Administrator with full access");
                return roleRepository.save(newRole);
            });

            Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
                logger.info("Creating USER role");
                Role newRole = new Role("USER", "Standard library user");
                return roleRepository.save(newRole);
            });

            // Skapa admin-användare om den inte finns
            if (!userRepository.existsByEmail("admin@bibliotek.se")) {
                logger.info("Creating admin user");
                User admin = new User();
                admin.setFirstName("Admin");
                admin.setLastName("Administratör");
                admin.setEmail("admin@bibliotek.se");
                admin.setPassword(passwordEncoder.encode("Admin123"));
                admin.setRegistrationDate(LocalDate.now());
                admin.setEnabled(true);
                admin.setAccountNonLocked(true);
                admin.setFailedLoginAttempts(0);
                admin.addRole(adminRole);
                admin.addRole(userRole);
                userRepository.save(admin);
                logger.info("✅ Admin user created: admin@bibliotek.se / Admin123");
            } else {
                logger.info("Admin user already exists");
            }

            // Skapa testanvändare om den inte finns
            if (!userRepository.existsByEmail("user@bibliotek.se")) {
                logger.info("Creating test user");
                User testUser = new User();
                testUser.setFirstName("Test");
                testUser.setLastName("Användare");
                testUser.setEmail("user@bibliotek.se");
                testUser.setPassword(passwordEncoder.encode("User123"));
                testUser.setRegistrationDate(LocalDate.now());
                testUser.setEnabled(true);
                testUser.setAccountNonLocked(true);
                testUser.setFailedLoginAttempts(0);
                testUser.addRole(userRole);
                userRepository.save(testUser);
                logger.info("✅ Test user created: user@bibliotek.se / User123");
            } else {
                logger.info("Test user already exists");
            }

            // Skapa författare och böcker om databasen är tom
            if (authorRepository.count() == 0) {
                logger.info("Creating sample authors and books...");
                logger.info("=====================================");

                // Astrid Lindgren
                Author astrid = new Author();
                astrid.setFirstName("Astrid");
                astrid.setLastName("Lindgren");
                astrid.setBirthYear(1907);
                astrid.setNationality("Swedish");
                astrid = authorRepository.save(astrid);
                logger.info("✅ Author created: Astrid Lindgren");

                // J.K. Rowling
                Author jk = new Author();
                jk.setFirstName("J.K.");
                jk.setLastName("Rowling");
                jk.setBirthYear(1965);
                jk.setNationality("British");
                jk = authorRepository.save(jk);
                logger.info("✅ Author created: J.K. Rowling");

                // Stephen King
                Author stephen = new Author();
                stephen.setFirstName("Stephen");
                stephen.setLastName("King");
                stephen.setBirthYear(1947);
                stephen.setNationality("American");
                stephen = authorRepository.save(stephen);
                logger.info("✅ Author created: Stephen King");

                // Skapa böcker
                logger.info("-------------------------------------");
                logger.info("Creating books...");

                // Astrid Lindgren böcker
                createBook("Pippi Långstrump", 1945, 3, 5, astrid.getAuthorId(), bookRepository);
                createBook("Ronja Rövardotter", 1981, 0, 2, astrid.getAuthorId(), bookRepository); // 0 tillgängliga för demo!
                createBook("Emil i Lönneberga", 1963, 2, 3, astrid.getAuthorId(), bookRepository);
                createBook("Bröderna Lejonhjärta", 1973, 1, 2, astrid.getAuthorId(), bookRepository);

                // Harry Potter böcker
                createBook("Harry Potter och De Vises Sten", 1997, 4, 5, jk.getAuthorId(), bookRepository);
                createBook("Harry Potter och Hemligheternas Kammare", 1998, 3, 4, jk.getAuthorId(), bookRepository);
                createBook("Harry Potter och Fången från Azkaban", 1999, 0, 3, jk.getAuthorId(), bookRepository); // 0 för test

                // Stephen King böcker
                createBook("The Shining", 1977, 2, 3, stephen.getAuthorId(), bookRepository);
                createBook("IT", 1986, 1, 2, stephen.getAuthorId(), bookRepository);
                createBook("The Green Mile", 1996, 3, 3, stephen.getAuthorId(), bookRepository);

                logger.info("=====================================");
                logger.info("✅ Sample data created successfully!");
                logger.info("=====================================");
                logger.info("📚 Total books: " + bookRepository.count());
                logger.info("✍️ Total authors: " + authorRepository.count());
                logger.info("👥 Total users: " + userRepository.count());
                logger.info("🔑 Total roles: " + roleRepository.count());
                logger.info("=====================================");

                // Visa böcker med 0 kopior för demo
                logger.info("⚠️ Books with 0 available copies (for testing):");
                logger.info("  - Ronja Rövardotter");
                logger.info("  - Harry Potter och Fången från Azkaban");
                logger.info("=====================================");

            } else {
                logger.info("Authors and books already exist in database");
                logger.info("📚 Current books: " + bookRepository.count());
                logger.info("✍️ Current authors: " + authorRepository.count());
            }

            // Logga inloggningsinfo
            logger.info("");
            logger.info("🔐 LOGIN CREDENTIALS:");
            logger.info("=====================================");
            logger.info("Admin: admin@bibliotek.se / Admin123");
            logger.info("User:  user@bibliotek.se / User123");
            logger.info("=====================================");
            logger.info("");
            logger.info("✅ System ready to use!");
            logger.info("");
        };
    }

    private void createBook(String title, int year, int available, int total, Long authorId, BookRepository repo) {
        Book book = new Book();
        book.setTitle(title);
        book.setPublicationYear(year);
        book.setAvailableCopies(available);
        book.setTotalCopies(total);
        book.setAuthorId(authorId);
        repo.save(book);

        // Visa varning om boken inte har några tillgängliga kopior
        if (available == 0) {
            logger.info("📕 Created book: " + title + " (⚠️ NO COPIES AVAILABLE - good for testing!)");
        } else {
            logger.info("📗 Created book: " + title + " (Available: " + available + "/" + total + ")");
        }
    }
}