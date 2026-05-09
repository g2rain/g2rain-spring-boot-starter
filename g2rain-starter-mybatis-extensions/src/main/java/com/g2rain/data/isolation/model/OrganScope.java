package com.g2rain.data.isolation.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author alpha
 * @since 2026/5/7
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganScope {
    private Long sourceOrganId;
    private Long targetOrganId;
}
