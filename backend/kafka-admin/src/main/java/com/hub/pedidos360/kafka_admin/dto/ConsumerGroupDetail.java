package com.hub.pedidos360.kafka_admin.dto;

import java.util.List;

public record ConsumerGroupDetail(String groupId, String state, String partitionAssignor,
        Integer coordinatorId, List<MemberInfo> members) {
}
