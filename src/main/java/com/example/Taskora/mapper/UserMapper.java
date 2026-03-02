package com.example.Taskora.mapper;

import com.example.Taskora.dto.request.CreateUserRequest;
import com.example.Taskora.dto.response.UserResponse;
import com.example.Taskora.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "status", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    User toEntity(CreateUserRequest request);

    UserResponse toResponse(User user);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "clients", ignore = true)
    @Mapping(target = "projects", ignore = true)
    @Mapping(target = "invoices", ignore = true)
    void updateEntityFromRequest(CreateUserRequest request, @MappingTarget User user);
}
