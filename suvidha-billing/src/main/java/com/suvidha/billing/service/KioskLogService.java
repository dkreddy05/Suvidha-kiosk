package com.suvidha.billing.service;

import com.suvidha.billing.entity.KioskLog;
import com.suvidha.billing.repository.KioskLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KioskLogService {
    private final KioskLogRepository repo;

    public KioskLogService(KioskLogRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public void log(String kioskId, String citizenId, String action, String path, String refNo) {
        KioskLog log = new KioskLog();
        log.setKioskId(kioskId);
        log.setCitizenId(citizenId);
        log.setAction(action);
        log.setPath(path);
        log.setRefNo(refNo);
        repo.save(log);
    }
}
