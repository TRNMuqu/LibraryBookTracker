// Base exception
class BookCatalogException extends Exception {
    public BookCatalogException(String message) {
        super(message);
    }
}

// ISBN not 13 digits or contains non-numeric characters
class InvalidISBNException extends BookCatalogException {
    public InvalidISBNException(String message) {
        super(message);
    }
}

// More than one book with same ISBN found
class DuplicateISBNException extends BookCatalogException {
    public DuplicateISBNException(String message) {
        super(message);
    }
}

// Missing fields, empty fields, invalid copies, wrong format
class MalformedBookEntryException extends BookCatalogException {
    public MalformedBookEntryException(String message) {
        super(message);
    }
}

// Less than 2 command line arguments
class InsufficientArgumentsException extends BookCatalogException {
    public InsufficientArgumentsException(String message) {
        super(message);
    }
}

// First argument does not end with .txt
class InvalidFileNameException extends BookCatalogException {
    public InvalidFileNameException(String message) {
        super(message);
    }
}