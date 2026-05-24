package com.example.PortPick_SERVER.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpsertRequest {

    private String name;
    private String organizationName;
    private boolean noOrganization;
    private String jobRole;
    private String careerType;
    private String careerRange;
    private boolean deleteProfileImage;
}
