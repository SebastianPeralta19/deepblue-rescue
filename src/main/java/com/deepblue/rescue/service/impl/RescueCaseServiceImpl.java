package com.deepblue.rescue.service.impl;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.RescueCaseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RescueCaseServiceImpl implements RescueCaseService {

    private final RescueCaseRepository repository;
    private final RescueCaseMapper mapper;

    public RescueCaseServiceImpl(RescueCaseRepository repository, RescueCaseMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public RescueCaseResponse findByCode(String caseCode) {
        return mapper.toResponse(getCase(caseCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RescueCaseResponse> findByStatus(RescueStatus status) {
        return repository.findByStatusOrderByRescueDateAsc(status)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public RescueCaseResponse changeStatus(String caseCode, ChangeRescueStatusRequest request) {
        if (request == null || request.status() == null) {
            throw new BusinessRuleException("Status is required");
        }

        RescueCase rescueCase = getCase(caseCode);
        RescueStatus current = rescueCase.getStatus();
        RescueStatus target = request.status();

        if (!current.canTransitionTo(target)) {
            throw new BusinessRuleException(
                    "Invalid status transition: " + current + " -> " + target);
        }

        rescueCase.setStatus(target);
        return mapper.toResponse(repository.save(rescueCase));
    }

    private RescueCase getCase(String caseCode) {
        return repository.findByCaseCode(caseCode)
                .orElseThrow(() -> new ResourceNotFoundException("Rescue case not found: " + caseCode));
    }
}
