/**
 * A simple document class.
 */
public class Doc {
    private final int id;
    private final String title, summary;

    public Doc(int id, String title, String summary) {
        this.id = id;
        this.title = title;
        this.summary = summary;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    @Override
    public String toString() {
        return title;
    }
}
