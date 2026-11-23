package com.hub.pedidos360.kafka_admin.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hub.pedidos360.kafka_admin.dto.ConsumerGroupDetail;
import com.hub.pedidos360.kafka_admin.dto.ConsumerGroupInfo;
import com.hub.pedidos360.kafka_admin.dto.CreateTopicRequest;
import com.hub.pedidos360.kafka_admin.dto.GroupOffsetInfo;
import com.hub.pedidos360.kafka_admin.dto.TopicDetail;
import com.hub.pedidos360.kafka_admin.dto.TopicInfo;
import com.hub.pedidos360.kafka_admin.dto.UpdateTopicRequest;
import com.hub.pedidos360.kafka_admin.service.KafkaConsumerGroupService;
import com.hub.pedidos360.kafka_admin.service.KafkaTopicService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/kafka")
public class KafkaAdminController {

    private final KafkaTopicService topics;
    private final KafkaConsumerGroupService groups;

    public KafkaAdminController(KafkaTopicService topics, KafkaConsumerGroupService groups) {
        this.topics = topics;
        this.groups = groups;
    }

    // ---------- Topicos ----------

    @GetMapping("/topics")
    public List<TopicInfo> listTopics() {
        return topics.list();
    }

    @GetMapping("/topics/{name}")
    public TopicDetail getTopic(@PathVariable("name") String name) {
        return topics.get(name);
    }

    @PostMapping("/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public TopicInfo createTopic(@Valid @RequestBody CreateTopicRequest request) {
        return topics.create(request);
    }

    @PutMapping("/topics/{name}")
    public TopicDetail updateTopic(@PathVariable("name") String name,
            @Valid @RequestBody UpdateTopicRequest request) {
        return topics.update(name, request);
    }

    @DeleteMapping("/topics/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTopic(@PathVariable("name") String name) {
        topics.delete(name);
    }

    @GetMapping("/topics/{name}/config")
    public Map<String, String> getTopicConfig(@PathVariable("name") String name) {
        return topics.getConfig(name);
    }

    // ---------- Consumer groups ----------

    @GetMapping("/consumer-groups")
    public List<ConsumerGroupInfo> listGroups() {
        return groups.list();
    }

    @GetMapping("/consumer-groups/{groupId}")
    public ConsumerGroupDetail getGroup(@PathVariable("groupId") String groupId) {
        return groups.describe(groupId);
    }

    @GetMapping("/consumer-groups/{groupId}/offsets")
    public List<GroupOffsetInfo> getGroupOffsets(@PathVariable("groupId") String groupId) {
        return groups.offsets(groupId);
    }
}
