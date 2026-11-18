package com.rural.travel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rural.travel.common.PageResult;
import com.rural.travel.entity.SysLog;
import com.rural.travel.mapper.SysLogMapper;
import org.springframework.stereotype.Service;

@Service
public class SysLogService {

    private final SysLogMapper sysLogMapper;

    public SysLogService(SysLogMapper sysLogMapper) {
        this.sysLogMapper = sysLogMapper;
    }

    public void save(Long userId, String username, String action, String detail) {
        SysLog log = new SysLog();
        log.setUserId(userId);
        log.setUsername(username);
        log.setAction(action);
        log.setDetail(detail != null && detail.length() > 480 ? detail.substring(0, 480) : detail);
        sysLogMapper.insert(log);
    }

    public PageResult<SysLog> page(long current, long size, String keyword) {
        boolean hasText = keyword != null && !keyword.isBlank();
        Page<SysLog> page = sysLogMapper.selectPage(new Page<>(current, size),
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysLog>()
                        .and(hasText, w -> w.like("action", keyword).or().like("username", keyword))
                        .orderByDesc("id"));
        return PageResult.of(page);
    }
}
