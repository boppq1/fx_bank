package com.example.bank.admin.service;

import com.example.bank.admin.dao.ITermsTypeDao;
import com.example.bank.admin.dto.TermsTypeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TermsTypeService {

    private final ITermsTypeDao termsTypeDao;

    public List<TermsTypeDto> getTypeList() {
        return termsTypeDao.selectTypeList();
    }

    /** 새 약관 종류 등록. 이미 같은 이름이 있으면 새로 만들지 않고 안내만 함 */
    @Transactional
    public TermsTypeDto registerNewType(String typeName) {
        if (!StringUtils.hasText(typeName)) {
            throw new IllegalArgumentException("약관 종류명을 입력해주세요.");
        }
        String trimmed = typeName.trim();

        if (termsTypeDao.countByTypeName(trimmed) > 0) {
            throw new IllegalStateException("이미 존재하는 약관 종류입니다: " + trimmed);
        }

        TermsTypeDto dto = TermsTypeDto.builder().typeName(trimmed).build();
        termsTypeDao.insertType(dto);
        return dto;
    }
}