package com.jovinn.capstoneproject.dto.client.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentRequest {
    @NotBlank(message = "Không thể bình luận trạng thái trống")
    @Size(min = 1, max = 255)
    String text;
}
