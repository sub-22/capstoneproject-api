package com.jovinn.capstoneproject.dto.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotNull;
import java.util.UUID;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeliveryResponse {
    @NotNull UUID id;
    UUID contractId;
    UUID milestoneId;
    String file;
    String description;

    public DeliveryResponse(UUID id, UUID contractId,
                            UUID milestoneId, String file,
                            String description) {
        this.id = id;
        this.contractId = contractId;
        this.milestoneId = milestoneId;
        this.file = file;
        this.description = description;
    }
}
