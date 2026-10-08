package com.aimatch.service;

import com.aimatch.model.entity.OperationLog;

public interface OperationLogService {

    void recordLog(OperationLog log);
}
