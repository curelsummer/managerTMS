package cc.mrbird.febs.common.authentication;

import cc.mrbird.febs.common.domain.ActiveUser;
import cc.mrbird.febs.common.domain.FebsConstant;
import cc.mrbird.febs.common.properties.FebsProperties;
import cc.mrbird.febs.common.service.RedisService;
import cc.mrbird.febs.common.utils.DateUtil;
import cc.mrbird.febs.common.utils.FebsUtil;
import cc.mrbird.febs.common.utils.HttpContextUtil;
import cc.mrbird.febs.common.utils.IPUtil;
import cc.mrbird.febs.system.domain.User;
import cc.mrbird.febs.system.manager.UserManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 自定义实现 ShiroRealm，包含认证和授权两大模块
 *
 * @author MrBird
 */
@Slf4j
public class ShiroRealm extends AuthorizingRealm {

    @Autowired
    private RedisService redisService;
    @Autowired
    private UserManager userManager;
    @Autowired
    private FebsProperties properties;
    @Autowired
    private ObjectMapper mapper;

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JWTToken;
    }

    /**`
     * 授权模块，获取用户角色和权限
     *
     * @param token token
     * @return AuthorizationInfo 权限信息
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection token) {
        String username = JWTUtil.getUsername(token.toString());

        SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();

        // 获取用户角色集
        Set<String> roleSet = userManager.getUserRoles(username);
        simpleAuthorizationInfo.setRoles(roleSet);

        // 获取用户权限集
        Set<String> permissionSet = userManager.getUserPermissions(username);
        simpleAuthorizationInfo.setStringPermissions(permissionSet);
        return simpleAuthorizationInfo;
    }

    /**
     * 用户认证
     *
     * @param authenticationToken 身份认证 token
     * @return AuthenticationInfo 身份认证信息
     * @throws AuthenticationException 认证相关异常
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authenticationToken) throws AuthenticationException {
        // 这里的 token是从 JWTFilter 的 executeLogin 方法传递过来的，已经经过了解密
        String token = (String) authenticationToken.getCredentials();

        // 从 redis里获取这个 token
        HttpServletRequest request = HttpContextUtil.getHttpServletRequest();
        String ip = IPUtil.getIpAddr(request);

        String encryptToken = FebsUtil.encryptToken(token);
        String encryptTokenInRedis = null;
        try {
            encryptTokenInRedis = redisService.get(FebsConstant.TOKEN_CACHE_PREFIX + encryptToken + "." + ip);
        } catch (Exception ignore) {
        }
        // 如果找不到，说明已经失效
        if (StringUtils.isBlank(encryptTokenInRedis))
            throw new AuthenticationException("token已经过期");

        String username = JWTUtil.getUsername(token);

        if (StringUtils.isBlank(username))
            throw new AuthenticationException("token校验不通过");

        // 通过用户名查询用户信息
        User user = userManager.getUser(username);

        if (user == null)
            throw new AuthenticationException("用户名或密码错误");
        if (!JWTUtil.verify(token, username, user.getPassword()))
            throw new AuthenticationException("token校验不通过");
        
        // 认证成功后，刷新token过期时间（滑动过期机制）
        refreshTokenExpireTime(encryptToken, ip);
        
        return new SimpleAuthenticationInfo(token, token, "febs_shiro_realm");
    }

    /**
     * 刷新token过期时间（滑动过期机制）
     * 每次用户请求通过认证后，将token过期时间延长1小时
     *
     * @param encryptToken 加密后的token
     * @param ip 用户IP地址
     */
    private void refreshTokenExpireTime(String encryptToken, String ip) {
        try {
            String tokenKey = FebsConstant.TOKEN_CACHE_PREFIX + encryptToken + "." + ip;
            Long jwtTimeOut = properties.getShiro().getJwtTimeOut() * 1000L;
            
            // 刷新Redis中token的过期时间
            redisService.pexpire(tokenKey, jwtTimeOut);
            
            // 更新zset中的过期时间戳
            // 计算新的过期时间戳
            LocalDateTime newExpireTime = LocalDateTime.now().plusSeconds(properties.getShiro().getJwtTimeOut());
            String newExpireTimeStr = DateUtil.formatFullTime(newExpireTime);
            Double newExpireScore = Double.valueOf(newExpireTimeStr);
            
            // 从zset中查找并更新用户记录
            Set<String> userOnlineStringSet = redisService.zrangeByScore(
                FebsConstant.ACTIVE_USERS_ZSET_PREFIX, "-inf", "+inf");
            
            for (String userOnlineString : userOnlineStringSet) {
                try {
                    ActiveUser activeUser = mapper.readValue(userOnlineString, ActiveUser.class);
                    // 通过token和ip匹配用户记录
                    if (StringUtils.equals(activeUser.getToken(), encryptToken) 
                        && StringUtils.equals(activeUser.getIp(), ip)) {
                        // 删除旧记录
                        redisService.zrem(FebsConstant.ACTIVE_USERS_ZSET_PREFIX, userOnlineString);
                        // 添加新记录（新的过期时间戳）
                        redisService.zadd(FebsConstant.ACTIVE_USERS_ZSET_PREFIX, 
                            newExpireScore, userOnlineString);
                        log.debug("刷新用户token过期时间: username={}, ip={}", 
                            activeUser.getUsername(), ip);
                        break;
                    }
                } catch (Exception e) {
                    log.warn("解析用户在线记录失败: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            // 刷新失败不影响认证流程，只记录日志
            log.warn("刷新token过期时间失败: {}", e.getMessage());
        }
    }
}
