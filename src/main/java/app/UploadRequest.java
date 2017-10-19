package app;

public class UploadRequest {
    private long timestamp;
    private long count;

    public UploadRequest() {
    }

    public UploadRequest(long timestamp, long count) {
        this.timestamp = timestamp;
        this.count = count;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getCount() {
        return count;
    }
}
