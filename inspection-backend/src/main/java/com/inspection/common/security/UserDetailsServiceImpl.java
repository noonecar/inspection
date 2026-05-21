package com.inspection.common.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inspection.entity.SysUser;
import com.inspection.mapper.SysUserMapper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private final SysUserMapper userMapper;

    public UserDetailsServiceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getEnabled, true));
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在");
        }
        return new User(user.getUsername(), user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
    }
}
