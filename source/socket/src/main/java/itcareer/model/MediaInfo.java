package itcareer.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;
@Data
public class MediaInfo {
    @Data
    public static class Format {
        @SerializedName("bit_rate")
        private String bitRate;
    }

    @Data
    public static class Stream {
        @SerializedName("index")
        private int index;

        @SerializedName("codec_name")
        private String codecName;

        @SerializedName("codec_long_name")
        private String codecLongame;

        @SerializedName("profile")
        private String profile;
    }

    // ----------------------------------

    @SerializedName("streams")
    private List<Stream> streams;

    @SerializedName("format")
    private Format format;
}
