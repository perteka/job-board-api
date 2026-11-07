package com.jobboard.api.service;

import com.jobboard.api.dto.request.CompanyRequest;
import com.jobboard.api.dto.response.CompanyResponse;
import com.jobboard.api.entity.Company;
import com.jobboard.api.entity.User;
import com.jobboard.api.exception.ConflictException;
import com.jobboard.api.exception.ResourceNotFoundException;
import com.jobboard.api.mapper.CompanyMapper;
import com.jobboard.api.repository.CompanyRepository;
import com.jobboard.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final CompanyMapper companyMapper;

    @Transactional
    public CompanyResponse create(Long ownerId, CompanyRequest request) {
        String normalizedName = request.name().trim();
        if (companyRepository.existsByOwnerId(ownerId)) {
            throw new ConflictException("Bu kullanıcının zaten bir firması var");
        }
        if (companyRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Bu firma adı zaten kullanılıyor");
        }

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Kullanıcı bulunamadı"));

        Company company = new Company();
        company.setOwner(owner);
        apply(company, request);
        return companyMapper.toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse update(Long ownerId, CompanyRequest request) {
        String normalizedName = request.name().trim();
        Company company = companyRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Firma bulunamadı"));

        if (!company.getName().equalsIgnoreCase(normalizedName)
                && companyRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ConflictException("Bu firma adı zaten kullanılıyor");
        }

        apply(company, request);
        return companyMapper.toResponse(company);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getMyCompany(Long ownerId) {
        Company company = companyRepository.findByOwnerId(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Firma bulunamadı"));
        return companyMapper.toResponse(company);
    }

    @Transactional(readOnly = true)
    public CompanyResponse getById(Long id) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Firma bulunamadı"));
        return companyMapper.toResponse(company);
    }

    private void apply(Company company, CompanyRequest request) {
        company.setName(request.name().trim());
        company.setDescription(request.description());
        company.setWebsite(request.website());
        company.setLocation(request.location());
        company.setLogoUrl(request.logoUrl());
    }
}
