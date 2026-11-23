package com.hub.pedidos360.kafka_admin.dto;

import java.util.List;

public record MemberInfo(String memberId, String clientId, String host, List<String> partitions) {
}
