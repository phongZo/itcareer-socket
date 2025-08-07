package itcareer.model.response;

import lombok.Data;
import itcareer.model.ABasicResponse;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DoneVideoProcessResponse extends ABasicResponse {
    private Long id;
    private Integer kind;
    private String thumbnail;
    private Boolean isSuccess;
    private String contentPath;
    private Long videoDuration;
}
