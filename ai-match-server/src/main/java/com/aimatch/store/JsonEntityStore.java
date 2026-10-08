package com.aimatch.store;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
public class JsonEntityStore<T> {

    private static final Path DATA_DIR = Paths.get("data");
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    private final ConcurrentHashMap<Long, T> data = new ConcurrentHashMap<>();
    private final AtomicLong idSeq = new AtomicLong(1);
    private final Path filePath;
    private final Class<T> entityClass;
    private Method getIdMethod;
    private Method setIdMethod;

    public JsonEntityStore(Class<T> entityClass, String fileName) {
        this.entityClass = entityClass;
        this.filePath = DATA_DIR.resolve(fileName);
        try {
            this.getIdMethod = entityClass.getMethod("getId");
            this.setIdMethod = entityClass.getMethod("setId", Long.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Entity must have getId/setId: " + entityClass.getName(), e);
        }
    }

    @PostConstruct
    public void init() {
        load();
    }

    // ============ CRUD ============

    public T findById(Long id) {
        return data.get(id);
    }

    public List<T> findAll() {
        return new ArrayList<>(data.values());
    }

    public List<T> findAll(Predicate<T> filter) {
        return data.values().stream().filter(filter).collect(Collectors.toList());
    }

    public T findOne(Predicate<T> filter) {
        return data.values().stream().filter(filter).findFirst().orElse(null);
    }

    public long count() {
        return data.size();
    }

    public long count(Predicate<T> filter) {
        return data.values().stream().filter(filter).count();
    }

    public List<T> page(int pageNum, int pageSize, Predicate<T> filter, Comparator<T> sorter) {
        return data.values().stream()
                .filter(filter)
                .sorted(sorter)
                .skip((long) (pageNum - 1) * pageSize)
                .limit(pageSize)
                .collect(Collectors.toList());
    }

    public List<T> page(int pageNum, int pageSize, Predicate<T> filter) {
        return page(pageNum, pageSize, filter, (a, b) -> 0);
    }

    public T save(T entity) {
        try {
            Long id = (Long) getIdMethod.invoke(entity);
            if (id == null) {
                id = idSeq.getAndIncrement();
                setIdMethod.invoke(entity, id);
            } else {
                if (id >= idSeq.get()) {
                    idSeq.set(id + 1);
                }
            }
            // Set createTime/updateTime if entity has those methods
            trySetField(entity, "setCreateTime", LocalDateTime.now(), LocalDateTime.class);
            trySetField(entity, "setUpdateTime", LocalDateTime.now(), LocalDateTime.class);
            data.put(id, entity);
            persist();
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save entity", e);
        }
    }

    public T update(T entity) {
        try {
            Long id = (Long) getIdMethod.invoke(entity);
            if (id == null || !data.containsKey(id)) {
                throw new RuntimeException("Entity not found: " + id);
            }
            trySetField(entity, "setUpdateTime", LocalDateTime.now(), LocalDateTime.class);
            data.put(id, entity);
            persist();
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Failed to update entity", e);
        }
    }

    public void deleteById(Long id) {
        data.remove(id);
        persist();
    }

    // ============ Persistence ============

    public synchronized void load() {
        try {
            Files.createDirectories(DATA_DIR);
            if (Files.exists(filePath)) {
                String json = Files.readString(filePath);
                if (!json.isBlank()) {
                    List<T> list = MAPPER.readValue(json,
                            MAPPER.getTypeFactory().constructCollectionType(List.class, entityClass));
                    long maxId = 0;
                    for (T item : list) {
                        Long id = (Long) getIdMethod.invoke(item);
                        if (id != null) {
                            data.put(id, item);
                            if (id > maxId) maxId = id;
                        }
                    }
                    idSeq.set(maxId + 1);
                    log.info("Loaded {} {} records from {}", data.size(), entityClass.getSimpleName(), filePath);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load data from {}, starting fresh", filePath, e);
        }
    }

    public synchronized void persist() {
        try {
            Files.createDirectories(DATA_DIR);
            List<T> list = new ArrayList<>(data.values());
            String json = MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(list);
            Path tmp = Paths.get(filePath.toString() + ".tmp");
            Files.writeString(tmp, json);
            Files.move(tmp, filePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("Failed to persist data to {}", filePath, e);
        }
    }

    // ============ Helpers ============

    private void trySetField(T entity, String methodName, Object value, Class<?> paramType) {
        try {
            Method m = entityClass.getMethod(methodName, paramType);
            m.invoke(entity, value);
        } catch (Exception ignored) {
        }
    }
}
