package com.mytest.ftp.controller;

import com.alibaba.fastjson.JSONObject;
import com.mytest.ftp.bean.FtpBean;
import com.mytest.ftp.util.FtpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.Enumeration;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author zoulinjun
 * @title: TestController
 * @projectName ftp
 * @description: TODO
 * @date 2021/3/19 9:36
 */
@RestController
@Slf4j
public class TestController {

    @RequestMapping(value = "test.do")
    public JSONObject test(@RequestBody JSONObject jsonObject){
        log.info(jsonObject.toJSONString());
        FtpBean ftpBean = new FtpBean();
        String ftpIp= jsonObject.getString("ftpIp");
        int ftpPort= jsonObject.getInteger("ftpPort");
        String ftpUser= jsonObject.getString("ftpUser");
        String ftpPassword= jsonObject.getString("ftpPassword");
        String ftpPath = jsonObject.getString("ftpPath");
        String ftpFileName = jsonObject.getString("ftpFileName");
        String gbPath = jsonObject.getString("gbPath");

        ftpBean.setFtpIp(ftpIp)
                .setFtpPort(ftpPort)
                .setFtpUser(ftpUser)
                .setFtpPassword(ftpPassword);
        InputStream is = null;
        try {
//            is = FtpUtil.downLoadFile("/FTP_UPLOAD/2021/3/18/","5765f43c8b944149b6dcff11e6d8de9c_20210318203240915.zip",ftpBean);
            is = FtpUtil.downLoadFile(ftpPath,ftpFileName,ftpBean);
        } catch (IOException e) {
            e.printStackTrace();
        }


//        String localFilePath = localPath + File.separator + ftpFileName);
//        try {
//            FtpUtil.writeToLocal(localFilePath,is);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        
        File zipFile = null;
        FileOutputStream fileOutputStream = null;
        try{
//            zipFile = new File("C:\\zlj\\doc\\dev\\FB\\abc\\111.zip");
//            zipFile = new File(localPath);
//            if(!zipFile.exists()){
//                zipFile.mkdirs();
//            }

//            zipFile = new File(localPath + File.separator + UUID.randomUUID().toString() + ".zip");
            zipFile = File.createTempFile("tmp",".zip");
            fileOutputStream = new FileOutputStream(zipFile);
            int count;
            byte data[] = new byte[8192];
            while ((count = is.read(data, 0, 8192)) != -1) {
                fileOutputStream.write(data, 0, count);
            }
//            zipFile.deleteOnExit();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(is != null){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if(fileOutputStream != null){
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        
        File outFile = new File(gbPath);
        unpack(zipFile,outFile,"UTF-8");
        log.info("解压完成");
//        new Thread(()->{
//            log.info("删除结果:" + del);
//            zipFile.deleteOnExit();
//            log.info("断开连接");
//        }).start();
        zipFile.deleteOnExit();
        log.info("删除完成");
        log.info("请求结束," + gbPath + "查看下载解压后的文件");
        return jsonObject;
    }


    private void unpack(File zip, File outputDir, String charsetName) {

        FileOutputStream out = null;
        InputStream in = null;
        //读出文件数据
        ZipFile zipFileData = null;
        ZipFile zipFile = null;
        try {
            //若目标保存文件位置不存在
            if (outputDir != null|| !outputDir.exists()) {
                outputDir.mkdirs();
            }
            zipFile = new ZipFile(zip.getPath());
//            if (charsetName != null && charsetName != "") {
//                zipFile = new ZipFile(zip.getPath(), Charset.forName(charsetName));
//            } else {
//                zipFile = new ZipFile(zip.getPath(), Charset.forName("UTF-8"));
//            }
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            //处理创建文件夹
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String filePath = "";

                if (outputDir == null) {
                    filePath = zip.getParentFile().getPath() + File.separator + entry.getName();
                } else {
                    filePath = outputDir.getPath() + File.separator + entry.getName();
                }
                File file = new File(filePath);
                File parentFile = file.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                if (parentFile.isDirectory()) {
                    continue;
                }
            }
            zipFileData = new ZipFile(zip.getPath());
//            if (charsetName != null && charsetName != "") {
//                zipFileData = new ZipFile(zip.getPath(), Charset.forName(charsetName));
//            } else {
//                zipFileData = new ZipFile(zip.getPath(), Charset.forName("UTF-8"));
//            }
            Enumeration<? extends ZipEntry> entriesData = zipFileData.entries();
            while (entriesData.hasMoreElements()) {
                ZipEntry entry = entriesData.nextElement();
                in = zipFile.getInputStream(entry);
                String filePath = "";
                if (outputDir == null) {
                    filePath = zip.getParentFile().getPath() + File.separator + entry.getName();
                } else {
                    filePath = outputDir.getPath() + File.separator + entry.getName();
                }
                File file = new File(filePath);
                if (file.isDirectory()) {
                    continue;
                }
                out = new FileOutputStream(filePath);
                int len = -1;
                byte[] bytes = new byte[1024];
                while ((len = in.read(bytes)) != -1) {
                    out.write(bytes, 0, len);
                }
                out.flush();
                in.close();
                out.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {


            try {
                if(out != null){
                    out.close();
                }
                if(in != null){

                    in.close();
                }
                if(zipFile != null){

                    zipFile.close();
                }
                if(zipFileData != null){

                    zipFileData.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
