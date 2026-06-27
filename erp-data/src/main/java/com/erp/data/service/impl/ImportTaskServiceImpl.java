package com.erp.data.service.impl;

import com.erp.common.cache.CacheTemplate;
import com.erp.data.dto.ImportTaskVO;
import com.erp.data.service.ImportTaskService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 导入任务进度服务：基于 Redis 存储任务进度，支持进度查询和失败快照。
 */
@Slf4j
@Service
public class ImportTaskServiceImpl implements ImportTaskService {

    private static final String KEY_PREFIX = "erp:import:";
    private static final Duration TTL = Duration.ofHours(2);
    private static final int MAX_FAIL_SNAPSHOT = 1000;

    private final CacheTemplate cacheTemplate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    public ImportTaskServiceImpl(CacheTemplate cacheTemplate, StringRedisTemplate redisTemplate) {
        this.cacheTemplate = cacheTemplate;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ImportTaskVO createTask(String taskId, String fileName) {
        ImportTaskVO vo = new ImportTaskVO();
        vo.setTaskId(taskId);
        vo.setFileName(fileName);
        vo.setStatus(ImportTaskVO.Status.PENDING.name());
        vo.setMessage("等待解析");
        vo.setTotalRows(0L);
        vo.setProcessedRows(0L);
        vo.setSuccessCount(0L);
        vo.setFailCount(0L);
        vo.setPercent(0);
        vo.setFailList(new ArrayList<>());
        vo.setCreatedAt(LocalDateTime.now());
        put(vo);
        return vo;
    }

    @Override
    public void markRunning(String taskId) {
        update(taskId, vo -> {
            vo.setStatus(ImportTaskVO.Status.RUNNING.name());
            vo.setMessage("正在解析并导入...");
        });
    }

    @Override
    public void updateProgress(String taskId, long totalRows, long processedRows,
                               long successCount, long failCount) {
        update(taskId, vo -> {
            vo.setTotalRows(totalRows);
            vo.setProcessedRows(processedRows);
            vo.setSuccessCount(successCount);
            vo.setFailCount(failCount);
            int percent = totalRows <= 0 ? 0 : (int) (processedRows * 100 / totalRows);
            vo.setPercent(Math.min(percent, 99));
        });
    }

    @Override
    public void addFailItem(String taskId, ImportTaskVO.FailItem item) {
        update(taskId, vo -> {
            if (vo.getFailList() == null) {
                vo.setFailList(new ArrayList<>());
            }
            // 只保留前 MAX_FAIL_SNAPSHOT 条失败明细
            if (vo.getFailList().size() < MAX_FAIL_SNAPSHOT) {
                vo.getFailList().add(item);
            }
        });
    }

    @Override
    public void markDone(String taskId, String message) {
        update(taskId, vo -> {
            vo.setStatus(ImportTaskVO.Status.DONE.name());
            vo.setMessage(message);
            vo.setPercent(100);
            vo.setFinishedAt(LocalDateTime.now());
        });
    }

    @Override
    public void markFailed(String taskId, String message) {
        update(taskId, vo -> {
            vo.setStatus(ImportTaskVO.Status.FAILED.name());
            vo.setMessage(message);
            vo.setFinishedAt(LocalDateTime.now());
        });
    }

    @Override
    public ImportTaskVO getTask(String taskId) {
        String key = KEY_PREFIX + taskId;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) return null;
            return deserialize(json);
        } catch (Exception e) {
            log.warn("读取导入任务进度失败, taskId={}: {}", taskId, e.getMessage());
            return null;
        }
    }

    private void update(String taskId, java.util.function.Consumer<ImportTaskVO> consumer) {
        ImportTaskVO vo = getTask(taskId);
        if (vo == null) {
            log.warn("导入任务不存在, taskId={}", taskId);
            return;
        }
        consumer.accept(vo);
        put(vo);
    }

    private void put(ImportTaskVO vo) {
        cacheTemplate.put(KEY_PREFIX + vo.getTaskId(), vo, TTL);
    }

    private String serialize(ImportTaskVO vo) {
        try {
            return objectMapper.writeValueAsString(vo);
        } catch (Exception e) {
            log.warn("导入任务序列化失败: {}", e.getMessage());
            return null;
        }
    }

    private ImportTaskVO deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("导入任务反序列化失败: {}", e.getMessage());
            return null;
        }
    }
}
