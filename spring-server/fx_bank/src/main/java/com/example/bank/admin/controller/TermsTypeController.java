package com.example.bank.admin.controller;

import com.example.bank.admin.dto.TermsTypeDto;
import com.example.bank.admin.service.TermsTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/terms-type")
@RequiredArgsConstructor
public class TermsTypeController {

    private final TermsTypeService typeServ;

    @GetMapping
    public ResponseEntity<List<TermsTypeDto>> getTypeList() {
        return ResponseEntity.ok(typeServ.getTypeList());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registerType(@RequestBody Map<String, String> body) {
        TermsTypeDto saved = typeServ.registerNewType(body.get("typeName"));
        return ResponseEntity.ok(Map.of(
                "typeNo", saved.getTypeNo(),
                "typeName", saved.getTypeName(),
                "message", "약관 종류가 추가되었습니다."
        ));
    }
}