package com.sakanal.common.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject,"createTime", Date.class,new Date());

        this.strictUpdateFill(metaObject,"updateTime", Date.class,new Date());
        this.strictUpdateFill(metaObject,"modifyTime", Date.class,new Date());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject,"updateTime", Date.class,new Date());
        this.strictUpdateFill(metaObject,"modifyTime", Date.class,new Date());
    }
}
