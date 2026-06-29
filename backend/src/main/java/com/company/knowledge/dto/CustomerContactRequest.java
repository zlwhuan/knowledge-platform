package com.company.knowledge.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerContactRequest {
    @NotBlank
    @Size(max = 100)
    private String name;
    @Size(max = 100)
    private String position;
    @Size(max = 100)
    private String department;
    private String gender;
    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String mobile;
    private String officePhone;
    @Email(message = "邮箱格式不正确")
    private String email;
    private String wechat;
    private String qq;
    private String decisionLevel;
    private Boolean primaryContact = false;
    private String notes;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }
    public String getOfficePhone() { return officePhone; }
    public void setOfficePhone(String officePhone) { this.officePhone = officePhone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getWechat() { return wechat; }
    public void setWechat(String wechat) { this.wechat = wechat; }
    public String getQq() { return qq; }
    public void setQq(String qq) { this.qq = qq; }
    public String getDecisionLevel() { return decisionLevel; }
    public void setDecisionLevel(String decisionLevel) { this.decisionLevel = decisionLevel; }
    public Boolean getPrimaryContact() { return primaryContact; }
    public void setPrimaryContact(Boolean primaryContact) { this.primaryContact = primaryContact; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
