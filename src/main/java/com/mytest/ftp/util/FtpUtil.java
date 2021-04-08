package com.mytest.ftp.util;

import com.mytest.ftp.bean.FtpBean;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.SocketException;

/**
 * @author zoulinjun
 * @title: FtpUtil
 * @projectName pospweb
 * @description: TODO
 * @date 2021/3/8 10:40
 */
@Slf4j
public class FtpUtil {
    private static FTPClient ftpClient;

    public static FTPClient connectionFTPServer(FtpBean ftpBean) throws IOException {
        ftpClient = new FTPClient();
        try {
            ftpClient.connect(ftpBean.getFtpIp(), ftpBean.getFtpPort());
            ftpClient.login(ftpBean.getFtpUser(), ftpBean.getFtpPassword());
            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                log.info("==============未连接到FTP，用户名或密码错误=================");
                ftpClient.disconnect();
                throw new RuntimeException("未连接到FTP，用户名或密码错误");
            } else {
                log.info("==============连接到FTP成功=================");
            }
        } catch (SocketException e) {
            log.error("==============FTP的IP地址错误==============");
            throw e;
        } catch (IOException e) {
            log.error("==============FTP的端口错误==============");
            throw e;
        }
        return ftpClient;
    }


    /**
     * 切换到父目录
     *
     * @return 切换结果 true：成功， false：失败
     */
    private static boolean changeToParentDir() {
        if (!ftpClient.isConnected()) {
            return false;
        }
        try {
            return ftpClient.changeToParentDirectory();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 改变当前目录到指定目录
     *
     * @param dir
     *            目的目录
     * @return 切换结果 true：成功，false：失败
     */
    private static boolean cd(String dir) {
        if (!ftpClient.isConnected()) {
            return false;
        }
        try {
            return ftpClient.changeWorkingDirectory(dir);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取目录下所有的文件名称
     *
     * @param filePath
     *            指定的目录
     * @return 文件列表,或者null
     */
    private static FTPFile[] getFileList(String filePath) {
        if (!ftpClient.isConnected()) {
            return null;
        }
        try {
            return ftpClient.listFiles(filePath);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 切换工作目录
     *
     * @param ftpPath
     *            目的目录
     * @return 切换结果
     */
    public static boolean changeDir(String ftpPath) {
        if (!ftpClient.isConnected()) {
            return false;
        }
        try {
            // 将路径中的斜杠统一
            char[] chars = ftpPath.toCharArray();
            StringBuffer sbStr = new StringBuffer(256);
            for (int i = 0; i < chars.length; i++) {
                if ('\\' == chars[i]) {
                    sbStr.append('/');
                } else {
                    sbStr.append(chars[i]);
                }
            }
            ftpPath = sbStr.toString();
            if (ftpPath.indexOf('/') == -1) {
                // 只有一层目录
                ftpClient.changeWorkingDirectory(new String(ftpPath.getBytes(), "iso-8859-1"));
            } else {
                // 多层目录循环创建
                String[] paths = ftpPath.split("/");
                for (int i = 0; i < paths.length; i++) {
                    ftpClient.changeWorkingDirectory(new String(paths[i].getBytes(), "iso-8859-1"));
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 循环创建目录，并且创建完目录后，设置工作目录为当前创建的目录下
     *
     * @param ftpPath
     *            需要创建的目录
     * @return
     */
    public static boolean mkDir(String ftpPath) {
        if (ftpClient ==null || !ftpClient.isConnected()) {
            return false;
        }
        try {
            // 将路径中的斜杠统一
            char[] chars = ftpPath.toCharArray();
            StringBuffer sbStr = new StringBuffer(256);
            for (int i = 0; i < chars.length; i++) {
                if ('\\' == chars[i]) {
                    sbStr.append('/');
                } else {
                    sbStr.append(chars[i]);
                }
            }
            ftpPath = sbStr.toString();
            if (ftpPath.indexOf('/') == -1) {
                // 只有一层目录
                ftpClient.makeDirectory(new String(ftpPath.getBytes(), "iso-8859-1"));
                ftpClient.changeWorkingDirectory(new String(ftpPath.getBytes(), "iso-8859-1"));
            } else {
                // 多层目录循环创建
                String[] paths = ftpPath.split("/");
                for (int i = 0; i < paths.length; i++) {
                    ftpClient.makeDirectory(new String(paths[i].getBytes(), "iso-8859-1"));
                    ftpClient.changeWorkingDirectory(new String(paths[i].getBytes(), "iso-8859-1"));
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 上传单个文件
     *
     * @param file
     *            单个文件
     * @param newFileName
     *            新文件名
     * @param folder
     *            自定义保存的文件夹
     * @return 上传结果
     */
    public static boolean uploadSingleAttachment(File file, String newFileName, String folder,FtpBean ftpBean) throws IOException {
        FtpUtil.connectionFTPServer(ftpBean);
        if (!ftpClient.isConnected()) {
            log.info("==============FTP服务已断开==============");
            return false;
        }
        boolean result = false;
        if (ftpClient != null) {
            FileInputStream fileInputStream = null;
            try {
                fileInputStream = new FileInputStream(file);
                if(StringUtils.isNotEmpty(folder)){
                    FtpUtil.mkDir(folder);
                }
                ftpClient.setBufferSize(100000);
                ftpClient.setControlEncoding("utf-8");
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                ftpClient.enterLocalPassiveMode();
                result = ftpClient.storeFile(new String(newFileName.getBytes(), "iso-8859-1"), fileInputStream);
            } catch (Exception e) {
                FtpUtil.close();
                log.error("",e);
                return false;
            } finally {
                try {
                    if (ftpClient.isConnected()) {
                        FtpUtil.close();
                    }
                    fileInputStream.close();
//                    if(file.exists()){
//                        file.delete();
//                    }
                } catch (IOException e) {
                    log.error("",e);
                }
            }
        }
        return result;
    }
    /**
     * 上传多个文件
     *
     * @param files
     *            多文件流对象数组
     * @param newFileNames
     *            多文件名数组（与流对象数组一一对应）
     * @param folder
     *            自定义保存的文件夹
     * @return 上传结果
     */
    public static String[] uploadBatchAttachment(File[] files , String[] newFileNames, String folder,FtpBean ftpBean) throws IOException {
        String[] filesSaveUrls = new String[files.length];
        FtpUtil.connectionFTPServer(ftpBean);
        if (!ftpClient.isConnected()) {
            log.info("==============FTP服务已断开==============");
            return null;
        }
        if (ftpClient != null) {
            FileInputStream fileInputStream = null;
            try {
                if(StringUtils.isNotEmpty(folder)){
                    FtpUtil.mkDir(folder);
                }
                for (int i = 0; i < files.length; i++) {
                    filesSaveUrls[i] = ftpBean.getFtpPath();
                    fileInputStream = new FileInputStream(files[i]);
                    if(StringUtils.isNotEmpty(folder)){
                        filesSaveUrls[i] += "/"+folder;
                    }
                    ftpClient.setBufferSize(100000);
                    ftpClient.setControlEncoding("utf-8");
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                    ftpClient.storeFile(new String(newFileNames[i].getBytes(), "iso-8859-1"), fileInputStream);
                    filesSaveUrls[i] += "/"+newFileNames[i];
                }
            } catch (Exception e) {
                FtpUtil.close();
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (ftpClient.isConnected()) {
                        FtpUtil.close();
                    }
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return filesSaveUrls;
    }
    /**
     * 上传单个文件
     *
     * @param multipartFile
     *            多文件流对象数组
     * @param newFileName
     *            多文件名数组（与流对象数组一一对应）
     * @param folder
     *            自定义保存的文件夹
     * @return 上传结果
     */
    public static String uploadSingleAttachment(MultipartFile multipartFile, String newFileName, String folder,FtpBean ftpBean) throws IOException {
        String filesSaveUrl = null;
        FtpUtil.connectionFTPServer(ftpBean);
        if (!ftpClient.isConnected()) {
            log.info("==============FTP服务已断开==============");
            return null;
        }
        if (ftpClient != null) {
            try {
                if(StringUtils.isNotEmpty(folder)){
                    FtpUtil.mkDir(folder);
                }
                filesSaveUrl = ftpBean.getFtpPath();
                if(StringUtils.isNotEmpty(folder)){
                    filesSaveUrl += "/"+folder;
                }
                ftpClient.setBufferSize(100000);
                ftpClient.setControlEncoding("utf-8");
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                ftpClient.storeFile(new String(newFileName.getBytes(), "iso-8859-1"), multipartFile.getInputStream());
                filesSaveUrl += "/"+newFileName;
            } catch (Exception e) {
                FtpUtil.close();
                e.printStackTrace();
                return null;
            } finally {
                if (ftpClient.isConnected()) {
                    FtpUtil.close();
                }
            }
        }
        return filesSaveUrl;
    }
    /**
     * 上传多个文件
     *
     * @param multipartFiles
     *            多文件流对象数组
     * @param newFileNames
     *            多文件名数组（与流对象数组一一对应）
     * @param folder
     *            自定义保存的文件夹
     * @return 上传结果
     */
    public static String[] uploadBatchAttachment(MultipartFile[] multipartFiles , String[] newFileNames, String folder,FtpBean ftpBean) throws IOException {
        String[] filesSaveUrls = new String[multipartFiles.length];
        FtpUtil.connectionFTPServer(ftpBean);
        if (!ftpClient.isConnected()) {
            log.info("==============FTP服务已断开==============");
            return null;
        }
        if (ftpClient != null) {
            InputStream inputStream = null;
            try {
                if(StringUtils.isNotEmpty(folder)){
                    FtpUtil.mkDir(folder);
                }
                for (int i = 0; i < multipartFiles.length; i++) {
                    filesSaveUrls[i] = ftpBean.getFtpPath();
                    inputStream = multipartFiles[i].getInputStream();
                    if(StringUtils.isNotEmpty(folder)){
                        filesSaveUrls[i] += "/"+folder;
                    }
                    ftpClient.setBufferSize(100000);
                    ftpClient.setControlEncoding("utf-8");
                    ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                    ftpClient.storeFile(new String(newFileNames[i].getBytes(), "iso-8859-1"), inputStream);
                    filesSaveUrls[i] += "/"+newFileNames[i];
                }
            } catch (Exception e) {
                FtpUtil.close();
                e.printStackTrace();
                return null;
            } finally {
                try {
                    if (ftpClient.isConnected()) {
                        FtpUtil.close();
                    }
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return filesSaveUrls;
    }
    /**
     * 将InputStream写入本地文件
     * @param destination 写入本地目录
     * @param input    输入流
     * @throws IOException
     */
    public static void writeToLocal(String destination, InputStream input)
            throws IOException {
        int index;
        byte[] bytes = new byte[2048];
        FileOutputStream downloadFile = new FileOutputStream(destination);
        while ((index = input.read(bytes)) != -1) {
            downloadFile.write(bytes, 0, index);
            downloadFile.flush();
        }
        downloadFile.close();
        input.close();
    }

    /**
     * 根据文件url下载文件
     *
     * @param path 被下载文件url
     * @return 文件流对象
     * @throws IOException
     */
    public static final InputStream downLoadFile(String path,String filaName,FtpBean ftpBean) throws IOException {
        log.info("开始下载");
        FtpUtil.connectionFTPServer(ftpBean);
        if (!ftpClient.isConnected()) {
            log.info("==============FTP服务已断开==============");
            return null;
        }
        try {
            FtpUtil.changeDir(path);
            ftpClient.setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
            ftpClient.setBufferSize(100000);
            ftpClient.setControlEncoding("UTF-8");
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            InputStream inputStream = ftpClient.retrieveFileStream(path + filaName);
            log.info("下载成功");
            return inputStream;
        } catch (IOException e) {
            log.info("==============获取文件异常==============");
            log.error("获取文件异常",e);
            return null;
        }finally{
            if (ftpClient.isConnected()) {
                    new Thread(()->{
                        log.info("开始删除");
                        boolean del = false;
                        try {
                            del = ftpClient.deleteFile(path + filaName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        log.info("删除结果:" + del);
                        FtpUtil.close();
                        log.info("断开连接");
                    }).start();
                    //boolean dsuccess=ftpClient.deleteFile(remoteFileName) 这里的路径应该是全路径 remote
//
//
//                    boolean dsuccess=ftpClient.deleteFile(new String(()).getBytes(“gb2312″, “iso-8859-1″));
//
//                    if (!dsuccess) {
//                        System.out.println(“删除FTP文件失败,可能是路径编码错误了，现在路径编码转换”+gb2312);
//
//                        return UploadStatus.Delete_Remote_Faild;
//
//                    }else{
//                        System.out.println(“删除ftp旧文件成功:”+remoteFileName);
//
//                    }
            }

        }
    }

    /**
     * 返回FTP目录下的文件列表
     *
     * @param pathName
     * @return
     */
    public static String[] getFileNameList(String pathName) {
        if (!ftpClient.isConnected()) {
            return null;
        }
        try {
            return ftpClient.listNames(pathName);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 删除FTP上的文件
     *
     * @param ftpDirAndFileName
     *            路径开头不能加/，比如应该是test/filename1
     * @return
     */
    public static boolean deleteFile(String ftpDirAndFileName,FtpBean ftpBean) throws IOException {
        if (!ftpClient.isConnected()) {
            FtpUtil.connectionFTPServer(ftpBean);
        }
        try {
            return ftpClient.deleteFile(ftpDirAndFileName);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 删除FTP目录
     *
     * @param ftpDirectory
     * @return
     */
    public static boolean deleteDirectory(String ftpDirectory) {
        if (!ftpClient.isConnected()) {
            return false;
        }
        try {
            return ftpClient.removeDirectory(ftpDirectory);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 关闭链接
     */
    public static void close() {
        try {
            if (ftpClient != null && ftpClient.isConnected()) {
                ftpClient.disconnect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }





}