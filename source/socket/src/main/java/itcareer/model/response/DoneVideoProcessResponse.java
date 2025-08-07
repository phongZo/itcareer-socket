package itcareer.model.response;

import lombok.Data;
import itcareer.model.ABasicResponse;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DoneVideoProcessResponse extends ABasicResponse {
    private Long simulationId;
    private Long taskId;
    private String thumbnail;
    private Boolean isSuccess;
    private String contentPath;
    private Long videoDuration;
}
