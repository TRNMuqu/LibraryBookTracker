import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LibraryBookTracker {

    
    private static final String HEADER_FORMAT = "%-30s %-20s %-15s %5s%n";
    private static final String ROW_FORMAT    = "%-30.30s %-20.20s %-15.15s %5d%n";


    private static class Stats {
        int validRecordsProcessed = 0;
        int searchResults = 0;
        int booksAdded = 0;
        int errorsEncountered = 0;
    }

    public static void main(String[] args) {
        Stats stats = new Stats();
        Path catalogPath = null;
        Path logPath = null;

        try {
           
            if (args.length < 2) {
                throw new InsufficientArgumentsException(
                        "Fewer than two command-line arguments provided. Expected: <catalog.txt> <operation>"
                );
            }

        
            String catalogArg = args[0];
            if (!catalogArg.toLowerCase().endsWith(".txt")) {
                throw new InvalidFileNameException("First argument must end with .txt");
            }

            catalogPath = Paths.get(catalogArg);
            ensureCatalogFileAndParentExist(catalogPath);

            logPath = getLogPathNextToCatalog(catalogPath);

         
            List<Book> books = readValidBooks(catalogPath, logPath, stats);

    
            String op = args[1];

            if (looksLikeNewRecord(op)) {
                try {
                    Book newBook = parseAndValidateBookRecord(op);
                    books.add(newBook);

                    books.sort(Comparator.comparing(b -> b.getTitle().toLowerCase()));

                    writeCatalog(catalogPath, books);
                    stats.booksAdded = 1;

                    printHeader();
                    printBookRow(newBook);

                } catch (BookCatalogException e) {
                    stats.errorsEncountered++;
                    logError(logPath, op, e);
                    System.out.println("Error: " + e.getMessage());
                }

            } else if (isExactly13Digits(op)) {
                // ISBN search
                List<Book> matches = new ArrayList<>();
                for (Book b : books) {
                    if (b.getIsbn().equals(op)) {
                        matches.add(b);
                    }
                }

                if (matches.size() > 1) {
                    throw new DuplicateISBNException(
                            "More than one book with this ISBN was found: " + op
                    );
                }

                printHeader();
                if (matches.size() == 1) {
                    printBookRow(matches.get(0));
                    stats.searchResults = 1;
                } else {
                    stats.searchResults = 0;
                }

            } else {
              
                String keyword = op.toLowerCase();
                printHeader();

                int count = 0;
                for (Book b : books) {
                    if (b.getTitle().toLowerCase().contains(keyword)) {
                        printBookRow(b);
                        count++;
                    }
                }
                stats.searchResults = count;
            }

        } catch (BookCatalogException e) {
            
            stats.errorsEncountered++;

            if (catalogPath != null) {
                try {
                    logPath = (logPath == null) ? getLogPathNextToCatalog(catalogPath) : logPath;
                    String offending = (args.length >= 2) ? args[1] : String.join(" ", args);
                    if (offending == null || offending.isBlank()) offending = "(no operation provided)";
                    logError(logPath, offending, e);
                } catch (Exception ignored) {
                  
                }
            }

            System.out.println("Error: " + e.getMessage());

        } catch (IOException e) {
            
            stats.errorsEncountered++;
            try {
                if (catalogPath != null) {
                    logPath = (logPath == null) ? getLogPathNextToCatalog(catalogPath) : logPath;
                    logError(logPath, "I/O operation", e);
                }
            } catch (Exception ignored) {}
            System.out.println("Error: I/O failure - " + e.getMessage());

        } catch (Exception e) {
            
            stats.errorsEncountered++;
            try {
                if (catalogPath != null) {
                    logPath = (logPath == null) ? getLogPathNextToCatalog(catalogPath) : logPath;
                    logError(logPath, "Unexpected error", e);
                }
            } catch (Exception ignored) {}
            System.out.println("Error: Unexpected failure - " + e.getMessage());

        } finally {
            
            System.out.println();
            System.out.println("Valid records processed: " + stats.validRecordsProcessed);
            System.out.println("Search results: " + stats.searchResults);
            System.out.println("Books added: " + stats.booksAdded);
            System.out.println("Errors encountered: " + stats.errorsEncountered);

          
            System.out.println();
            System.out.println("Thank you for using the Library Book Tracker.");
        }
    }

    private static void ensureCatalogFileAndParentExist(Path catalogPath) throws IOException {
        Path parent = catalogPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(catalogPath)) {
            Files.createFile(catalogPath);
        }
    }

    private static Path getLogPathNextToCatalog(Path catalogPath) {
        Path abs = catalogPath.toAbsolutePath();
        Path parent = abs.getParent();
        if (parent == null) {
            return Paths.get("errors.log").toAbsolutePath();
        }
        return parent.resolve("errors.log");
    }

    private static void logError(Path logPath, String offendingText, Exception e) throws IOException {
        String ts = LocalDateTime.now().toString();
        String line = String.format("[%s] INVALID: \"%s\" - %s: %s",
                ts,
                offendingText,
                e.getClass().getSimpleName(),
                e.getMessage()
        );

       
        try (BufferedWriter bw = Files.newBufferedWriter(
                logPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            bw.write(line);
            bw.newLine();
        }
    }

  
    private static List<Book> readValidBooks(Path catalogPath, Path logPath, Stats stats) throws IOException {
        List<Book> books = new ArrayList<>();
        List<String> lines = Files.readAllLines(catalogPath, StandardCharsets.UTF_8);

        for (String line : lines) {
            if (line == null) continue;
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            try {
                Book b = parseAndValidateBookRecord(trimmed);
                books.add(b);
                stats.validRecordsProcessed++;
            } catch (BookCatalogException e) {
                stats.errorsEncountered++;
                logError(logPath, trimmed, e);
               
            }
        }
        return books;
    }

    private static void writeCatalog(Path catalogPath, List<Book> books) throws IOException {
        List<String> out = new ArrayList<>();
        for (Book b : books) {
            out.add(b.toCatalogLine());
        }
        Files.write(catalogPath, out, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
    }


    private static boolean looksLikeNewRecord(String s) {
        if (s == null) return false;
        String[] parts = s.split(":", -1);
        return parts.length == 4; 
    }

    private static Book parseAndValidateBookRecord(String record) throws BookCatalogException {
        String[] parts = record.split(":", -1);
        if (parts.length != 4) {
            throw new MalformedBookEntryException("Book entry must have exactly 4 fields: Title:Author:ISBN:Copies");
        }

        String title = parts[0].trim();
        String author = parts[1].trim();
        String isbn = parts[2].trim();
        String copiesStr = parts[3].trim();

        if (title.isEmpty()) {
            throw new MalformedBookEntryException("Title is empty");
        }
        if (author.isEmpty()) {
            throw new MalformedBookEntryException("Author is empty");
        }
        if (!isExactly13Digits(isbn)) {
            throw new InvalidISBNException("ISBN is not exactly 13 digits or contains non-numeric characters");
        }

        int copies;
        try {
            copies = Integer.parseInt(copiesStr);
        } catch (NumberFormatException e) {
            throw new MalformedBookEntryException("Copies is not a valid integer");
        }

        if (copies <= 0) {
            throw new MalformedBookEntryException("Copies must be a positive integer");
        }

        return new Book(title, author, isbn, copies);
    }

    private static boolean isExactly13Digits(String s) {
        if (s == null) return false;
        if (s.length() != 13) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

  
    private static void printHeader() {
        System.out.printf(HEADER_FORMAT, "Title", "Author", "ISBN", "Copies");
    }

    private static void printBookRow(Book b) {
        System.out.printf(ROW_FORMAT,
                b.getTitle(),
                b.getAuthor(),
                b.getIsbn(),
                b.getCopies()
        );
    }
}