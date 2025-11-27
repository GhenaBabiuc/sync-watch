package com.example.storageservice.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MappingUtil {

    private static final ModelMapper modelMapper;

    static {
        modelMapper = new ModelMapper();
        modelMapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STRICT)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);
    }

    public static <T, D> D map(T source, Class<D> destinationClass) {
        Objects.requireNonNull(source, "Source object cannot be null");
        Objects.requireNonNull(destinationClass, "Destination class cannot be null");

        return modelMapper.map(source, destinationClass);
    }

    public static <T, D> List<D> map(Collection<T> sourceList, Class<D> destinationClass) {
        Objects.requireNonNull(sourceList, "Source collection cannot be null");
        Objects.requireNonNull(destinationClass, "Destination class cannot be null");

        return sourceList.stream()
                .filter(Objects::nonNull)
                .map(source -> map(source, destinationClass))
                .toList();
    }

    public static <T, D> void map(T source, D destination) {
        Objects.requireNonNull(source, "Source object cannot be null");
        Objects.requireNonNull(destination, "Destination object cannot be null");

        modelMapper.map(source, destination);
    }
}
