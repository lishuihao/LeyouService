package com.leyou.user.service;

import com.leyou.common.enums.ExceptionEnums;
import com.leyou.common.exception.LyException;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public Boolean checkData(String data, Integer type) {
        //判断数据类型
        User user =new User();
        switch (type){
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
            default:
                throw new LyException(ExceptionEnums.USER_DATA_TYPE_ERROR);
        }
        return userMapper.selectCount(user) ==0;
    }

    public void register(User user, String code) {
        //1.校验验证码
        if(!StringUtils.equals(code,"123456")){
            throw new LyException(ExceptionEnums.INVALID_VERYFY_CODE);
        }
        //2.对密码进行加密
        //2.1生成盐
        String salt = CodecUtils.generateSalt();
        //2.2将盐记录下来
        user.setSalt(salt);
        //2.3对密码加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));
        //3.将数据写入数据库
        user.setCreated(new Date());
        userMapper.insert(user);
    }

    public User queryUserByUsernameAndPassword(String username, String password) {
        User user = new User();
        user.setUsername(username);
        User one = userMapper.selectOne(user);
        //校验
        if (one == null){
            throw new LyException(ExceptionEnums.INVALID_USERANDPASSWORD);
        }
        //校验密码
        if (!StringUtils.equals(one.getPassword(), CodecUtils.md5Hex(password,one.getSalt()))) {
            throw new LyException(ExceptionEnums.INVALID_USERANDPASSWORD);
        }
        //用户名密码正确
        return one;
    }
}
