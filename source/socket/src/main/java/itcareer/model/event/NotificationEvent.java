package itcareer.model.event;

import itcareer.model.ABasicModel;
import lombok.Data;

@Data
public class NotificationEvent extends ABasicModel {
    private String message;
    private String app;
    private Integer kind;
    private Long userId;
}
