package itcareer.model.push;

import itcareer.model.ABasicPushRequest;
import lombok.Data;

@Data
public class PushNotiRequest extends ABasicPushRequest {
    private String message;
    private String app;
    private Integer kind;
}
