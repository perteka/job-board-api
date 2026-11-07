package com.jobboard.api.util;

import com.jobboard.api.exception.BadRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

public final class PageableSupport {

    private PageableSupport() {
    }

    public static Pageable requireAllowedSort(Pageable pageable, Set<String> allowedProperties) {
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new BadRequestException("Geçersiz sıralama alanı: " + order.getProperty());
            }
        }
        return pageable;
    }
}
