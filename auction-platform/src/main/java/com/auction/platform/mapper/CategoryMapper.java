package com.auction.platform.mapper;

import com.auction.platform.dto.response.CategoryResponse;
import com.auction.platform.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentCategoryId", source = "parentCategory.id")
    CategoryResponse toResponse(Category category);
}
