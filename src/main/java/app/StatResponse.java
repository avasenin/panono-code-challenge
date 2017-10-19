package app;

import com.fasterxml.jackson.annotation.JsonProperty;

public class StatResponse {
    @JsonProperty
    final long min;

    @JsonProperty
    final long max;

    @JsonProperty
    final long sum;

    @JsonProperty
    final long count;

    public StatResponse(Statistics.Record record) {
        this.min = record.min;
        this.max = record.max;
        this.sum = record.sum;
        this.count = record.count;
    }

    @JsonProperty
    public double getAvg() {
        if (0 == count) {
            return 0;
        }
        return (double) sum / count;
    }
}
