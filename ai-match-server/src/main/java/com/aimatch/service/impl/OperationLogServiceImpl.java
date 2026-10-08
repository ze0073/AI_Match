package com.aimatch.service.impl;

import com.aimatch.model.entity.OperationLog;
import com.aimatch.service.OperationLogService;
import com.aimatch.store.DataStores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperationLogServiceImpl implements OperationLogService {

    private final DataStores stores;

    @Override
    public void recordLog(OperationLog log) {
        stores.operationLogStore.save(log);
    }
}
