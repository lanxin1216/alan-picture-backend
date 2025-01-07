package com.alan.alanpicturebackend.controller;

import com.alan.alanpicturebackend.annotation.AuthCheck;
import com.alan.alanpicturebackend.common.BaseResponse;
import com.alan.alanpicturebackend.common.DeleteRequest;
import com.alan.alanpicturebackend.common.ResultUtils;
import com.alan.alanpicturebackend.constant.UserConstant;
import com.alan.alanpicturebackend.exception.BusinessException;
import com.alan.alanpicturebackend.exception.ErrorCode;
import com.alan.alanpicturebackend.exception.ThrowUtils;
import com.alan.alanpicturebackend.model.dto.user.*;
import com.alan.alanpicturebackend.model.entity.User;
import com.alan.alanpicturebackend.model.vo.LoginUserVO;
import com.alan.alanpicturebackend.model.vo.UserVO;
import com.alan.alanpicturebackend.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户接口
 *
 * @author alan
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();

        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     *
     * @param userLoginRequest 用户登录请求
     * @param request          请求
     * @return 返回脱敏后的用户信息
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户信息
     *
     * @param request
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销（退出登录）
     *
     * @param request 请求
     * @return 返回结果
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        // 使用自定义工具类判断请求参数是否为 null
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 添加用户 (管理员）
     *
     * @param userAddRequest 用户添加请求
     * @return 返回用户id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        // 使用自定义工具类判断请求参数是否为 null
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        // 拷贝请求参数到实体类
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码：12345678
        final String DEFAULT_PASSWORD = "12345678";
        // 加密密码
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        // 判断用户是否添加成功
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户(管理员)
     *
     * @param id 用户id
     * @return 返回用户信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取用户VO (包装类)
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 调用上方的方法
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户(管理员）
     *
     * @param deleteRequest 删除请求
     * @return 返回删除结果
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户信息(管理员）
     *
     * @param userUpdateRequest 更新请求
     * @return 更新结果
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        // 拷贝请求参数到实体类
        BeanUtils.copyProperties(userUpdateRequest, user);
        // 调用 mybatis-plus 的 updateById 方法
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户列表(管理员)
     *
     * @param userQueryRequest 查询请求参数
     * @return 返回查询的参数结果
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 校验查询请求是否为空
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);

        // 获取当前页码和页面大小
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();

        // 执行用户列表的分页查询
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));

        // 初始化用户视图对象的分页结果
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());

        // 将查询到的用户记录转换为用户视图对象列表
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());

        // 设置用户视图对象的记录
        userVOPage.setRecords(userVOList);

        // 返回用户视图对象的分页结果
        return ResultUtils.success(userVOPage);
    }

}
