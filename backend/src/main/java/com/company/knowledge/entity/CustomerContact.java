package com.company.knowledge.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "kp_customer_contact")
public class CustomerContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerCompany customer;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String position;

    @Column(length = 50)
    private String department;

    @Column(length = 20)
    private String gender;

    @Column(length = 100)
    private String mobile;

    @Column(length = 100)
    private String officePhone;

    @Column(length = 200)
    private String email;

    @Column(length = 100)
    private String wechat;

    @Column(length = 100)
    private String qq;

    @Column(length = 20)
    private String decisionLevel;

    private Boolean primaryContact = false;

    @Column(length = 2000)
    private String notes;

    @Column(length = 100)
    private String createdBy;

    @Column(length = 100)
    private String updatedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public CustomerCompany getCustomer() { return customer; }
    public void setCustomer(CustomerCompany customer) { this.customer = customer; }
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
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
