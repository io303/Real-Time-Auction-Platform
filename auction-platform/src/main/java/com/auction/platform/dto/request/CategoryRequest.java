package com.auction.platform.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    private String description;

    private Long parentCategoryId;
}
