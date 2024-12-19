package com.alan.alanpicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 权限枚举类
 */
@Getter
public enum UserRoleEnum {
    USER("普通用户", "user"),
    ADMIN("管理员", "admin");

    private final String txt;
    private final String value;

    UserRoleEnum(String txt, String value) {
        this.txt = txt;
        this.value = value;
    }

    /**
     * 根据枚举的值获取枚举
     *
     * @param value 枚举的值
     * @return 枚举
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum anEnum : UserRoleEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
