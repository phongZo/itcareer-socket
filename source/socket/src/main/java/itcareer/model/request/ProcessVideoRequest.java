package itcareer.model.request;

import lombok.Data;
import itcareer.model.ABasicRequest;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class ProcessVideoRequest extends ABasicRequest {
    private String app;
    private String url;
    private Long simulationId;
    private Long subTaskId;
    private String tsSecond;
}
