package com.aimatch.model.dto;

import java.util.List;

public record CapabilityGraphDTO(
    List<GraphNode> nodes,
    List<GraphEdge> edges
) {
    public record GraphNode(
        String id,
        String label,
        String group,
        int symbolSize
    ) {}

    public record GraphEdge(
        String source,
        String target,
        String label
    ) {}
}
