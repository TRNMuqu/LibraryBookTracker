class Book {
    private final String title;
    private final String author;
    private final String isbn;   
    private final int copies;

    public Book(String title, String author, String isbn, int copies) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.copies = copies;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
    public String getIsbn() { return isbn; }
    public int getCopies() { return copies; }

    
    public String toCatalogLine() {
        return String.format("%s:%s:%s:%d", title, author, isbn, copies);
    }
}