package pl.waw.krakus.codecGenerator

import lombok.Builder
import lombok.Data

@Data
@Builder
class Example {
    private long id
    private String name
    private boolean isActive
}