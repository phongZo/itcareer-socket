package itcareer.model.request;

import itcareer.model.ABasicRequest;
import lombok.Data;

@Data
public class ClientInfoRequest extends ABasicRequest {
    private String app;
}
