package com.company.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DictionaryItemRequest {
    @NotBlank
    @Size(max = 100)
    private String dictType;
    @NotBlank
    @Size(max = 100)
    private String itemValue;
    @NotBlank
    @Size(max = 100)
    private String itemLabel;
    @Size(max = 500)
    private String remark;
    @NotNull
    private Integer sortOrder;
    private Boolean enabled;

    public String getDictType() { return dictType; }
    public void setDictType(String dictType) { this.dictType = dictType; }
    public String getItemValue() { return itemValue; }
    public void setItemValue(String itemValue) { this.itemValue = itemValue; }
    public String getItemLabel() { return itemLabel; }
    public void setItemLabel(String itemLabel) { this.itemLabel = itemLabel; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
