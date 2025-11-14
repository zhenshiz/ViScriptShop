package com.viscriptshop.util;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class FileScanner {
    /**
     * 递归扫描文件夹,获取所有指定后缀的文件
     *
     * @param rootDir 根目录
     * @param suffix  文件后缀,例如 ".shop"
     * @return 相对路径集合,不包含后缀名
     */
    public static Set<String> scanFilesWithSuffix(File rootDir, String suffix) {
        Set<String> result = new HashSet<>();
        if (!rootDir.isDirectory()) {
            return result;
        }
        scanRecursive(rootDir, rootDir, suffix, result);
        return result;
    }

    private static void scanRecursive(File rootDir, File currentDir, String suffix, Set<String> result) {
        File[] files = currentDir.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子文件夹
                scanRecursive(rootDir, file, suffix, result);
            } else if (file.isFile() && file.getName().endsWith(suffix)) {
                // 计算相对路径
                String relativePath = getRelativePath(rootDir, file);
                // 移除后缀名
                String nameWithoutSuffix = relativePath.substring(0, relativePath.length() - suffix.length());
                result.add(nameWithoutSuffix);
            }
        }
    }

    private static String getRelativePath(File rootDir, File file) {
        String rootPath = rootDir.getAbsolutePath();
        String filePath = file.getAbsolutePath();

        if (filePath.startsWith(rootPath)) {
            String relative = filePath.substring(rootPath.length());
            // 移除开头的分隔符
            if (relative.startsWith(File.separator)) {
                relative = relative.substring(1);
            }
            // 统一使用 / 作为路径分隔符
            return relative.replace('\\', '/');
        }
        return file.getName();
    }
}
