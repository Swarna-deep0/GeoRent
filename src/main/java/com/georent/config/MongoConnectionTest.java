package com.georent.config;

import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MongoConnectionTest {

    private final MongoTemplate mongoTemplate;

    public MongoConnectionTest(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @GetMapping("/api/test/mongodb")
    public String testMongoDBConnection() {

        Document result = mongoTemplate
                .getDb()
                .runCommand(new Document("ping", 1));

        return "MongoDB Connected Successfully! Response: "
                + result.toJson();
    }
}