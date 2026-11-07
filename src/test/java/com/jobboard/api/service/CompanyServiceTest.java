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
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CompanyServiceTest {
    private final CompanyRepository companies = mock(CompanyRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final CompanyMapper mapper = mock(CompanyMapper.class);
    private final CompanyService service = new CompanyService(companies, users, mapper);

    @Test
    void create_trimsNameAndPersistsCompany() {
        User owner = new User(); owner.setId(4L);
        when(companies.existsByOwnerId(4L)).thenReturn(false);
        when(companies.existsByNameIgnoreCase("Acme")).thenReturn(false);
        when(users.findById(4L)).thenReturn(Optional.of(owner));
        when(companies.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new CompanyResponse(1L, 4L, "Acme", null, null, null, null, null));

        CompanyResponse response = service.create(4L, new CompanyRequest(" Acme ", null, null, null, null));

        assertThat(response.name()).isEqualTo("Acme");
        verify(companies).save(argThat(company -> company.getName().equals("Acme")
                && company.getOwner().getId().equals(4L)));
    }

    @Test
    void create_rejectsSecondCompanyForOwner() {
        when(companies.existsByOwnerId(4L)).thenReturn(true);
        assertThatThrownBy(() -> service.create(4L, new CompanyRequest("Acme", null, null, null, null)))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(users);
    }

    @Test
    void create_checksNormalizedNameForConflict() {
        when(companies.existsByOwnerId(4L)).thenReturn(false);
        when(companies.existsByNameIgnoreCase("Acme")).thenReturn(true);
        assertThatThrownBy(() -> service.create(4L, new CompanyRequest(" Acme ", null, null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_missingCompanyReturnsNotFound() {
        when(companies.findByOwnerId(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.update(9L, new CompanyRequest("Acme", null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
