package com.tripsy.api.filter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo {


    private int id;
    private String name;
    private String email;
    private String password;
    private String roles;

}