package com.alan.alanpicturebackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 删除请求包装类
 *
 * @author alan
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
