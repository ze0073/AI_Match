package com.aimatch.store;

import com.aimatch.model.entity.*;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class DataStores {

    public final JsonEntityStore<User> userStore = new JsonEntityStore<>(User.class, "users.json");
    public final JsonEntityStore<Resume> resumeStore = new JsonEntityStore<>(Resume.class, "resumes.json");
    public final JsonEntityStore<Job> jobStore = new JsonEntityStore<>(Job.class, "jobs.json");
    public final JsonEntityStore<Skill> skillStore = new JsonEntityStore<>(Skill.class, "skills.json");
    public final JsonEntityStore<MatchRecord> matchRecordStore = new JsonEntityStore<>(MatchRecord.class, "match_records.json");
    public final JsonEntityStore<Notification> notificationStore = new JsonEntityStore<>(Notification.class, "notifications.json");
    public final JsonEntityStore<OperationLog> operationLogStore = new JsonEntityStore<>(OperationLog.class, "operation_logs.json");
    public final JsonEntityStore<TalentFavorite> talentFavoriteStore = new JsonEntityStore<>(TalentFavorite.class, "talent_favorites.json");
    public final JsonEntityStore<SystemConfigEntity> systemConfigStore = new JsonEntityStore<>(SystemConfigEntity.class, "system_configs.json");

    @PostConstruct
    public void init() {
        userStore.init();
        resumeStore.init();
        jobStore.init();
        skillStore.init();
        matchRecordStore.init();
        notificationStore.init();
        operationLogStore.init();
        talentFavoriteStore.init();
        systemConfigStore.init();
    }
}
