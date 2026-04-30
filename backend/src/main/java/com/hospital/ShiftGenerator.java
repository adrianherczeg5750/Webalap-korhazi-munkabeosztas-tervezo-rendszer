package com.hospital;

import java.util.List;

public interface ShiftGenerator {
    List<ShiftAssigment> generate(GeneratorInput input);
}
