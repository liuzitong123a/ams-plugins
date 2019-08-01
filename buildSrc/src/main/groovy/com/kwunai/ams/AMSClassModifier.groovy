package com.kwunai.ams

import org.apache.commons.compress.utils.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter

class AMSClassModifier {

    private static HashSet<String> exclude

    static {
        exclude = new HashSet<>()
        // 采集的包名
        exclude.add("com.kwunai.app.asmplugin")
    }


    static boolean isShouldModify(String className) {

        Iterator<String> iterator = exclude.iterator()

        while (iterator.hasNext()) {
            String packageName = iterator.next()
            if (className.startsWith(packageName)) {
                return false
            }
        }

        // 如果是系统资源和配置文件，移除掉
        if (className.contains('R$') ||
                className.contains('R2$') ||
                className.contains('R.class') ||
                className.contains('R2.class') ||
                className.contains('BuildConfig.class')) {
            return false
        }

        return true
    }

    static File modifyClassFile(File dir, File classFile, File tempDir) {
        File modified = null
        try {
            String className = path2ClassName(classFile.absolutePath
                    .replace("${dir.absolutePath}${File.separator}", ""))
            byte[] sourceClassBytes = IOUtils.toByteArray(new FileInputStream(classFile))
            byte[] modifiedClassBytes = modifyClass(sourceClassBytes)
            if (modifiedClassBytes) {
                modified = new File(tempDir, className.replace('.', '') + '.class')
                if (modified.exists()) {
                    modified.delete()
                }
                modified.createNewFile()
                new FileOutputStream(modified).write(modifiedClassBytes)
            }
        } catch (Exception e) {
            e.printStackTrace()
            modified = classFile
        }
        return modified
    }

    private static byte[] modifyClass(byte[] srcClass) throws IOException {
        // class阅读器
        ClassReader cr = new ClassReader(srcClass)
        // 写出器 COMPUTE_FRAMES 计算所有的内容，后续操作更简单。
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        // 方法分析器
        ClassVisitor classVisitor = new AMSClassVisitor(classWriter)
        // 分析，处理结果写入cw。 EXPAND_FRAMES(栈帧格式)，android必须用它
        cr.accept(classVisitor, ClassReader.EXPAND_FRAMES)
        return classWriter.toByteArray()
    }

    static String path2ClassName(String pathName) {
        pathName.replace(File.separator, ".")
                .replace(".class", "")
    }
}