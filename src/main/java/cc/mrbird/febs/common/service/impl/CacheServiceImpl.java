package cc.mrbird.febs.common.service.impl;

import cc.mrbird.febs.common.domain.FebsConstant;
import cc.mrbird.febs.common.service.CacheService;
import cc.mrbird.febs.common.service.RedisService;
import cc.mrbird.febs.system.dao.UserMapper;
import cc.mrbird.febs.system.domain.Menu;
import cc.mrbird.febs.system.domain.Role;
import cc.mrbird.febs.system.domain.User;
import cc.mrbird.febs.system.domain.UserConfig;
import cc.mrbird.febs.system.service.MenuService;
import cc.mrbird.febs.system.service.RoleService;
import cc.mrbird.febs.system.service.UserConfigService;
import cc.mrbird.febs.system.service.UserService;
import cn.hutool.core.collection.CollUtil;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service("cacheService")
public class CacheServiceImpl implements CacheService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private RoleService roleService;

    @Autowired
    private MenuService menuService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserConfigService userConfigService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper mapper;

    @Override
    public void testConnect() throws Exception {
        this.redisService.exists("test");
    }

    @Override
    public User getUser(String username) throws Exception {
        String key = FebsConstant.USER_CACHE_PREFIX + username.toLowerCase();
        String userString = redisService.get(key);
        if (StringUtils.isBlank(userString)) {
            User dbUser = userMapper.findDetail(username);
            if (dbUser != null) {
                redisService.set(key, mapper.writeValueAsString(dbUser), 1800L);
            }
            return dbUser;   // 允许返回 null（用户不存在）
        }
        return mapper.readValue(userString, User.class);
    }

    @Override
    public List<Role> getRoles(String username) throws Exception {
        String key = FebsConstant.USER_ROLE_CACHE_PREFIX + username.toLowerCase();
        String roleListString = redisService.get(key);
        if (StringUtils.isBlank(roleListString)) {
            List<Role> dbRoles = roleService.findUserRole(username);
            if (CollUtil.isNotEmpty(dbRoles)) {
                redisService.set(key, mapper.writeValueAsString(dbRoles), 1800L);
            }
            return CollUtil.isEmpty(dbRoles) ? Collections.emptyList() : dbRoles;
        }
        JavaType type = mapper.getTypeFactory().constructParametricType(List.class, Role.class);
        return mapper.readValue(roleListString, type);
    }

    @Override
    public List<Menu> getPermissions(String username) throws Exception {
        String key = FebsConstant.USER_PERMISSION_CACHE_PREFIX + username.toLowerCase();
        String permissionListString = redisService.get(key);

        // 1. Redis 未命中，走数据库
        if (StringUtils.isBlank(permissionListString)) {
            List<Menu> dbPerms = menuService.findUserPermissions(username);
            if (CollUtil.isNotEmpty(dbPerms)) {
                // 2. 写回 Redis，30 min 过期
                redisService.set(key, mapper.writeValueAsString(dbPerms), 1800L);
            }
            // 3. 永不返回 null，避免上层强转异常
            return CollUtil.isEmpty(dbPerms) ? Collections.emptyList() : dbPerms;
        }

        // 4. Redis 命中，反序列化
        JavaType type = mapper.getTypeFactory().constructParametricType(List.class, Menu.class);
        return mapper.readValue(permissionListString, type);
    }

    @Override
    public UserConfig getUserConfig(String userId) throws Exception {
        String key = FebsConstant.USER_CONFIG_CACHE_PREFIX + userId;
        String userConfigString = redisService.get(key);
        if (StringUtils.isBlank(userConfigString)) {
            UserConfig dbConfig = userConfigService.findByUserId(userId);
            if (dbConfig != null) {
                redisService.set(key, mapper.writeValueAsString(dbConfig), 1800L);
            }
            return dbConfig;   // 允许返回 null（无配置）
        }
        return mapper.readValue(userConfigString, UserConfig.class);
    }

    @Override
    public void saveUser(User user) throws Exception {
        String username = user.getUsername();
        this.deleteUser(username);
        redisService.set(FebsConstant.USER_CACHE_PREFIX + username, mapper.writeValueAsString(user));
    }

    @Override
    public void saveUser(String username) throws Exception {
        User user = userMapper.findDetail(username);
        this.deleteUser(username);
        redisService.set(FebsConstant.USER_CACHE_PREFIX + username, mapper.writeValueAsString(user));
    }

    @Override
    public void saveRoles(String username) throws Exception {
        List<Role> roleList = this.roleService.findUserRole(username);
        if (!roleList.isEmpty()) {
            this.deleteRoles(username);
            redisService.set(FebsConstant.USER_ROLE_CACHE_PREFIX + username, mapper.writeValueAsString(roleList));
        }

    }

    @Override
    public void savePermissions(String username) throws Exception {
        List<Menu> permissionList = this.menuService.findUserPermissions(username);
        if (!permissionList.isEmpty()) {
            this.deletePermissions(username);
            redisService.set(FebsConstant.USER_PERMISSION_CACHE_PREFIX + username, mapper.writeValueAsString(permissionList));
        }
    }

    @Override
    public void saveUserConfigs(String userId) throws Exception {
        UserConfig userConfig = this.userConfigService.findByUserId(userId);
        if (userConfig != null) {
            this.deleteUserConfigs(userId);
            redisService.set(FebsConstant.USER_CONFIG_CACHE_PREFIX + userId, mapper.writeValueAsString(userConfig));
        }
    }

    @Override
    public void deleteUser(String username) throws Exception {
        username = username.toLowerCase();
        redisService.del(FebsConstant.USER_CACHE_PREFIX + username);
    }

    @Override
    public void deleteRoles(String username) throws Exception {
        username = username.toLowerCase();
        redisService.del(FebsConstant.USER_ROLE_CACHE_PREFIX + username);
    }

    @Override
    public void deletePermissions(String username) throws Exception {
        username = username.toLowerCase();
        redisService.del(FebsConstant.USER_PERMISSION_CACHE_PREFIX + username);
    }

    @Override
    public void deleteUserConfigs(String userId) throws Exception {
        redisService.del(FebsConstant.USER_CONFIG_CACHE_PREFIX + userId);
    }
}
