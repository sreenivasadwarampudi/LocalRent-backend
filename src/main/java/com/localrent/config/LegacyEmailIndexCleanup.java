package com.localrent.config;

import com.localrent.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Users used to be keyed by email, leaving a unique {@code email_1} index behind. Accounts created
 * after the switch to phone numbers have no email, so that index rejects every signup past the
 * first with a duplicate {@code email: null} key.
 */
@Component
public class LegacyEmailIndexCleanup implements ApplicationRunner {

    private static final String LEGACY_INDEX = "email_1";
    private static final Logger log = LoggerFactory.getLogger(LegacyEmailIndexCleanup.class);

    private final MongoTemplate mongoTemplate;

    public LegacyEmailIndexCleanup(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        var indexOps = mongoTemplate.indexOps(User.class);
        boolean present = indexOps.getIndexInfo().stream()
                .anyMatch(index -> LEGACY_INDEX.equals(index.getName()));
        if (!present) {
            return;
        }
        try {
            indexOps.dropIndex(LEGACY_INDEX);
            log.info("Dropped legacy unique index {} on users", LEGACY_INDEX);
        } catch (DataAccessException ex) {
            log.warn("Could not drop legacy index {} on users: {}", LEGACY_INDEX, ex.getMessage());
        }
    }
}
