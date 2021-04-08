package com.mytest.ftp.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author zoulinjun
 * @title: FtpBean
 * @projectName ftp
 * @description: TODO
 * @date 2021/3/19 9:39
 */
@Data
@Accessors(chain = true)
public class FtpBean {

    private String ftpIp;
    private int ftpPort;
    private String ftpUser;
    private String ftpPassword;
    private String ftpPath;
    private String ftpDriveletter;

}
